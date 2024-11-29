package com.example.yogaappadmin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var addClassInstanceButton = findViewById<Button>(R.id.addClassInstanceButton)
        val addCourseButton = findViewById<Button>(R.id.addCourseButton)
        val viewCoursesButton = findViewById<Button>(R.id.viewCoursesButton)
        val searchButton = findViewById<Button>(R.id.searchButton)
        val viewClassInstance = findViewById<Button>(R.id.viewClassInstancesButton)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        addCourseButton.setOnClickListener {
            startActivity(Intent(this, AddCourseActivity::class.java))
        }

        viewCoursesButton.setOnClickListener {
            startActivity(Intent(this, ViewCoursesActivity::class.java))
        }

        searchButton.setOnClickListener {
            startActivity(Intent(this, SearchClassesActivity::class.java))
        }

        addClassInstanceButton.setOnClickListener {
            startActivity(Intent(this, AddClassInstanceActivity::class.java))
        }

        viewClassInstance.setOnClickListener {
            startActivity(Intent(this, ViewClassInstancesActivity::class.java))
        }

        logoutButton.setOnClickListener {
            // Chỉ chuyển về LoginActivity khi người dùng click vào nút logout
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}