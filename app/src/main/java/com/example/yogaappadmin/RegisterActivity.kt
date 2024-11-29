package com.example.yogaappadmin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var registerButton: Button
    private lateinit var backToLoginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase
        val database = Firebase.database("https://yoga-2f931-default-rtdb.asia-southeast1.firebasedatabase.app")
        val usersRef = database.getReference("users")

        dbHelper = DatabaseHelper(this)

        // Initialize views
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        registerButton = findViewById(R.id.registerButton)
        backToLoginButton = findViewById(R.id.backToLoginButton)

        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (validateInputs(email, password, confirmPassword)) {
                // Check if email exists in Firebase
                usersRef.orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                Toast.makeText(this@RegisterActivity, "Email already exists", Toast.LENGTH_SHORT).show()
                            } else {
                                // Create user data
                                val userData = HashMap<String, Any>()
                                userData["email"] = email
                                userData["password"] = password
                                userData["role"] = "admin" // Default role is admin

                                // Generate a new user ID
                                val newUserRef = usersRef.push()

                                // Save to Firebase
                                newUserRef.setValue(userData)
                                    .addOnSuccessListener {
                                        // Also save to SQLite for offline access
                                        dbHelper.registerUser(email, password, "admin")

                                        Toast.makeText(this@RegisterActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this@RegisterActivity, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@RegisterActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }

        backToLoginButton.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Invalid email format"
            isValid = false
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.error = "Confirm password is required"
            isValid = false
        } else if (password != confirmPassword) {
            confirmPasswordInput.error = "Passwords do not match"
            isValid = false
        }

        return isValid
    }
}