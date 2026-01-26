# Copy-Paste Implementation Guide

Use this document to quickly integrate the session management fixes into your app.

---

## 1. Add SessionManager.kt

**File Path:** `app/src/main/java/com/isetr/cupcake/session/SessionManager.kt`

**Dependencies Required:**
```gradle
// build.gradle.kts (app level)
dependencies {
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
}
```

**Code:** Copy this full file
```kotlin
package com.isetr.cupcake.session

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "cupcake_session")

/**
 * SessionManager: Single source of truth for active user session.
 * Uses DataStore for persistent, non-blocking storage.
 * 
 * Key Design: SessionManager is only updated on login/logout/delete.
 * Room is separate persistence layer (never cleared on logout).
 */
class SessionManager(private val context: Context) {

    companion object {
        private val ACTIVE_USER_ID = intPreferencesKey("active_user_id")
        private val ACTIVE_USER_EMAIL = stringPreferencesKey("active_user_email")
        private val ACTIVE_USER_NAME = stringPreferencesKey("active_user_name")
        
        const val NO_USER = -1
        
        @Volatile
        private var instance: SessionManager? = null
        
        fun getInstance(context: Context): SessionManager =
            instance ?: synchronized(this) {
                instance ?: SessionManager(context).also { instance = it }
            }
    }

    /**
     * Reactive flow of active user ID.
     * -1 means no user logged in.
     */
    val activeUserIdFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_USER_ID] ?: NO_USER
    }

    /**
     * Reactive flow of active user email.
     * Empty string means no user logged in.
     */
    val activeUserEmailFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_USER_EMAIL] ?: ""
    }

    /**
     * Reactive flow of active user name.
     * Empty string means no user logged in.
     */
    val activeUserNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_USER_NAME] ?: ""
    }

    /**
     * Reactive flow of complete session info.
     * Contains userId, email, and name.
     */
    val sessionFlow: Flow<SessionInfo> = context.dataStore.data.map { preferences ->
        SessionInfo(
            userId = preferences[ACTIVE_USER_ID] ?: NO_USER,
            email = preferences[ACTIVE_USER_EMAIL] ?: "",
            name = preferences[ACTIVE_USER_NAME] ?: ""
        )
    }

    /**
     * Login: Save active user to SessionManager.
     * 
     * Call this AFTER user is verified in Room.
     * Do NOT clear Room on login.
     */
    suspend fun login(userId: Int, email: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_USER_ID] = userId
            preferences[ACTIVE_USER_EMAIL] = email
            preferences[ACTIVE_USER_NAME] = name
        }
    }

    /**
     * Logout: Clear active user from SessionManager.
     * 
     * Important: This does NOT delete from Room.
     * User data is preserved for future login.
     */
    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_USER_ID] = NO_USER
            preferences[ACTIVE_USER_EMAIL] = ""
            preferences[ACTIVE_USER_NAME] = ""
        }
    }

    /**
     * On Account Deleted: Clear session (used after Room deletion).
     * 
     * Call this AFTER deleteUserById() succeeds.
     */
    suspend fun onAccountDeleted() {
        logout()  // Clears SessionManager
    }

    /**
     * Get active user ID (synchronous).
     * Returns -1 if not logged in.
     * 
     * Use this from suspend functions.
     */
    suspend fun getActiveUserId(): Int {
        return activeUserIdFlow.first()
    }

    /**
     * Get active user email (synchronous).
     * Returns empty string if not logged in.
     */
    suspend fun getActiveUserEmail(): String {
        return activeUserEmailFlow.first()
    }

    /**
     * Get active user name (synchronous).
     * Returns empty string if not logged in.
     */
    suspend fun getActiveUserName(): String {
        return activeUserNameFlow.first()
    }

    /**
     * Session info data class.
     * Contains userId, email, and name.
     */
    data class SessionInfo(
        val userId: Int = NO_USER,
        val email: String = "",
        val name: String = ""
    ) {
        val isLoggedIn: Boolean get() = userId != NO_USER
    }
}
```

---

## 2. Update UserDao.kt

**File Path:** `app/src/main/java/com/isetr/cupcake/data/local/UserDao.kt`

