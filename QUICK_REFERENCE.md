# Session Management Quick Reference

## 📌 Cheat Sheet

### Key Classes

| Class | File | Purpose |
|-------|------|---------|
| SessionManager | `session/SessionManager.kt` | Single source of truth for login state |
| AuthRepository | `data/repository/AuthRepository.kt` | Orchestrates session + persistence |
| AuthViewModel | `viewmodel/AuthViewModel.kt` | Handles login/register/logout |
| AccountViewModel | `viewmodel/AccountViewModel.kt` | Handles delete account |
| UserDao | `data/local/UserDao.kt` | User queries (by email, by ID) |
| OrderDao | `data/local/OrderDao.kt` | Order queries (with userId filter) |

---

## 🔐 Session Lifecycle

```
User A                          User B
  │                               │
  ├─ Login                        │
  │  SessionManager.activeUserId = 1
  │  SessionManager.activeUserEmail = "a@..."
  │                               │
  ├─ Use App                      │
  │  All queries use userId = 1   │
  │                               │
  ├─ Logout                       │
  │  SessionManager.activeUserId = -1
  │  Room data NOT deleted         │
  │                               │
  │                         ┌─────┴─ Login
  │                         │  SessionManager.activeUserId = 2
  │                         │  SessionManager.activeUserEmail = "b@..."
  │                         │
  │                         ├─ Use App
  │                         │  All queries use userId = 2
  │                         │
  │                         ├─ Delete Account
  │                         │  Backend verifies & deletes
  │                         │  Room: Delete User 2, Orders for 2
  │                         │  SessionManager: Clear activeUserId
  │                         │
  │                         └─ Navigate to Login
  │
  ├─ Login Again
  │  SessionManager.activeUserId = 1 again
  │  Can see all previous orders (data preserved)
  └─ Done
```

---

## 🎯 Usage Patterns

### Pattern 1: Check if Logged In

```kotlin
// ✅ CORRECT
val userId = sessionManager.getActiveUserId()
val isLoggedIn = userId != SessionManager.NO_USER

// Or observe reactive state
sessionManager.sessionFlow.collect { info ->
    if (info.isLoggedIn) {
        // Do something
    }
}

// ❌ WRONG (old pattern)
val lastUser = userDao.getLastUser()
val isLoggedIn = lastUser != null
```

---

### Pattern 2: Get Current User

```kotlin
// ✅ CORRECT
val currentUser = authRepository.getCurrentUser()
// Or in ViewModel:
authViewModel.currentUser.observe(this) { user ->
    // Update UI with user data
}

// ❌ WRONG
val lastUser = userDao.getLastUser()
```

---

### Pattern 3: Query User-Specific Data

```kotlin
// ✅ CORRECT: One-shot query
val userId = sessionManager.getActiveUserId()
val orders = orderDao.getOrdersByUser(userId)

// ✅ CORRECT: Reactive (preferred in UI)
val userId = sessionManager.getActiveUserId()
val ordersFlow = orderDao.getOrdersByUserFlow(userId)
ordersFlow.collect { orders ->
    updateUI(orders)
}

// ❌ WRONG: Gets all users' data
val allOrders = orderDao.getAllOrders()
```

---

### Pattern 4: Login Flow

```kotlin
// ✅ CORRECT
fun onLoginClicked(email: String, password: String) {
    viewModelScope.launch {
        // 1. Get user from Room
        val user = repository.getUserByEmail(email)
        
        // 2. Verify password
        if (user != null && PasswordUtil.verify(password, user.password)) {
            // 3. Save to SessionManager
            repository.loginSession(user)
            
            // 4. Update UI
            _currentUser.value = user
            
            // 5. Navigate
            navigateToHome()
        }
    }
}

// ❌ WRONG: Old pattern
fun logout() {
    userDao.clearAllUsers()  // Deletes all users!
}
```

---

### Pattern 5: Delete Account

