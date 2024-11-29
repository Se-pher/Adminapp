package com.example.yogaappadmin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private val database = Firebase.database("https://yoga-2f931-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val usersRef = database.getReference("users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize views
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.registerButton)

        // Set click listeners
        loginButton.setOnClickListener { performLogin() }
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = emailInput.text?.toString()?.trim() ?: ""
        val password = passwordInput.text?.toString()?.trim() ?: ""

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        loginButton.isEnabled = false

        usersRef.orderByChild("email").equalTo(email)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val dbPassword = userSnapshot.child("password").getValue(String::class.java)
                        val userRole = userSnapshot.child("role").getValue(String::class.java)

                        if (dbPassword == password) {
                            // Check if user is admin
                            if (userRole == "admin") {
                                // Login success - navigate to MainActivity
                                val intent = Intent(this, MainActivity::class.java).apply {
                                    putExtra("USER_EMAIL", email)
                                }
                                startActivity(intent)
                                finish()
                                return@addOnSuccessListener
                            } else {
                                Toast.makeText(this, "Access denied. Admin privileges required.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                }
                loginButton.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                loginButton.isEnabled = true
            }
    }
}