**Replace entire file with:**
```kotlin
package com.isetr.cupcake.data.local

import androidx.room.*

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    /**
     * Get user by email (for login verification).
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    /**
     * Get user by ID (for profile loading).
     */
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): UserEntity?

    /**
     * Get all users (admin/debug only).
     */
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    /**
     * Delete specific user (for account deletion).
     * ✅ Use this to delete specific userId, not "last user".
     */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Int)

    /**
     * Clear all users (TESTING ONLY).
     * ❌ Do NOT use this for logout!
     */
    @Query("DELETE FROM users")
    suspend fun clearAllUsers()

    /**
     * Deprecated: getLastUser()
     * This pattern is unreliable for session management.
     * Use SessionManager.activeUserId instead.
     */
    @Query("SELECT * FROM users ORDER BY id DESC LIMIT 1")
    suspend fun getLastUser(): UserEntity?
}
```

---

## 3. Update OrderDao.kt

**File Path:** `app/src/main/java/com/isetr/cupcake/data/local/OrderDao.kt`

**Replace entire file with:**
```kotlin
package com.isetr.cupcake.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity): Long

    /**
     * Get all orders (⚠️ CAUTION: Shows all users' orders).
     * ❌ Don't use this in production.
     * Use getOrdersByUser() or getOrdersByUserFlow() instead.
     */
    @Query("SELECT * FROM orders")
    suspend fun getAllOrders(): List<OrderEntity>

    /**
     * Get orders for specific user (one-shot query).
     * ✅ Use this for immediate order fetch.
     */
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY orderDate DESC")
    suspend fun getOrdersByUser(userId: Int): List<OrderEntity>

    /**
     * Get orders for specific user (reactive flow).
     * ✅ Use this for UI updates whenever orders change.
     */
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY orderDate DESC")
    fun getOrdersByUserFlow(userId: Int): Flow<List<OrderEntity>>

    /**
     * Delete specific order.
     */
    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrder(orderId: Int)

    /**
     * Delete all orders for specific user.
     * ✅ Use this when deleting account.
     */
    @Query("DELETE FROM orders WHERE userId = :userId")
    suspend fun deleteOrdersForUser(userId: Int)
}
```

---

## 4. Update AuthRepository.kt

**File Path:** `app/src/main/java/com/isetr/cupcake/data/repository/AuthRepository.kt`

**Replace entire file with:**
```kotlin
package com.isetr.cupcake.data.repository

import android.content.Context
import com.isetr.cupcake.data.local.AppDatabase
import com.isetr.cupcake.data.local.UserEntity
import com.isetr.cupcake.session.SessionManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData

/**
 * AuthRepository: Orchestrates authentication and session management.
 * 
 * Key Separation:
 * - SessionManager: Who is currently logged in (activeUserId)
 * - Room Database: All users and historical data
 * 
 * On Login: Update SessionManager ONLY (user already in Room)
 * On Logout: Update SessionManager ONLY (keep user in Room)
 * On Delete: Delete from Room THEN update SessionManager
 */
class AuthRepository(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val userDao = database.userDao()
    private val sessionManager = SessionManager.getInstance(context)

    /**
     * Get SessionManager instance (for ViewModels to observe flows).
     */
    fun getSessionManager(): SessionManager = sessionManager

    /**
     * Register: Insert user into Room (doesn't login).
     * Return true if successful, false if email exists.
     */
    suspend fun registerUser(user: UserEntity): Boolean {
        return try {
            val existingUser = userDao.getUserByEmail(user.email)
            if (existingUser != null) {
                false  // Email already registered
            } else {
                userDao.insert(user)
                true  // Registration successful
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get user by email (for login verification).
     */
    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    /**
     * Get user by ID (for profile loading).
     */
    suspend fun getUserById(userId: Int): UserEntity? {
        return userDao.getUserById(userId)
    }

    /**
     * Login Session: Save user to SessionManager.
     * 
     * Precondition: User must already exist in Room.
     * Call this after verifying password with getUserByEmail().
     */
    suspend fun loginSession(user: UserEntity) {
        sessionManager.login(user.id, user.email, "${user.prenom} ${user.nom}")
    }

    /**
     * Get current user: Read from SessionManager, then fetch from Room.
     * 
     * This is the primary way to load the logged-in user.
     */
    suspend fun getCurrentUser(): UserEntity? {
        val userId = sessionManager.getActiveUserId()
        return if (userId > 0) {
            userDao.getUserById(userId)
        } else {
            null
        }
    }

    /**
     * Get current user as LiveData (for ViewModels).
     */
    fun getCurrentUserLiveData(): LiveData<UserEntity?> {
        return sessionManager.activeUserIdFlow.asLiveData().map { userId ->
            if (userId > 0) {
                // This will be called each time userId changes
                // In real implementation, fetch from Room in a coroutine
                try {
                    // Note: Can't call suspend from map, so return cached value
                    // Actual implementation should use switchMap with loadCurrentUser()
                    null
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        } as LiveData<UserEntity?>
    }

    /**
     * Logout: Clear SessionManager (don't touch Room).
     * 
     * User data stays in Room for future login.
     */
    suspend fun logout() {
        sessionManager.logout()
    }

    /**
     * Delete user: Remove from Room and clear SessionManager.
     * 
     * Precondition: Should only be called after backend verification.
     */
    suspend fun deleteUserById(userId: Int) {
        userDao.deleteUserById(userId)
    }

    /**
     * On account deleted: Clear SessionManager after deletion.
     */
    suspend fun onAccountDeleted() {
        sessionManager.onAccountDeleted()
    }
}
```

