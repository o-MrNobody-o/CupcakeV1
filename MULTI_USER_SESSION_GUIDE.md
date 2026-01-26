# Multi-User Session Management Integration Guide

## 📋 Overview

This document explains the production-ready multi-user session management architecture implemented to fix critical issues:

- ❌ **Problem 1:** No active user tracking → Orders mixed between accounts
- ❌ **Problem 2:** No explicit session state → Could delete wrong user
- ❌ **Problem 3:** Room used for login state → Unreliable on app restart

- ✅ **Solution:** SessionManager (DataStore) as single source of truth

---

## 🏗️ Architecture

### Three Layers (Separation of Concerns)

```
┌─────────────────────────────────────┐
│        UI Layer (Activities)         │
│   - AuthActivity, MainActivity       │
└────────────────────┬────────────────┘
                     │
┌─────────────────────▼────────────────┐
│    ViewModel Layer (Business Logic)  │
│   - AuthViewModel, AccountViewModel  │
└────────────────────┬────────────────┘
                     │
┌─────────────────────▼────────────────┐
│       Repository Layer (API + DB)    │
│   - AuthRepository, OrderRepository  │
└────────┬──────────────────────────┬──┘
         │                          │
    ┌────▼─────┐           ┌───────▼──┐
    │ SessionM  │           │   Room   │
    │ anager    │           │ Database │
    │(DataStore)│           │(Persist.)│
    └──────────┘           └──────────┘
```

### Key Design Principles

