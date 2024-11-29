package com.example.yogaappadmin

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class AddCourseActivity : AppCompatActivity() {
    private lateinit var backButton: Button
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var dayOfWeekSpinner: Spinner
    private lateinit var database: FirebaseDatabase
    private lateinit var courseTimeInput: TextInputEditText
    private lateinit var timePickerDialog: TimePickerDialog

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_course)

        // Initialize Firebase
        database = Firebase.database("https://yoga-2f931-default-rtdb.asia-southeast1.firebasedatabase.app")
        val coursesRef = database.getReference("courses")

        dbHelper = DatabaseHelper(this)
        backButton = findViewById(R.id.backButton)
        dayOfWeekSpinner = findViewById(R.id.dayOfWeek)
        courseTimeInput = findViewById(R.id.courseTime)

        // Initialize TimePickerDialog
        timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                // Format time as HH:mm
                val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                courseTimeInput.setText(formattedTime)
            },
            10, // Default hour
            0,  // Default minute
            true // 24 hour view
        )

        // Set click listener for courseTime input
        courseTimeInput.setOnClickListener {
            timePickerDialog.show()
        }

        // Set click listener for the end icon (clock icon)
        val timeInputLayout = findViewById<TextInputLayout>(R.id.timeInputLayout)
        timeInputLayout.setEndIconOnClickListener {
            timePickerDialog.show()
        }

        dbHelper = DatabaseHelper(this)
        backButton = findViewById(R.id.backButton)
        dayOfWeekSpinner = findViewById(R.id.dayOfWeek)
        val courseTime = findViewById<EditText>(R.id.courseTime)
        val capacity = findViewById<EditText>(R.id.capacity)
        val duration = findViewById<EditText>(R.id.duration)
        val price = findViewById<EditText>(R.id.price)
        val classType = findViewById<EditText>(R.id.classType)
        val description = findViewById<EditText>(R.id.description)

        // Set up Spinner for days of the week
        val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, daysOfWeek)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dayOfWeekSpinner.adapter = adapter

        val saveCourseButton = findViewById<Button>(R.id.saveCourseButton)
        saveCourseButton.setOnClickListener {
            if (validateInputs(courseTime, capacity, duration, price, classType)) {
                val selectedDay = dayOfWeekSpinner.selectedItem.toString()

                // Save to SQLite and get the generated ID
                val courseId = saveCourseToSQLite(
                    selectedDay,
                    courseTime.text.toString(),
                    capacity.text.toString().toInt(),
                    duration.text.toString(),
                    price.text.toString(),
                    classType.text.toString(),
                    description.text.toString()
                )

                // Only proceed if course ID was successfully generated
                if (courseId != null) {
                    // Create course data object
                    val courseData = HashMap<String, Any>().apply {
                        put("id", courseId)
                        put("day", selectedDay)
                        put("time", courseTime.text.toString())
                        put("capacity", capacity.text.toString().toInt())
                        put("duration", duration.text.toString())
                        put("price", price.text.toString())
                        put("type", classType.text.toString())
                        put("description", description.text.toString())
                    }

                    // Save to Firebase using the same ID
                    saveCourseToFirebase(courseData, coursesRef, courseId)

                    Toast.makeText(this, "Course Added Successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun validateInputs(vararg inputs: EditText): Boolean {
        for (input in inputs) {
            if (input.text.isEmpty()) {
                input.error = "This field is required"
                return false
            }
        }
        return true
    }

    private fun saveCourseToSQLite(
        day: String, time: String, capacity: Int, duration: String,
        price: String, type: String, description: String
    ): Int? {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_DAY, day)
            put(DatabaseHelper.COLUMN_TIME, time)
            put(DatabaseHelper.COLUMN_CAPACITY, capacity)
            put(DatabaseHelper.COLUMN_DURATION, duration)
            put(DatabaseHelper.COLUMN_PRICE, price)
            put(DatabaseHelper.COLUMN_TYPE, type)
            put(DatabaseHelper.COLUMN_DESCRIPTION, description)
        }

        // Insert course into SQLite and get the generated ID
        val rowId = db.insert(DatabaseHelper.TABLE_COURSES, null, values)
        db.close()

        return if (rowId != -1L) rowId.toInt() else null
    }

    private fun saveCourseToFirebase(
        courseData: HashMap<String, Any>,
        reference: com.google.firebase.database.DatabaseReference,
        courseId: Int
    ) {
        reference.child(courseId.toString()).setValue(courseData)
            .addOnSuccessListener {
                // Firebase save successful
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save to Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
