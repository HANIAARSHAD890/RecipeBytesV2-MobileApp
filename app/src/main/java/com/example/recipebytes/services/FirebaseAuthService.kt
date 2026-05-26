package com.example.recipebytes.services

import android.util.Log
import com.example.recipebytes.models.User
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.FirebaseDatabase

class FirebaseAuthService {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    companion object {
        private const val TAG = "FirebaseAuthService"
        private const val USERS_NODE = "users"
    }

    // ============ SIGN UP ============

    /**
     * Sign Up: Create new user with email & password
     * Firebase Auth handles password encryption
     */
    fun signUp(
        email: String,
        password: String,
        onSuccess: (userId: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        // Validate input
        if (!validateEmail(email)) {
            onError("Invalid email format")
            return
        }

        if (!validatePassword(password)) {
            onError("Password must be at least 6 characters")
            return
        }

        Log.d(TAG, "🔐 Attempting sign up with email: $email")

        // Step 1: Create user in Firebase Auth (handles password encryption)
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: ""
                Log.d(TAG, " Auth account created! UID: $userId")

                // Step 2: Save to Realtime Database (NO password stored!)
                val user = User(
                    uid = userId,
                    email = email,
                    createdAt = System.currentTimeMillis()
                )

                saveUserToDatabase(userId, user,
                    onSuccess = {
                        Log.d(TAG, " User saved to database!")
                        onSuccess(userId)
                    },
                    onError = { error ->
                        Log.e(TAG, " Database error: $error")
                        onError(error)
                    }
                )
            }
            .addOnFailureListener { error ->
                Log.e(TAG, " Sign up error: ${error.message}")
                val errorMessage = parseAuthError(error)
                onError(errorMessage)
            }
    }

    // ============ SIGN IN ============

    /**
     * Sign In: Authenticate user with email & password
     * Firebase Auth verifies password
     */
    fun signIn(
        email: String,
        password: String,
        onSuccess: (userId: String, user: User?) -> Unit,
        onError: (error: String) -> Unit
    ) {
        if (email.isEmpty() || password.isEmpty()) {
            onError("Email and password cannot be empty")
            return
        }

        Log.d(TAG, "🔐 Attempting sign in with email: $email")

        // Step 1: Authenticate with Firebase Auth (verifies password)
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: ""
                Log.d(TAG, " Sign in successful! UID: $userId")

                // Step 2: Fetch user data from database
                fetchUserFromDatabase(userId,
                    onSuccess = { user ->
                        Log.d(TAG, " User data fetched: ${user?.email}")
                        onSuccess(userId, user)
                    },
                    onError = { error ->
                        Log.e(TAG, " Could not fetch user data: $error")
                        // Still return userId even if database fetch fails
                        onSuccess(userId, null)
                    }
                )
            }
            .addOnFailureListener { error ->
                Log.e(TAG, " Sign in error: ${error.message}")
                val errorMessage = parseAuthError(error)
                onError(errorMessage)
            }
    }

    // ============ DATABASE OPERATIONS ============

    /**
     * Save user to Realtime Database
     */
    private fun saveUserToDatabase(
        userId: String,
        user: User,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        database.child(USERS_NODE).child(userId).setValue(user)
            .addOnSuccessListener {
                Log.d(TAG, "📝 User saved to DB: $userId")
                onSuccess()
            }
            .addOnFailureListener { error ->
                Log.e(TAG, " DB Save error: ${error.message}")
                onError(error.message ?: "Failed to save user")
            }
    }

    /**
     * Fetch user from Realtime Database
     */
    fun fetchUserFromDatabase(
        userId: String,
        onSuccess: (user: User?) -> Unit,
        onError: (error: String) -> Unit
    ) {
        Log.d(TAG, "📥 Fetching user: $userId")

        database.child(USERS_NODE).child(userId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)
                    Log.d(TAG, "User fetched: ${user?.email}")
                    onSuccess(user)
                } else {
                    Log.w(TAG, " User not found in database: $userId")
                    onSuccess(null)
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, " DB Fetch error: ${error.message}")
                onError(error.message ?: "Failed to fetch user")
            }
    }

    /**
     * Update user data in database
     */
    fun updateUser(
        userId: String,
        updates: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "✏️ Updating user: $userId")

        database.child(USERS_NODE).child(userId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Log.d(TAG, "User updated!")
                onSuccess()
            }
            .addOnFailureListener { error ->
                Log.e(TAG, " Update error: ${error.message}")
                onError(error.message ?: "Failed to update user")
            }
    }

    /**
     * Delete user from database
     */
    fun deleteUserFromDatabase(
        userId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "🗑️ Deleting user: $userId")

        database.child(USERS_NODE).child(userId)
            .removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "User deleted from database!")
                onSuccess()
            }
            .addOnFailureListener { error ->
                Log.e(TAG, " Delete error: ${error.message}")
                onError(error.message ?: "Failed to delete user")
            }
    }

    /**
     * Get all users (for testing/admin)
     */
    fun getAllUsers(
        onSuccess: (List<User>) -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "📥 Fetching all users...")

        database.child(USERS_NODE).get()
            .addOnSuccessListener { snapshot ->
                val users = mutableListOf<User>()
                snapshot.children.forEach { child ->
                    val user = child.getValue(User::class.java)
                    if (user != null) users.add(user)
                }
                Log.d(TAG, "Fetched ${users.size} users")
                onSuccess(users)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, " Error fetching users: ${error.message}")
                onError(error.message ?: "Failed to fetch users")
            }
    }

    // ============ AUTHENTICATION STATE ============

    /**
     * Get current logged-in user ID
     */
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    /**
     * Get current user's email
     */
    fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Sign out user
     */
    fun signOut() {
        firebaseAuth.signOut()
        Log.d(TAG, "👋 User signed out")
    }

    // ============ VALIDATION HELPERS ============

    /**
     * Validate email format
     */
    private fun validateEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    /**
     * Validate password strength
     */
    private fun validatePassword(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Parse Firebase Auth errors to user-friendly messages
     */
    private fun parseAuthError(error: Exception): String {
        return when {
            error.message?.contains("already in use") == true ->
                "Email already registered. Please sign in."
            error.message?.contains("invalid email") == true ->
                "Invalid email format. Please check and try again."
            error.message?.contains("no user record") == true ->
                "User not found. Please sign up first."
            error.message?.contains("password is invalid") == true ->
                "Incorrect password. Please try again."
            error.message?.contains("NETWORK_ERROR") == true ->
                "Network error. Please check your internet connection."
            error.message?.contains("too many requests") == true ->
                "Too many attempts. Please try later."
            else -> error.message ?: "An error occurred. Please try again."
        }
    }
}
