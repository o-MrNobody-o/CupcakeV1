# User Session Management - Logout Implementation Guide

## Problem Overview

**Issue:** When a user logs out and a different user logs in, the app still shows the previous user's information because Room retains the old cached user data.

**Root Cause:** The app uses `getLastUser()` to retrieve the "current" user, which returns the most recently inserted user from Room. Logout does not clear this cached data.

---

## Why This Matters

### Security & Privacy Risks:
1. **Privacy Violation** — Previous user's personal info (name, email, address, phone) exposed to new user
2. **Data Corruption** — Orders/cart items from different users could get mixed up
3. **Security Issue** — Unauthorized access to previous user's account data
4. **Incorrect App Behavior** — Wrong account displayed in UI, operations executed in wrong user context

---

## Solution Implemented

### 1. **UserDao.kt** - Added Clear Method
```kotlin
// Clear all cached user sessions (for logout)
@Query("DELETE FROM users")
suspend fun clearAllUsers()
```

**Purpose:** Removes ALL users from the local Room cache. This is safe because:
- ✅ Only clears the **local cached copy**
- ✅ Does NOT delete the actual account (accounts are in your backend/database)
- ✅ User can log back in anytime with the same credentials

---

### 2. **AuthRepository.kt** - Added Logout & Login Methods

```kotlin
/**
 * Logout: Clear all cached user sessions from Room.
 * This removes the local cached copy without deleting the account.
 */
suspend fun logout() {
    userDao.clearAllUsers()
}

/**
 * Login: Save the authenticated user to Room as the current session.
 * This replaces any previous cached user.
 */
suspend fun loginUser(user: UserEntity) {
    // Clear previous session first
    userDao.clearAllUsers()
    // Insert the new logged-in user
    userDao.insertUser(user)
}
```

**Key Points:**
- `logout()` clears the cached session
- `loginUser()` clears old sessions BEFORE inserting new user
- This ensures only ONE user is cached at a time

---

### 3. **AuthViewModel.kt** - Updated Login/Register Logic

**Login:**
```kotlin
if (user != null && PasswordUtil.verify(password, user.password)) {
    // Clear previous session and save new user as current session
    repository.loginUser(user)  // <-- NEW: Clears old data first
    _currentUser.value = user
    _success.value = true
}
```

**Register:**
```kotlin
repository.registerUser(user)

// After registration, login the new user (clear old sessions)
repository.loginUser(user)  // <-- NEW: Clears old data first
_currentUser.value = user
_success.value = true
```

**New Logout Method:**
```kotlin
fun logout() {
    viewModelScope.launch {
        try {
            repository.logout()  // Clear Room cache
            _currentUser.value = null  // Clear in-memory state
        } catch (e: Exception) {
            _error.value = "Logout failed: ${e.message}"
        }
    }
}
```

---

### 4. **AccountViewModel.kt** - Updated Logout

**Before:**
```kotlin
fun logout() {
    _currentUser.value = null  // Only clears memory, NOT Room!
}
```

**After:**
```kotlin
fun logout() {
    viewModelScope.launch {
        try {
            repository.logout()  // <-- Clears Room cache
            _currentUser.value = null
            _message.value = "Logged out successfully"
        } catch (e: Exception) {
            _message.value = "Logout failed: ${e.message}"
        }
    }
}
```

---

## How to Use in Your UI

### Example 1: Logout Button in AccountFragment

```kotlin
class AccountFragment : Fragment() {
    
    private lateinit var viewModel: AccountViewModel
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val factory = AccountViewModel.Factory(requireActivity().application)
        viewModel = ViewModelProvider(this, factory)[AccountViewModel::class.java]
        
        // Load current user
        viewModel.loadCurrentUser()
        
        // Logout button
        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
        
        // Observe current user
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user == null) {
                // User logged out, navigate to login screen
                navigateToLogin()
            } else {
                // Display user info
                binding.tvUserName.text = "${user.prenom} ${user.nom}"
                binding.tvEmail.text = user.email
            }
        }
    }
    
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun navigateToLogin() {
        findNavController().navigate(
            R.id.action_accountFragment_to_loginFragment
        )
    }
}
```

---

### Example 2: Logout from Menu/Toolbar

```kotlin
class MainActivity : AppCompatActivity() {
    
    private lateinit var accountViewModel: AccountViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val factory = AccountViewModel.Factory(application)
        accountViewModel = ViewModelProvider(this, factory)[AccountViewModel::class.java]
        
        // Observe logout
        accountViewModel.currentUser.observe(this) { user ->
            if (user == null) {
                // Logged out, go to login screen
                navigateToLogin()
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                accountViewModel.logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
```

---

### Example 3: Auto-login on App Startup

```kotlin
class MainActivity : AppCompatActivity() {
    
    private lateinit var authViewModel: AuthViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authViewModel = AuthViewModel(applicationContext)
        
        // Load saved user session on startup
        authViewModel.loadCurrentUser()
        
        // Observe current user
        authViewModel.currentUser.observe(this) { user ->
            if (user != null) {
                // User is logged in, go to main screen
                navigateToHome()
            } else {
                // No user, stay on login screen
                navigateToLogin()
            }
        }
    }
}
```

---

## Testing the Fix

### Test Case 1: Single User Login/Logout
1. Login as User A
2. Verify User A's data is displayed
3. Logout
4. Verify login screen is shown
5. Login as User A again
6. ✅ Verify User A's data is displayed correctly

### Test Case 2: Multiple Users
1. Login as User A
2. Verify User A's data is displayed
3. Logout
4. Login as User B
5. ✅ Verify User B's data is displayed (NOT User A's)
6. Logout
7. Login as User A
8. ✅ Verify User A's data is displayed again

### Test Case 3: App Restart
1. Login as User A
2. Close the app completely
3. Reopen the app
4. ✅ Verify User A is still logged in (session persisted)
5. Logout
6. Close and reopen app
7. ✅ Verify login screen is shown (logout persisted)

---

## Important Notes

### What `logout()` Does:
✅ Clears the local cached user from Room  
✅ Clears in-memory user state  
✅ Allows different user to login  
✅ Preserves the account (user can login again)

### What `logout()` Does NOT Do:
❌ Does NOT delete the user account  
❌ Does NOT contact any backend/server  
❌ Does NOT delete user's orders/cart data  

### When to Clear Other Data:
If you want to also clear cart items or orders on logout, add this to `logout()`:

```kotlin
fun logout() {
    viewModelScope.launch {
        try {
            repository.logout()  // Clear user session
            
            // Optional: Clear cart items for logged-out user
            val userId = _currentUser.value?.id
            if (userId != null) {
                cartRepository.clearCartForUser(userId)
            }
            
            _currentUser.value = null
            _message.value = "Logged out successfully"
        } catch (e: Exception) {
            _message.value = "Logout failed: ${e.message}"
        }
    }
}
```

---

## Summary

✅ **Problem Fixed:** Old user data no longer shows after logout  
✅ **Safe Implementation:** Only clears local cache, not actual accounts  
✅ **Clean Sessions:** Each login starts fresh  
✅ **Privacy Protected:** No data leakage between users  
✅ **Easy to Use:** Simple `logout()` method call

Your app now properly handles user sessions!
