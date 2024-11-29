package com.example.yogaappadmin

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference

data class Course(
    val id: Int = 0,
    val day: String = "",
    val time: String = "",
    val capacity: Int = 0,
    val duration: String = "",
    val price: String = "",
    val type: String = "",
    val description: String = ""
)

class ViewCoursesActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var coursesListView: ListView
    private lateinit var deleteCourseButton: Button
    private lateinit var editCourseButton: Button
    private lateinit var backButton: Button
    private var selectedCourseId: Int? = null
    private val courseIdMap = mutableMapOf<Int, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_courses)

        // Initialize SQLite
        // Initialize Firebase
        firebaseDatabase = FirebaseDatabase.getInstance("https://yoga-2f931-default-rtdb.asia-southeast1.firebasedatabase.app")
        databaseReference = firebaseDatabase.getReference("courses")
        dbHelper = DatabaseHelper(this)

        coursesListView = findViewById(R.id.coursesListView)
        deleteCourseButton = findViewById(R.id.deleteCourseButton)
        editCourseButton = findViewById(R.id.editCourseButton)
        backButton = findViewById(R.id.backButton)

        loadCourses()

        coursesListView.setOnItemClickListener { _, _, position, _ ->
            selectedCourseId = courseIdMap[position]
        }

        deleteCourseButton.setOnClickListener {
            selectedCourseId?.let {
                deleteCourse(it)
                loadCourses()
                selectedCourseId = null
            }
        }

        editCourseButton.setOnClickListener {
            selectedCourseId?.let {
                val intent = Intent(this, EditCourseActivity::class.java)
                intent.putExtra("COURSE_ID", it)
                startActivity(intent)
            }
        }

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("Range")
    private fun loadCourses() {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_COURSES}", null)
        val courses = mutableListOf<String>()
        courseIdMap.clear()

        var index = 0
        while (cursor.moveToNext()) {
            val courseId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID))
            val day = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DAY))
            val time = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME))
            val capacity = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_CAPACITY))
            val duration = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DURATION))
            val price = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PRICE))
            val type = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE))
            val description = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DESCRIPTION))

            // Create Course object and sync with Firebase
            val course = Course(
                courseId, day, time, capacity, duration, price, type, description
            )
            syncCourseToFirebase(course)

            val courseDetails = "Day: $day\nTime: $time\nCapacity: $capacity\nDuration: $duration\n" +
                    "Price: $price\nType: $type\nDescription: $description"
            courses.add(courseDetails)
            courseIdMap[index] = courseId
            index++
        }
        cursor.close()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, courses)
        coursesListView.adapter = adapter
    }

    private fun syncCourseToFirebase(course: Course) {
        databaseReference.child(course.id.toString()).setValue(course)
            .addOnSuccessListener {
                // Successfully synced to Firebase
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to sync with Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteCourse(courseId: Int) {
        // Delete from SQLite
        val db = dbHelper.writableDatabase
        val result = db.delete(DatabaseHelper.TABLE_COURSES, "${DatabaseHelper.COLUMN_ID} = ?",
            arrayOf(courseId.toString()))
        db.close()

        // Delete from Firebase
        if (result > 0) {
            databaseReference.child(courseId.toString()).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Course deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to delete from Firebase: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
        }
    }
}