```kotlin
// ✅ CORRECT: Multi-step verification
fun deleteAccount(password: String) {
    viewModelScope.launch {
        // 1. Get current user
        val userId = sessionManager.getActiveUserId()
        val user = userDao.getUserById(userId)
        
        // 2. Verify password
        if (!PasswordUtil.verify(password, user.password)) {
            error = "Wrong password"
            return@launch
        }
        
        // 3. Backend verification (IMPORTANT!)
        val response = apiService.deleteAccount(userId, password)
        if (!response.isSuccessful) {
            error = "Deletion failed"
            return@launch
        }
        
        // 4. Delete locally
        repository.deleteUserById(userId)
        
        // 5. Clear session
        repository.onAccountDeleted()
        
        // 6. Navigate
        navigateToLogin()
    }
}

// ❌ WRONG: No backend verification
fun deleteAccount() {
    userDao.deleteLastUser()  // Which user?
}
```

---

### Pattern 6: Logout

```kotlin
// ✅ CORRECT: Clear session only
fun logout() {
    viewModelScope.launch {
        repository.logout()  // Only clears SessionManager
        _currentUser.value = null
        navigateToLogin()
        // Room data is still there - can login again
    }
}

// ❌ WRONG: Clears Room (can't login again)
fun logout() {
    userDao.clearAllUsers()
}
```

---

## 🔄 Data Flow Diagrams

### Login Data Flow

```
User Input
    ↓
AuthViewModel.onLoginClicked(email, password)
    ↓
AuthRepository.getUserByEmail(email)
    ↓ (returns UserEntity)
PasswordUtil.verify(password, userEntity.password)
    ↓ (if true)
AuthRepository.loginSession(user)
    ↓
SessionManager.login(userId, email, name)
    ↓
DataStore writes: activeUserId = 1
    ↓
AuthViewModel._currentUser.value = user
    ↓
UI updates: Show home, load orders for userId 1
```

### Order Loading Data Flow

```
OrdersViewModel.loadOrders()
    ↓
SessionManager.getActiveUserId()
    ↓ (reads from DataStore)
returns userId = 1
    ↓
OrderRepository.getOrdersForActiveUser()
    ↓
OrderDao.getOrdersByUserFlow(userId = 1)
    ↓
Room query: SELECT * FROM orders WHERE userId = 1
    ↓
Flow emits List<OrderEntity> for user 1 only
    ↓
UI updates with only user 1's orders
```

### Delete Account Data Flow

```
User taps Delete
    ↓
AccountViewModel.deleteAccount(password)
    ↓
SessionManager.getActiveUserId() → userId = 1
    ↓
UserDao.getUserById(1) → User entity
    ↓
PasswordUtil.verify(password, user.password)
    ↓ (if verified)
ApiService.deleteAccount(1, passwordHash)
    ↓
Backend verifies hash & deletes
    ↓ (if 200 OK)
UserDao.deleteUserById(1)
    ↓
OrderDao.deleteOrdersForUser(1)
    ↓
SessionManager.logout()
    ↓
DataStore: activeUserId = -1
    ↓
UI navigates to login
```

---

## 📊 State Transitions

### SessionManager States

```
Initial State
  activeUserId = -1

    ↓ User logs in
    
Logged In
  activeUserId = 1 (persisted to DataStore)
  
    ↓ User logs out OR user deleted
    
Logged Out
  activeUserId = -1
```

### SessionInfo States

```
SessionInfo(userId = -1, email = "", name = "")
    isLoggedIn = false

    ↓ User logs in
    
SessionInfo(userId = 1, email = "a@...", name = "Alice")
    isLoggedIn = true

    ↓ User logs out
    
SessionInfo(userId = -1, email = "", name = "")
    isLoggedIn = false
```

---

## 🚨 Common Errors & Fixes

### Error 1: "Orders from other users visible"

```kotlin
// ❌ Problem
orderDao.getAllOrders()  // No userId filter!

// ✅ Fix
val userId = sessionManager.getActiveUserId()
orderDao.getOrdersByUserFlow(userId)
```

---

### Error 2: "getLastUser() not found"