---

## 5. Replace AuthViewModel.kt

**File Path:** `app/src/main/java/com/isetr/cupcake/viewmodel/AuthViewModel.kt`

**Use the complete code from AuthViewModel_New.kt (provided separately)**

---

## 6. Replace AccountViewModel.kt

**File Path:** `app/src/main/java/com/isetr/cupcake/viewmodel/AccountViewModel.kt`

**Use the complete code from AccountViewModel_New.kt (provided separately)**

---

## 7. Update CupcakeApi.kt

**File Path:** `app/src/main/java/com/isetr/cupcake/data/api/CupcakeApi.kt`

**Add this endpoint:**
```kotlin
interface CupcakeApi {
    // ... existing endpoints ...

    /**
     * Delete account: Backend verifies password and deletes user.
     */
    @DELETE("/users/{id}/delete")
    suspend fun deleteAccount(
        @Path("id") userId: Int,
        @Body request: DeleteAccountRequest
    ): Response<DeleteAccountResponse>
}

data class DeleteAccountRequest(
    val userId: Int,
    val password: String  // Hashed password from UserEntity.password
)

data class DeleteAccountResponse(
    val success: Boolean,
    val message: String
)
```

---

## 8. Update MainActivity.kt

**File Path:** `app/src/main/java/com/isetr/cupcake/MainActivity.kt`

**Add to onCreate():**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Initialize AuthViewModel
    val authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)
    
    // ✅ Load current user from SessionManager on startup
    authViewModel.loadCurrentUser()
    
    // ✅ Observe session state and navigate accordingly
    authViewModel.sessionState.observe(this) { sessionInfo ->
        if (sessionInfo.isLoggedIn) {
            // User is logged in
            navigateToHomeScreen()
        } else {
            // User is not logged in
            navigateToLoginScreen()
        }
    }
}

private fun navigateToHomeScreen() {
    // Navigate to home/products screen
    // Example: navController.navigate(R.id.homeFragment)
}

private fun navigateToLoginScreen() {
    // Navigate to login screen
    // Example: navController.navigate(R.id.loginFragment)
}
```

---

## 9. Update Login Activity/Fragment

**Example: LoginFragment.kt**

```kotlin
class LoginFragment : Fragment() {
    
    private lateinit var authViewModel: AuthViewModel
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        authViewModel = ViewModelProvider(requireActivity()).get(AuthViewModel::class.java)
        
        // Login button
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            
            // ✅ Call repository.loginSession() method
            authViewModel.onLoginClicked(email, password)
        }
        
        // Observe login result
        authViewModel.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                // Navigate to home screen
                findNavController().navigate(R.id.action_login_to_home)
            }
        }
        
        // Observe errors
        authViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                authViewModel.clearError()
            }
        }
    }
}
```

---

## 10. Update Delete Account Fragment

**Example: AccountFragment.kt**

```kotlin
class AccountFragment : Fragment() {
    
    private lateinit var accountViewModel: AccountViewModel
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        accountViewModel = ViewModelProvider(requireActivity()).get(AccountViewModel::class.java)
        