1. **SessionManager = Source of Truth for "Who is logged in?"**
   - Stores: activeUserId, activeUserEmail, activeUserName
   - Uses: DataStore (persistent, non-blocking)
   - Updated: On login, logout, delete-account
   - NOT updated: On regular logout (doesn't delete Room data)

2. **Room = Persistent User History**
   - Stores: ALL users (ever registered)
   - Stores: ALL orders (historical)
   - Only deleted: On explicit account deletion
   - Queries: Always filter by SessionManager.activeUserId

3. **API = Verification & Deletion**
   - Login: App verifies locally; backend not needed
   - Delete: Backend verifies password hash before deletion
   - Why: Prevents unauthorized deletion of other accounts

---

## 🔐 Session Lifecycle

### On App Startup

```kotlin
// MainActivity.onCreate()
val authViewModel = AuthViewModel(this)
authViewModel.loadCurrentUser()
// → Reads activeUserId from SessionManager
// → Fetches user details from Room
// → Updates UI with current user
```

Flow:
```
SessionManager.activeUserIdFlow
    ↓
Read from DataStore (fast, persistent)
    ↓
If userId > 0: Fetch UserEntity from Room
    ↓
Update UI with current user
```

### On Login

```kotlin
// AuthViewModel.onLoginClicked(email, password)
1. Get user by email from Room
2. Verify password hash (local, fast)
3. Call repository.loginSession(user)
   → Saves userId, email, name to SessionManager
   → No Room changes (data already there)
4. Update UI (_currentUser = user)
5. Navigate to home screen
```

**Critical Detail:**
```kotlin
// ❌ WRONG (old code)
logout() // Clears Room
login()  // Would fail if account deleted

// ✅ CORRECT (new code)
logout() // Clears SessionManager only
login()  // Can login again (user still in Room)
```

### On Logout

```kotlin
// AuthViewModel.logout()
1. Call repository.logout()
   → sessionManager.logout()
   → Clears SessionManager (activeUserId = -1)
   → Room data untouched (can login again)
2. Update UI (_currentUser = null)
3. Navigate to login screen
```

### On Account Delete (Complete Flow)

```kotlin
// AccountViewModel.deleteAccount(password)

// Step 1: Verify it's the right person
activeUserId = sessionManager.getActiveUserId()
currentUser = repository.getUserById(activeUserId)

// Step 2: Verify password locally (fast feedback)
if (!PasswordUtil.verify(password, currentUser.password)) {
    error = "Incorrect password"
    return
}

// ✅ Step 3: Send to backend for FINAL verification
DELETE /users/:id { userId, passwordHash }
Backend:
  1. Verify hash against stored password
  2. Delete user from database
  3. Return success or error

// ✅ Step 4: On success, delete locally
repository.deleteUserById(activeUserId)  // Delete from Room
repository.onAccountDeleted()             // Clear SessionManager

// Step 5: Update UI
Navigate to login screen
```

**Why This is Safe:**
- Local verification: Fast, no network
- Backend verification: Password hash prevents spoofing
- Room deletion: Only if backend succeeded
- Session clear: Only if Room deletion succeeded

---

## 📁 Updated Files & Methods

### SessionManager.kt (NEW)

**Purpose:** Single source of truth for active user session

**Key Methods:**
```kotlin
// Login: Save active session
suspend fun login(userId: Int, email: String, name: String)

// Logout: Clear active session
suspend fun logout()

// Get: Fetch current user ID (suspend)
suspend fun getActiveUserId(): Int

// Observe: Get reactive session state
val sessionFlow: Flow<SessionInfo>
val activeUserIdFlow: Flow<Int>
val activeUserEmailFlow: Flow<String>
val activeUserNameFlow: Flow<String>
```

**Data Structure:**
```kotlin
data class SessionInfo(
    val userId: Int = NO_USER,      // -1 = not logged in
    val email: String = "",
    val name: String = ""
) {
    val isLoggedIn: Boolean get() = userId != NO_USER
}
```

**Usage:**
```kotlin
// Observe login state in UI
sessionManager.sessionFlow.collect { info ->
    if (info.isLoggedIn) {
        // User is logged in: show home screen
    } else {
        // User is logged out: show login screen
    }
}

// Get current user ID (for queries)
val userId = sessionManager.getActiveUserId()
```

---

### UserDao.kt (UPDATED)

**Old Methods (REMOVED):**
```kotlin
// ❌ This was unreliable for session management
suspend fun getLastUser(): UserEntity?
suspend fun setCurrentUser(userId: Int)  // Didn't work as intended
```

**New Methods:**
```kotlin
// Get user by email (for login verification)
suspend fun getUserByEmail(email: String): UserEntity?

// Get user by ID (for profile loading)
suspend fun getUserById(userId: Int): UserEntity?

// Get all users (admin/debug)
suspend fun getAllUsers(): List<UserEntity>

// Delete specific user (for account deletion)
suspend fun deleteUserById(userId: Int)

// Clear all (testing only)
suspend fun clearAllUsers()
```

**Key Change:**
- Never query "last user" to infer login state
- Always use SessionManager.activeUserId instead

---

### OrderDao.kt (UPDATED)

**New Methods:**
```kotlin
// One-shot query (for immediate use)
suspend fun getOrdersByUser(userId: Int): List<OrderEntity>

// Reactive query (for UI updates)
fun getOrdersByUserFlow(userId: Int): Flow<List<OrderEntity>>

// Delete orders when account deleted
suspend fun deleteOrdersForUser(userId: Int)
```

**Usage Pattern:**
```kotlin
// Always filter by activeUserId
val activeUserId = sessionManager.getActiveUserId()
val orders = orderDao.getOrdersByUserFlow(activeUserId)
    .collect { orders ->
        // Update UI
    }
```

---

### AuthRepository.kt (COMPLETELY REFACTORED)

**Old Architecture (REMOVED):**
```kotlin
// ❌ UNSAFE: Clearing Room on logout
fun logout() {
    userDao.clearAllUsers()  // Deletes everything!
}

// ❌ UNSAFE: Could affect other users
fun loginUser(user: UserEntity) {
    clearAllUsers()  // Delete old users first
    userDao.insert(user)
}
```

**New Architecture:**
```kotlin
// Session management (SessionManager only)
suspend fun login(userId, email, name) {
    sessionManager.login(userId, email, name)
}

suspend fun logout() {
    sessionManager.logout()  // Don't touch Room
}

suspend fun onAccountDeleted() {
    sessionManager.logout()  // Clear session after delete
}

// User persistence (Room only)
suspend fun getUserByEmail(email): UserEntity? { ... }
suspend fun getUserById(userId): UserEntity? { ... }
suspend fun registerUser(user): Boolean { ... }
suspend fun deleteUserById(userId) { ... }

// Session-aware queries
suspend fun getCurrentUser(): UserEntity? {
    val userId = sessionManager.getActiveUserId()
    return if (userId > 0) getUserById(userId) else null
}
```

**Key Principle:**
```
SessionManager.logout()  ≠  UserDao.deleteUser()

Logout = "Clear who is logged in" (SessionManager)
Delete = "Delete user account" (Room + API)
```

---

### AuthViewModel.kt (UPDATED)

**Login Flow:**
```kotlin
fun onLoginClicked(email, password) {
    // 1. Find user by email
    val user = repository.getUserByEmail(email)
    
    // 2. Verify password
    if (PasswordUtil.verify(password, user.password)) {
        // 3. Save to SessionManager (NOT Room)
        repository.loginSession(user)
        
        // 4. Update UI
        _currentUser.value = user
        
        // 5. Navigate to home
    }
}
```

**Register Flow:**
```kotlin
fun onRegisterClicked(...) {
    // 1. Hash password
    val hash = PasswordUtil.hash(password)
    
    // 2. Create UserEntity
    val user = UserEntity(..., password = hash)
    
    // 3. Insert into Room
    repository.registerUser(user)
    
    // 4. Auto-login using SessionManager
    val insertedUser = repository.getUserByEmail(email)
    repository.loginSession(insertedUser)
}
```

**Logout Flow:**
```kotlin
fun logout() {
    repository.logout()  // Clears SessionManager only
    _currentUser.value = null
    // User data stays in Room (can login again)
}
```

---

### AccountViewModel.kt (NEW DELETE FLOW)

**Delete Account (Production-Safe):**
```kotlin
fun deleteAccount(password) {
    // 1. Get current user from SessionManager
    val activeUserId = sessionManager.getActiveUserId()
    
    // 2. Get user from Room
    val user = repository.getUserById(activeUserId)
    
    // 3. Verify password locally (fast)
    if (!PasswordUtil.verify(password, user.password)) {
        error = "Incorrect password"
        return
    }
    
    // ✅ 4. Send to backend for final verification
    apiService.deleteAccount(activeUserId, {
        userId: activeUserId,
        password: user.password  // Hash for verification
    })
    
    // 5. On backend success:
    //    - Delete from Room
    repository.deleteUserById(activeUserId)
    
    //    - Delete related data
    orderDao.deleteOrdersForUser(activeUserId)
    cartDao.deleteCartForUser(activeUserId)
    
    //    - Clear session
    repository.onAccountDeleted()
    
    // 6. Navigate to login
}
```

---

## 🎯 Usage Examples

### Example 1: Multi-User Session

```kotlin
// User A logs in
authViewModel.onLoginClicked("alice@example.com", "password123")
// → SessionManager.activeUserId = 1
// → SessionManager.activeUserEmail = "alice@example.com"

// Load User A's orders
orderDao.getOrdersByUserFlow(1)
    .collect { orders ->
        // Shows only User A's orders
    }

// User A logs out
authViewModel.logout()
// → SessionManager.activeUserId = -1
// → User A's data stays in Room

// User B logs in
authViewModel.onLoginClicked("bob@example.com", "password456")
// → SessionManager.activeUserId = 2
// → SessionManager.activeUserEmail = "bob@example.com"

// Load User B's orders
orderDao.getOrdersByUserFlow(2)
    .collect { orders ->
        // Shows only User B's orders (not A's)
    }
```

### Example 2: Account Deletion Safety

```kotlin
// Current user is Alice (ID = 1)
accountViewModel.deleteAccount("alice_password")

// Backend verification:
// 1. POST /users/1/delete {password: hash}
// 2. Backend verifies hash(alice_password) == stored hash
// 3. Backend deletes user 1 from database
// 4. Returns success

// On success, app:
// 1. repository.deleteUserById(1)  // Delete from Room
// 2. orderDao.deleteOrdersForUser(1)
// 3. repository.onAccountDeleted()  // Clear SessionManager
// 4. Navigate to login

// Result: Alice's account is completely gone
// Other users (Bob, Charlie, etc.) unaffected
```

### Example 3: Session Persistence on Restart

```kotlin
// App starts
MainActivity.onCreate() {
    authViewModel.loadCurrentUser()
}

// AuthViewModel.loadCurrentUser()
// 1. Read from SessionManager.activeUserIdFlow
//    → DataStore has userId = 2 (persistent)
// 2. Fetch UserEntity(2) from Room
// 3. Update UI with User 2's profile
// Result: App restarts with session still logged in

// If SessionManager was cleared:
// 1. Read from SessionManager.activeUserIdFlow
//    → DataStore has userId = -1
// 2. Don't fetch from Room
// 3. UI shows login screen
// Result: App restarts at login screen
```

---

## 🚨 Critical Safeguards

### 1. Never Use Room to Infer Login State

```kotlin
// ❌ WRONG
val isLoggedIn = userDao.getLastUser() != null

// ✅ CORRECT
val isLoggedIn = sessionManager.getActiveUserId() != -1
```

### 2. Always Filter Orders by Active User

```kotlin
// ❌ WRONG
val orders = orderDao.getAllOrders()  // Shows everyone's orders!

// ✅ CORRECT
val userId = sessionManager.getActiveUserId()
val orders = orderDao.getOrdersByUserFlow(userId)
```

### 3. Never Delete from Room on Logout

```kotlin
// ❌ WRONG
fun logout() {
    userDao.clearAllUsers()  // Deletes all users!
}

// ✅ CORRECT
fun logout() {
    sessionManager.logout()  // Only clears SessionManager
}
```

### 4. Always Verify Delete Request at Backend

```kotlin
// ❌ WRONG (app-side only)
fun deleteAccount(password) {
    if (verify(password)) {
        deleteFromRoom()  // Trusts app's verification
    }
}

// ✅ CORRECT (app + backend)
fun deleteAccount(password) {
    if (verify(password)) {
        // Send to backend for additional verification
        apiService.deleteAccount(userId, password)
        // Backend verifies hash before deletion
        deleteFromRoom()  // Only if backend succeeded
    }
}
```

---

## 📋 Integration Checklist

- [ ] Copy `SessionManager.kt` to `com.isetr.cupcake.session`
- [ ] Update `UserDao.kt` with new methods
- [ ] Update `OrderDao.kt` with Flow variant & deleteOrdersForUser()
- [ ] Update `AuthRepository.kt` with refactored methods
- [ ] Replace old `AuthViewModel.kt` with new implementation
- [ ] Replace old `AccountViewModel.kt` with new implementation
- [ ] Update `CupcakeApi.kt` to include `deleteAccount()` endpoint
- [ ] Add `DeleteAccountRequest` data class
- [ ] Update `MainActivity.onCreate()` to call `authViewModel.loadCurrentUser()`
- [ ] Update navigation logic: Show login if userId == -1, else show home
- [ ] Test: Login User A → Logout → Login User B (orders separate)
- [ ] Test: Login User A → Delete Account → Can't login again
- [ ] Test: App restart while logged in → Still logged in
- [ ] Test: App restart after logout → Shows login screen

---

## 🔧 Backend Requirements

### DELETE /users/:id

**Request:**
```json
{
  "userId": 1,
  "password": "bcrypt_hash_string"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "User deleted successfully"
}
```

**Response (401 Unauthorized):**
```json
{
  "success": false,
  "message": "Invalid password"
}
```

**Backend Logic:**
```javascript
POST /users/:id/delete
1. Get userId from request body
2. Get user from database
3. Verify bcrypt.compare(requestPassword, user.password)
4. If verified:
   - Delete user from database
   - Delete all related orders
   - Return 200 OK
5. If not verified:
   - Return 401 Unauthorized
```

---

## ✅ Expected Outcomes

After implementation:

- ✅ Login/logout/register work correctly
- ✅ Orders always filtered by activeUserId
- ✅ Delete account removes ONLY current user
- ✅ Other users' accounts unaffected
- ✅ Session persists across app restarts
- ✅ No data mixing between accounts
- ✅ Password verification for deletion
- ✅ Safe logout (preserves user history)

---

## 🎓 Key Learnings

| Problem | Root Cause | Solution |
|---------|-----------|----------|
| Orders mixed between users | No activeUserId in queries | Always use SessionManager.getActiveUserId() |
| Wrong user deleted | getLastUser() pattern | Delete by explicit userId only |
| Lost login on restart | Room cleared on logout | SessionManager persists via DataStore |
| No active user tracking | Using Room as session | SessionManager as source of truth |
| Logout clears history | clearAllUsers() on logout | Logout clears SessionManager only |

---

## 📚 Related Files

- `SessionManager.kt` - DataStore-based session state
- `UserDao.kt` - User-specific Room queries
- `OrderDao.kt` - Order-specific queries with userId filter
- `AuthRepository.kt` - Separation of session from persistence
- `AuthViewModel.kt` - Login/register/logout flows
- `AccountViewModel.kt` - Account deletion with backend verification
- `MainActivity.kt` - Session restoration on startup
- `CupcakeApi.kt` - Backend endpoints (needs deleteAccount)