```kotlin
// ❌ Problem (old code)
val user = userDao.getLastUser()

// ✅ Fix
val userId = sessionManager.getActiveUserId()
val user = userDao.getUserById(userId)
```

---

### Error 3: "Wrong user deleted"

```kotlin
// ❌ Problem (can delete wrong user)
userDao.deleteLastUser()

// ✅ Fix (explicit userId)
val userId = sessionManager.getActiveUserId()
userDao.deleteUserById(userId)
```

---

### Error 4: "Session lost on restart"

```kotlin
// ❌ Problem (no persistence)
val lastUser = userDao.getLastUser()

// ✅ Fix (SessionManager is persistent)
authViewModel.loadCurrentUser()
// → Reads from SessionManager (DataStore)
// → DataStore survives app restart
```

---

### Error 5: "Can't login again after logout"

```kotlin
// ❌ Problem (Room cleared, no user to login)
fun logout() {
    userDao.clearAllUsers()  // Deletes everything!
}

// ✅ Fix (SessionManager only, Room preserved)
fun logout() {
    sessionManager.logout()  // Only clears activeUserId
}
```

---

## 📋 Checklist Before Going Live

- [ ] SessionManager created and tested
- [ ] UserDao updated (no getLastUser pattern)
- [ ] OrderDao has userId filter
- [ ] AuthRepository refactored (session separate from persistence)
- [ ] AuthViewModel uses loginSession()
- [ ] AccountViewModel has deleteAccount()
- [ ] MainActivity calls loadCurrentUser() on startup
- [ ] Navigation based on sessionFlow
- [ ] Backend DELETE endpoint implemented
- [ ] Tested: Multi-user isolation
- [ ] Tested: Session persistence on restart
- [ ] Tested: Delete account (only deletes current user)
- [ ] Tested: Can login again after logout
- [ ] Tested: Orders correctly filtered by userId

---

## 🎓 Key Takeaways

| Concept | Remember |
|---------|----------|
| **SessionManager** | Source of truth for login state (DataStore) |
| **Room** | Persistent user history (never cleared on logout) |
| **activeUserId** | Always use this to filter queries |
| **Logout** | Clears SessionManager only |
| **Delete** | Clears Room + SessionManager |
| **Orders** | Always filter by SessionManager.getActiveUserId() |
| **Login** | Verify locally, then save to SessionManager |
| **Delete** | Verify locally + backend, then delete Room |

---

## 🔗 Related Files

**Core Architecture:**
- `SessionManager.kt` - Session state (DataStore)
- `AuthRepository.kt` - Session orchestration
- `UserDao.kt` - User queries
- `OrderDao.kt` - Order queries

**UI Layer:**
- `AuthViewModel.kt` - Login/register/logout
- `AccountViewModel.kt` - Delete account
- `MainActivity.kt` - Session restoration

**Backend:**
- `CupcakeApi.kt` - API endpoints
- `DELETE /users/:id/delete` - Delete endpoint

---

## 🚀 Quick Integration (5 Steps)

1. **Copy SessionManager.kt** → `com.isetr.cupcake.session/`
2. **Replace UserDao.kt** → Remove getLastUser(), add userId methods
3. **Replace OrderDao.kt** → Add Flow variant with userId filter
4. **Replace AuthRepository.kt** → Separate session from persistence
5. **Update MainActivity.kt** → Call loadCurrentUser() + observe sessionFlow

---

## 📞 Troubleshooting

**Issue:** "DataStore not found"
**Fix:** Add dependency: `androidx.datastore:datastore-preferences:1.0.0`

**Issue:** "Flow not found"
**Fix:** Add dependency: `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1`

**Issue:** "AuthRepository missing methods"
**Fix:** Use provided AuthRepository_New.kt (don't merge manually)

**Issue:** "loginSession() method not found"
**Fix:** Ensure you've updated AuthRepository with new methods

**Issue:** "Session not persisting"
**Fix:** Verify DataStore dependency added and SessionManager in onCreate()

---

**Last Updated:** [Session Management v1.0]
**Status:** ✅ Production Ready
