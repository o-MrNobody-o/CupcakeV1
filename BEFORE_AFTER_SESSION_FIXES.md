# Session Management Fixes: Before vs. After

## Issue 1: Orders Mixed Between Users

### ❌ BEFORE (Broken)

```kotlin
// OrdersViewModel.kt
class OrdersViewModel(context: Context) : ViewModel() {
    private val orderDao = AppDatabase.getInstance(context).orderDao()
    
    fun loadOrders() {
        viewModelScope.launch {
            // ❌ Problem: No userId filter!
            val orders = orderDao.getAllOrders()  // Gets ALL users' orders
            _orders.value = orders
        }
    }
}
```

**What Happens:**
```
User A logs in
  → Sees all users' orders (A's, B's, C's orders all visible)
  → Can see orders they didn't create
  → Can potentially modify/delete other users' orders

User B logs in
  → Sees same mixed orders
  → No data isolation between accounts
```

### ✅ AFTER (Fixed)

```kotlin
// OrdersViewModel.kt
class OrdersViewModel(context: Context) : ViewModel() {
    private val repository = OrderRepository(context)
    private val sessionManager = repository.getSessionManager()
    
    fun loadOrders() {
        viewModelScope.launch {
            // ✅ Solution: Filter by activeUserId
            repository.getOrdersForActiveUser().collect { orders ->
                _orders.value = orders  // Only current user's orders
            }
        }
    }
}

// OrderRepository.kt
class OrderRepository(context: Context) {
    
    suspend fun getOrdersForActiveUser(): Flow<List<OrderEntity>> {
        val userId = sessionManager.getActiveUserId()
        return if (userId > 0) {
            orderDao.getOrdersByUserFlow(userId)
        } else {
            flowOf(emptyList())
        }
    }
}

// OrderDao.kt
suspend fun getOrdersByUserFlow(userId: Int): Flow<List<OrderEntity>> {
    // ✅ Database query filtered by userId
    return userDatabase
        .orders()
        .where(Orders.USER_ID.eq(userId))
        .asFlow()
}
```

**What Happens Now:**
```
User A logs in
  → SessionManager.activeUserId = 1
  → getOrdersForActiveUser() → getOrdersByUserFlow(1)
  → Sees only User A's orders

User B logs in
  → SessionManager.activeUserId = 2
  → getOrdersForActiveUser() → getOrdersByUserFlow(2)
  → Sees only User B's orders
  → Can't see User A's orders
```

---

## Issue 2: Wrong User Deleted on Account Deletion

### ❌ BEFORE (Dangerous)

```kotlin
// AccountViewModel.kt (OLD)
class AccountViewModel(context: Context) : ViewModel() {
    private val userDao = AppDatabase.getInstance(context).userDao()
    
    fun deleteAccount(password: String) {
        viewModelScope.launch {
            try {
                // ❌ Problem: Which user are we deleting?
                val lastUser = userDao.getLastUser()  // Gets the most recent user
                
                if (lastUser != null && verify(password, lastUser.password)) {
                    userDao.deleteUser(lastUser.id)  // Delete based on "last"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}

// OrderDao.kt (OLD)
suspend fun getLastUser(): UserEntity? {
    return database.userDao().query("SELECT * FROM users ORDER BY id DESC LIMIT 1")
}
```

**What Happens (Disaster Scenario):**
```
User A logs in
User B logs in (overwrites "last user")
App restarts or User A taps "Delete Account"
  → getLastUser() returns User B (most recent)
  → User B's account gets deleted instead of User A
  → User A is still logged in but account is gone
```

### ✅ AFTER (Safe)

```kotlin
// AccountViewModel.kt (NEW)
class AccountViewModel(context: Context) : ViewModel() {
    private val repository = AuthRepository(context)
    
    fun deleteAccount(password: String) {
        viewModelScope.launch {
            try {
                // ✅ Solution: Get SPECIFIC user from SessionManager
                val activeUserId = repository.getSessionManager().getActiveUserId()
                
                if (activeUserId == -1) {
                    _error.value = "Not logged in"
                    return@launch
                }
                
                // ✅ Get the EXACT user we're about to delete
                val currentUser = repository.getUserById(activeUserId)
                    ?: throw Exception("User not found")
                
                // ✅ Verify password
                if (!PasswordUtil.verify(password, currentUser.password)) {
                    _error.value = "Incorrect password"
                    return@launch
                }
                
                // ✅ Send to backend for ADDITIONAL verification
                // This prevents app-side spoofing
                val response = apiService.deleteAccount(activeUserId, {
                    userId = activeUserId,
                    password = currentUser.password
                })
                
                // ✅ Only delete if backend verified
                if (response.isSuccessful) {
                    repository.deleteUserById(activeUserId)
                    repository.onAccountDeleted()
                    _success.value = true
                }
                
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}

// SessionManager.kt
suspend fun getActiveUserId(): Int {
    // ✅ Get from persistent DataStore, not database
    return activeUserIdFlow.first()
}

// UserDao.kt
suspend fun deleteUserById(userId: Int) {
    // ✅ Delete SPECIFIC user by ID
    database.delete("users", "id = ?", arrayOf(userId.toString()))
}
```