        // Delete account button
        binding.deleteAccountButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        
        // Observe delete result
        accountViewModel.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_account_to_login)
            }
        }
        
        accountViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                accountViewModel.clearError()
            }
        }
    }
    
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("This action cannot be undone. Enter your password to confirm.")
            .setView(EditText(requireContext()).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                hint = "Password"
            })
            .setPositiveButton("Delete") { dialog, _ ->
                val passwordInput = (dialog as? AlertDialog)
                    ?.findViewById<EditText>(android.R.id.text1)
                    ?.text.toString()
                
                // ✅ Call deleteAccount with password verification
                accountViewModel.deleteAccount(passwordInput)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
```

---

## Backend Implementation (Node.js/Express)

**Example: Delete Account Endpoint**

```javascript
const bcrypt = require('bcrypt');

// DELETE /users/:id/delete
app.delete('/users/:id/delete', async (req, res) => {
    try {
        const userId = parseInt(req.params.id);
        const { password } = req.body;
        
        // 1. Get user from database
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ success: false, message: 'User not found' });
        }
        
        // 2. Verify password hash
        const isValidPassword = await bcrypt.compare(password, user.password);
        if (!isValidPassword) {
            return res.status(401).json({ success: false, message: 'Invalid password' });
        }
        
        // 3. Delete user and related data
        await Order.deleteMany({ userId: userId });
        await Cart.deleteMany({ userId: userId });
        await User.findByIdAndDelete(userId);
        
        // 4. Return success
        res.json({ success: true, message: 'User deleted successfully' });
        
    } catch (error) {
        res.status(500).json({ success: false, message: error.message });
    }
});
```

---

## Testing Checklist

After implementation:

- [ ] Login with User A → See A's orders
- [ ] Logout → App shows login screen
- [ ] Login with User B → See B's orders (not A's)
- [ ] App restart while B logged in → Still logged in as B
- [ ] Kill app & restart → Session restored from DataStore
- [ ] Delete B's account → Can't login as B anymore
- [ ] User A still exists & can login → A's data untouched
- [ ] Orders correctly filtered by userId
- [ ] Password verification works for login
- [ ] Password verification required for delete

---

## Common Issues & Solutions

### Issue: "getLastUser() not found"

**Solution:** Replace all `getLastUser()` calls with:
```kotlin
val userId = sessionManager.getActiveUserId()
if (userId > 0) {
    val user = userDao.getUserById(userId)
}
```

### Issue: "getAllOrders() mixing user data"

**Solution:** Replace with:
```kotlin
val userId = sessionManager.getActiveUserId()
orderDao.getOrdersByUserFlow(userId)
```

### Issue: "Logout clears all user data"

**Solution:** Remove `userDao.clearAllUsers()` from logout:
```kotlin
// ✅ Correct logout
fun logout() {
    repository.logout()  // SessionManager only
}
```

### Issue: "Wrong user deleted"

**Solution:** Always use explicit userId:
```kotlin
// ❌ Wrong
userDao.deleteLastUser()

// ✅ Correct
val userId = sessionManager.getActiveUserId()
userDao.deleteUserById(userId)
```

### Issue: "Session lost on app restart"

**Solution:** DataStore is persistent, ensure it's used:
```kotlin
// In MainActivity.onCreate()
authViewModel.loadCurrentUser()  // Restores from SessionManager
```

---

## Dependencies Summary

Add to `build.gradle.kts`:

```kotlin
dependencies {
    // DataStore for SessionManager
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    
    // Room (already added)
    implementation("androidx.room:room-runtime:2.5.2")
    
    // Retrofit (already added)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    
    // LiveData + ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
}
```

---

## Next Steps

1. **Copy files**: Copy SessionManager.kt, updated UserDao, OrderDao, AuthRepository
2. **Update ViewModels**: Replace AuthViewModel and AccountViewModel
3. **Add dependencies**: Add DataStore to build.gradle.kts
4. **Update MainActivity**: Add loadCurrentUser() and sessionState observation
5. **Add API endpoint**: Add deleteAccount() to CupcakeApi.kt
6. **Test flows**: Multi-user login/logout, order isolation, delete account
7. **Backend implementation**: Implement DELETE /users/:id/delete endpoint
