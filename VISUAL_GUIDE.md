# Session Management Visual Guide

## 🎯 The Three Problems & Solutions (Illustrated)

---

## ❌ PROBLEM 1: Orders Mixed Between Users

### Before (Broken)
```
┌─────────────────────────────────────┐
│      User A Logs In                  │
│      SessionManager.activeUserId = 1 │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│   OrdersViewModel.loadOrders()       │
│   orderDao.getAllOrders() ❌         │
│                                      │
│   Returns ALL orders from ALL users: │
│   Order 1 (User A)                   │
│   Order 2 (User A)                   │
│   Order 3 (User B) ← SHOULDN'T SEE   │
│   Order 4 (User B) ← SHOULDN'T SEE   │
│   Order 5 (User C) ← SHOULDN'T SEE   │
└─────────────────────────────────────┘
              ↓
        UI Shows ALL Orders
        (Security Issue!)
```

### After (Fixed)
```
┌─────────────────────────────────────┐
│      User A Logs In                  │
│      SessionManager.activeUserId = 1 │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│   OrdersViewModel.loadOrders()       │
│   userId = sessionManager.         │
│             getActiveUserId() → 1   │
│   orderDao.getOrdersByUserFlow(1)✅ │
│                                      │
│   Returns ONLY User A's orders:      │
│   Order 1 (User A) ✓                 │
│   Order 2 (User A) ✓                 │
└─────────────────────────────────────┘
              ↓
        UI Shows Only A's Orders
        (Secure!)
```

---

## ❌ PROBLEM 2: Wrong User Deleted

### Before (Dangerous)
```
Timeline:
  User A logs in
    → lastUser = User A
  User B logs in (overwrites)
    → lastUser = User B
  App killed
  User A comes back and taps "Delete Account"
  
┌─────────────────────────────────────┐
│  AccountViewModel.deleteAccount()    │
│                                      │
│  lastUser = userDao.getLastUser()   │
│            → User B (WRONG!) ❌      │
│                                      │
│  if (verify(password, lastUser...)) │
│    userDao.deleteUser(B)  BOOM! 💥  │
│                                      │
│  Result: User B deleted, not A!      │
│  User A is shocked, User B is gone   │
└─────────────────────────────────────┘
```

### After (Safe)
```
Timeline:
  User A logs in
    → SessionManager.activeUserId = 1
    → Saved to DataStore (persistent)
  User B logs in
    → SessionManager.activeUserId = 2
    → Overwrites DataStore
  App killed
  User A comes back and taps "Delete Account"
  
┌─────────────────────────────────────┐
│  AccountViewModel.deleteAccount()    │
│                                      │
│  userId = sessionManager.           │
│         getActiveUserId() → 1 ✅     │
│                                      │
│  currentUser = userDao.getUserById(1)│
│               → User A (Correct!)    │
│                                      │
│  if (verify(password, userA...))    │
│    api.deleteAccount(1, ...)  ✓     │
│    userDao.deleteUserById(1)  ✓     │
│                                      │
│  Result: User A deleted (correct)    │
│  User B still exists (unaffected)    │
└─────────────────────────────────────┘
```

**Key Difference:**
- ❌ OLD: `getLastUser()` returns "most recent user"
- ✅ NEW: `getActiveUserId()` returns "currently logged in user"

---

## ❌ PROBLEM 3: Lost Session on Restart

### Before (Unreliable)
```
Session in Room (unreliable):
  ┌─────────────────────┐
  │  users table:       │
  │  User 1 (Alice)     │
  │  User 2 (Bob)       │
  │  (No indicator of   │
  │   who is logged in) │
  └─────────────────────┘
              ↓
  App killed
              ↓
  App restarted
              ↓
  lastUser = getLastUser()
           = User 2 (assumes Bob)
  But Bob never logged in!
  ❌ Wrong assumption
```

### After (Persistent)
```
Session in DataStore (persistent):
  ┌──────────────────────────────┐
  │  DataStore (Persistent):      │
  │  activeUserId = 1             │
  │  activeUserEmail = "a@..."    │
  │  activeUserName = "Alice"     │
  │                               │
  │  Survives app kill! ✓         │
  └──────────────────────────────┘
              ↓
  App killed
              ↓
  App restarted
              ↓
  userId = sessionManager.getActiveUserId()
         = 1 (from DataStore, persistent!)
  user = userDao.getUserById(1)
       = Alice
  ✅ Correct! Still logged in as Alice
```

---

## 🏗️ Architecture Layers

### Layer 1: UI Layer (Activities/Fragments)
```
┌──────────────────────────────────┐
│     MainActivity                 │
│                                  │
│  onSessionStateChanged(info) {    │
│    if (info.isLoggedIn) {         │
│      showHomeScreen()             │
│    } else {                       │
│      showLoginScreen()            │
│    }                              │
│  }                               │
└──────────────────────────────────┘
```