**What Happens Now (Safe):**
```
User A logs in
  → SessionManager.activeUserId = 1

User B logs in
  → SessionManager.activeUserId = 2 (overwrites)

App restarts
  → SessionManager.activeUserId still = 2 (DataStore is persistent!)

User A tries to login again
  → Provides password for User A
  → repository.getUserByEmail("userA@...")
  → Verifies password against User A's hash
  → SessionManager.activeUserId = 1
  → Now User A is logged in

User A taps "Delete Account"
  → activeUserId = 1 (correct!)
  → Deletes User A only
  → User B still exists
```

---

## Issue 3: No Active User Tracking / Lost Session on Restart

### ❌ BEFORE (Unreliable)

```kotlin
// MainActivity.kt (OLD)
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ❌ Problem: How do we know if user is logged in?
        val lastUser = userDao.getLastUser()
        
        if (lastUser != null) {
            // Assume logged in
            showHomeScreen()
        } else {
            // Assume logged out
            showLoginScreen()
        }
    }
}

// When user logs out:
fun logout() {
    userDao.clearAllUsers()  // ❌ Deletes ALL users!
}
```

**What Happens:**
```
User A logs in
  → lastUser = User A
  → MainActivity shows home screen

App restarts
  → lastUser = User A (still in Room)
  → Shows home screen ✓ (works)

User A logs out
  → clearAllUsers() deletes User A from Room
  → lastUser = null
  → Shows login screen ✓ (works)

But if User A logs back in:
  → Need to re-create User A in Room
  → Or had to save User A somewhere else
  → Messy and error-prone!
```

### ✅ AFTER (Reliable)

```kotlin
// MainActivity.kt (NEW)
class MainActivity : AppCompatActivity() {
    private lateinit var authViewModel: AuthViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authViewModel = AuthViewModel(this)
        
        // ✅ Load current user from SessionManager
        authViewModel.loadCurrentUser()
        
        // Observe session state (persistent across restarts)
        authViewModel.sessionState.observe(this) { sessionInfo ->
            if (sessionInfo.isLoggedIn) {
                // User is logged in (SessionManager.activeUserId > 0)
                showHomeScreen()
            } else {
                // User is logged out (SessionManager.activeUserId == -1)
                showLoginScreen()
            }
        }
    }
}

// AuthViewModel.kt (NEW)
fun loadCurrentUser() {
    viewModelScope.launch {
        // ✅ Read from SessionManager (DataStore is persistent)
        val user = repository.getCurrentUser()
        _currentUser.value = user
    }
}

// AuthRepository.kt (NEW)
suspend fun getCurrentUser(): UserEntity? {
    val userId = sessionManager.getActiveUserId()
    return if (userId > 0) {
        getUserById(userId)
    } else {
        null
    }
}

// When user logs out:
fun logout() {
    sessionManager.logout()  // ✅ Only clears SessionManager
    // Room data untouched - can login again later
}
```

**What Happens Now (Reliable):**
```
User A logs in
  → SessionManager stores: activeUserId = 1, email = "a@...", name = "Alice"
  → Main shows home screen

App killed and restarted
  → DataStore reads: activeUserId = 1 (persistent!)
  → fetchUser(1) from Room
  → showHomeScreen() ✓

App is backgrounded for days
  → DataStore still has activeUserId = 1
  → User not logged out
  → Data is safe

User taps Logout
  → SessionManager.logout() clears activeUserId = -1
  → Main shows login screen
  → User data stays in Room

User logs back in with same account
  → SessionManager stores: activeUserId = 1 again
  → Main shows home screen
  → All orders still there ✓
```

---

## Issue 4: Logout Behavior Confusion

### ❌ BEFORE (Unclear Semantics)

```kotlin
// What does logout() do?

// Option A: Clears Room (loses everything)
fun logout() {
    userDao.clearAllUsers()
}

// Option B: Just clears current user
fun logout() {
    userDao.clearCurrentUser()  // But how? No setCurrentUser exists reliably
}

// Result: Confusion about logout semantics
// - Can user login again after logout?
// - Is user data deleted or just hidden?
// - What about multi-user scenarios?
```

### ✅ AFTER (Clear Semantics)

```kotlin
// LOGOUT = "Switch out of this user's session"
// - SessionManager.activeUserId = -1
// - Room data is preserved
// - Can login again later with same or different account

fun logout() {
    sessionManager.logout()  // Only clears SessionManager
}

// DELETE ACCOUNT = "Permanently delete this user"
// - SessionManager.activeUserId = -1
// - Room data is deleted
// - User cannot login again (no account exists)

suspend fun deleteUserById(userId: Int) {
    userDao.deleteUserById(userId)
    orderDao.deleteOrdersForUser(userId)
    cartDao.deleteCartForUser(userId)
    if (sessionManager.getActiveUserId() == userId) {
        sessionManager.logout()
    }
}
```

---

## Comparison Table

| Aspect | ❌ BEFORE | ✅ AFTER |
|--------|----------|---------|
| **Login State Source** | Room (unreliable) | SessionManager (DataStore) |
| **Session Persistence** | Lost on app restart | Preserved (DataStore) |
| **Order Filtering** | getAllOrders() (mixes users) | getOrdersByUserFlow(userId) |
| **Delete Account** | getLastUser() pattern (wrong user!) | getActiveUserId() from SessionManager |
| **Logout Behavior** | Deletes Room (can't relogin) | Clears SessionManager only |
| **Multi-User Support** | None (data mixing) | Full isolation via userId filters |
| **Delete Verification** | App-side only (spoofable) | App + Backend verification |
| **Data Safety** | Low (easy to delete wrong user) | High (explicit userId + backend check) |

---

## Migration Checklist

### Phase 1: Add SessionManager
- [ ] Create `SessionManager.kt` with DataStore integration
- [ ] Add DataStore dependency: `androidx.datastore:datastore-preferences:1.0.0`
- [ ] Test: Can read/write activeUserId to DataStore

### Phase 2: Update Room DAOs
- [ ] Update `UserDao.kt`: Add getUserById(), deleteUserById(), getAllUsers()
- [ ] Update `OrderDao.kt`: Add getOrdersByUserFlow(), deleteOrdersForUser()
- [ ] Test: Queries work with userId filter

### Phase 3: Update Repository
- [ ] Update `AuthRepository.kt`: Separate session from persistence
- [ ] Add getSessionManager() accessor for ViewModels
- [ ] Test: login/logout work correctly

### Phase 4: Update ViewModels
- [ ] Update `AuthViewModel.kt`: Use repository.loginSession() on login
- [ ] Update `AccountViewModel.kt`: Use deleteAccount() with backend call
- [ ] Test: Login/logout/delete flows work

### Phase 5: Update UI
- [ ] Update `MainActivity.kt`: Call loadCurrentUser() on startup
- [ ] Add navigation based on SessionManager.sessionFlow
- [ ] Test: Session persists across app restarts

### Phase 6: Test Multi-User
- [ ] [ ] Create 3 test accounts (User A, B, C)
- [ ] [ ] Login A → See A's orders
- [ ] [ ] Logout → Login B → See B's orders (not A's)
- [ ] [ ] Kill app while B logged in → Restart → Still logged in as B
- [ ] [ ] Delete B's account → B's data gone, A's data untouched

---

## Code Patterns to Replace

### Pattern 1: Room-Based Session

```kotlin
// ❌ Remove this pattern
val lastUser = userDao.getLastUser()
if (lastUser != null) { /* logged in */ }
```

```kotlin
// ✅ Use this pattern
val userId = sessionManager.getActiveUserId()
if (userId > 0) { /* logged in */ }
```

### Pattern 2: Universal Queries

```kotlin
// ❌ Remove this pattern
val orders = orderDao.getAllOrders()
val users = userDao.getAllUsers()
```

```kotlin
// ✅ Use this pattern
val userId = sessionManager.getActiveUserId()
val orders = orderDao.getOrdersByUserFlow(userId)
val user = userDao.getUserById(userId)
```

### Pattern 3: Clearing Room on Logout

```kotlin
// ❌ Remove this pattern
fun logout() {
    userDao.clearAllUsers()
}
```

```kotlin
// ✅ Use this pattern
fun logout() {
    sessionManager.logout()  // SessionManager only
}
```

### Pattern 4: Delete Without Verification

```kotlin
// ❌ Remove this pattern
fun deleteAccount() {
    userDao.deleteLastUser()  // Which user?
}
```

```kotlin
// ✅ Use this pattern
fun deleteAccount(password: String) {
    // 1. Verify it's the right user
    val userId = sessionManager.getActiveUserId()
    val user = userDao.getUserById(userId)
    
    // 2. Verify password
    if (!verify(password, user.password)) return
    
    // 3. Verify with backend
    val response = api.deleteAccount(userId, password)
    if (!response.isSuccessful) return
    
    // 4. Delete locally
    userDao.deleteUserById(userId)
}
```

---

## Result

After implementing these changes, the app will have:

✅ **No order mixing** - Orders filtered by userId
✅ **Safe deletion** - Always delete specific userId
✅ **Session persistence** - SessionManager via DataStore
✅ **Multi-user support** - Full isolation between accounts
✅ **Clear semantics** - logout ≠ delete
✅ **Backend verification** - Can't spoof deletion on app side