### Layer 2: ViewModel Layer (Business Logic)
```
┌──────────────────────────────────┐
│     AuthViewModel                │
│                                  │
│  onLoginClicked(email, pwd) {     │
│    user = repo.getUserByEmail()   │
│    if (verify(pwd, user.pwd)) {   │
│      repo.loginSession(user)      │
│      _currentUser = user          │
│      navigateHome()               │
│    }                              │
│  }                               │
└──────────────────────────────────┘
```

### Layer 3: Repository Layer (API + DB)
```
┌──────────────────────────────────┐
│     AuthRepository               │
│                                  │
│  loginSession(user) {             │
│    sessionManager.login(...)      │
│  }                               │
│                                  │
│  getCurrentUser() {               │
│    userId = sessionManager.get()  │
│    return userDao.getById(userId) │
│  }                               │
└──────────────────────────────────┘
```

### Layer 4: Data Layer (Persistence)
```
┌──────────────────────────────────┐
│     SessionManager               │ (NEW)
│     (DataStore)                  │
│                                  │
│  activeUserId = 1                │
│  activeUserEmail = "a@..."       │
│  activeUserName = "Alice"        │
│                                  │
│  Source of truth for login       │
└──────────────────────────────────┘

┌──────────────────────────────────┐
│     Room Database                │
│     (Users, Orders, Cart)        │
│                                  │
│  users: id, email, password...   │
│  orders: userId, orderDate...    │
│  cart: userId, items...          │
│                                  │
│  Historical persistence          │
└──────────────────────────────────┘
```

---

## 🔄 Data Flow: Multi-User Session

### Step 1: User A Logs In
```
┌────────────────────────────────┐
│ User A enters:                  │
│ email: a@example.com            │
│ password: password123           │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ AuthViewModel.onLoginClicked()  │
│ (Business Logic)                │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ AuthRepository.getUserByEmail()│
│ Query Room: SELECT * FROM      │
│ users WHERE email = 'a@...'   │
│ Returns: UserEntity(id=1, ...)│
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ Verify password:               │
│ PasswordUtil.verify(            │
│   "password123",               │
│   stored_hash                  │
│ ) → true ✓                      │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ AuthRepository.loginSession()   │
│ SessionManager.login(           │
│   userId=1,                     │
│   email="a@...",                │
│   name="Alice"                  │
│ )                              │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ Save to DataStore:             │
│ activeUserId = 1               │
│ activeUserEmail = "a@..."      │
│ activeUserName = "Alice"       │
│ (Persisted!)                   │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ AuthViewModel updates UI:       │
│ _currentUser = User A           │
│ _success = true                │
└────┬───────────────────────────┘
     │
     ↓
    Navigate to Home Screen
```

### Step 2: Load User A's Orders
```
┌────────────────────────────────┐
│ OrdersViewModel.loadOrders()    │
│                                │
│ Get userId from SessionManager│
│ userId = 1                     │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ OrderDao.getOrdersByUserFlow(1)│
│                                │
│ SQL: SELECT * FROM orders      │
│ WHERE userId = 1               │
│ ORDER BY orderDate DESC        │
│                                │
│ Returns: [Order1, Order2]      │
│ (Only User A's orders)         │
└────┬───────────────────────────┘
     │
     ↓
    UI Updates: Show A's Orders
```

### Step 3: User A Logs Out
```
┌────────────────────────────────┐
│ User taps Logout               │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ AuthViewModel.logout()          │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ AuthRepository.logout()         │
│ SessionManager.logout()         │
│                                │
│ activeUserId = -1              │
│ activeUserEmail = ""           │
│ activeUserName = ""            │
│ (Persisted to DataStore)       │
│                                │
│ Room data UNTOUCHED ✓          │
│ (Users and Orders still there) │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ AuthViewModel._currentUser = null
│ Navigate to Login Screen       │
└───────────────────────────────┘
```

### Step 4: User B Logs In
```
(Same flow as Step 1, but...)
     │
     ↓
┌────────────────────────────────┐
│ SessionManager.login(          │
│   userId=2,                    │
│   email="b@...",               │
│   name="Bob"                   │
│ )                              │
│                                │
│ activeUserId = 2 (overwrites)  │
│ activeUserEmail = "b@..."      │
│ activeUserName = "Bob"         │
│ (Persisted to DataStore)       │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ Load User B's Orders:          │
│ OrderDao.getOrdersByUserFlow(2)│
│                                │
│ Returns: [Order3, Order4, ...]│
│ (Only User B's orders)         │
│ (NOT User A's orders!)         │
└───────────────────────────────┘
```

---

## 🗑️ Delete Account Flow

```
┌────────────────────────────────┐
│ User B taps "Delete Account"   │
│ Enters password: "mypassword"  │
└────┬───────────────────────────┘
     │
     ↓ AccountViewModel.deleteAccount()
┌────────────────────────────────┐
│ Step 1: Get Current User       │
│ userId = sessionManager.       │
│          getActiveUserId() = 2 │
│ user = userDao.getUserById(2)  │
│      = User B (Correct!)       │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ Step 2: Verify Password        │
│ PasswordUtil.verify(           │
│   "mypassword",                │
│   stored_hash                  │
│ ) → true ✓                      │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ Step 3: Backend Verification   │
│ apiService.deleteAccount(      │
│   userId=2,                    │
│   password=stored_hash         │
│ )                              │
│                                │
│ Backend:                       │
│  1. Verify hash                │
│  2. Delete from database       │
│  3. Return 200 OK              │
│                                │
│ Response: success ✓            │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ Step 4: Delete Locally         │
│ userDao.deleteUserById(2)      │
│ orderDao.deleteOrdersForUser(2)│
│ cartDao.deleteCartForUser(2)   │
│                                │
│ Room: User B + all related     │
│ data GONE ✓                    │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ Step 5: Clear Session          │
│ sessionManager.               │
│   onAccountDeleted()          │
│ activeUserId = -1             │
│ (Persisted to DataStore)      │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ Step 6: Navigate to Login      │
│ User B's account gone!         │
│ Can't login as User B anymore  │
│                                │
│ User A still exists:           │
│ ✓ User A can still login       │
│ ✓ User A's orders still there  │
│ ✓ No data mixing!              │
└───────────────────────────────┘
```

---

## 🔄 Session Persistence on App Restart

```
Scenario: User A logs in and app is killed

┌────────────────────────────────┐
│ Before Kill:                   │
│                                │
│ SessionManager.activeUserId=1  │
│ (Saved to DataStore)           │
│                                │
│ Room:                          │
│ users: [A, B, C]               │
│ orders: [A's, B's, C's orders] │
└────┬───────────────────────────┘
     │
     ↓
    App Process Killed
     │
     ↓
┌────────────────────────────────┐
│ After Restart:                 │
│                                │
│ MainActivity.onCreate() {       │
│   authViewModel.               │
│     loadCurrentUser()          │
│ }                              │
└────┬───────────────────────────┘
     │
     ↓
┌────────────────────────────────┐
│ SessionManager reads DataStore:│
│ activeUserId = 1 (Persisted!)  │
│                                │
│ UserDao.getUserById(1)         │
│ Returns: User A                │
│                                │
│ AuthViewModel._currentUser =A  │
│ Navigate to Home Screen        │
│                                │
│ Result: Still logged in! ✓     │
└───────────────────────────────┘
```

---

## 📊 Comparison Table (Visual)

### Order Loading
```
❌ BEFORE:
getOrdersByUser(userId)
    ↓
Orders: [A1, A2, B1, B2, C1]  (Mixed!)

✅ AFTER:
getOrdersByUser(activeUserId=1)
    ↓
Orders: [A1, A2]  (Only A's)
```

### User Deletion
```
❌ BEFORE:
getLastUser() → User B (WRONG!)
    ↓
Delete User B (User A wanted to delete!)

✅ AFTER:
getActiveUserId() → 1 (User A)
    ↓
Delete User 1 (Correct!)
```

### Session Persistence
```
❌ BEFORE:
Room-based ➜ Clear on logout ➜ Lost on restart

✅ AFTER:
DataStore-based ➜ Persists across restarts
```

---

## 🎯 Key Takeaways (Visual)

```
┌─────────────────────────────────────┐
│  SessionManager (NEW)               │
│  Single Source of Truth             │
│  ┌─────────────────────────────┐   │
│  │ activeUserId = 1            │   │
│  │ activeUserEmail = "a@..."   │   │
│  │ activeUserName = "Alice"    │   │
│  └─────────────────────────────┘   │
│                                     │
│  Persisted to DataStore             │
│  Survives app restart               │
│  ONLY place that answers:           │
│  "Who is logged in?"                │
└─────────────────────────────────────┘

              ↓ Uses

┌─────────────────────────────────────┐
│  Room Database                      │
│  Historical Persistence             │
│  ┌─────────────────────────────┐   │
│  │ users:                      │   │
│  │ ├─ User 1 (Alice)          │   │
│  │ ├─ User 2 (Bob)            │   │
│  │ └─ User 3 (Charlie)        │   │
│  │                             │   │
│  │ orders:                     │   │
│  │ ├─ Order 1 (userId=1)      │   │
│  │ ├─ Order 2 (userId=2)      │   │
│  │ └─ Order 3 (userId=1)      │   │
│  └─────────────────────────────┘   │
│                                     │
│  NEVER cleared on logout            │
│  Filtered by userId from            │
│  SessionManager                     │
└─────────────────────────────────────┘
```

---

**Visual Guide Complete** ✅
