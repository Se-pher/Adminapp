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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditCourseActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var dayOfWeekSpinner: Spinner
    private lateinit var courseTimeInput: TextInputEditText
    private lateinit var timeInputLayout: TextInputLayout
    private lateinit var capacity: EditText
    private lateinit var duration: EditText
    private lateinit var price: EditText
    private lateinit var classType: EditText
    private lateinit var description: EditText
    private lateinit var saveCourseButton: Button
    private lateinit var backButton: Button
    private lateinit var timePickerDialog: TimePickerDialog

    // Firebase reference
    private lateinit var dbRef: DatabaseReference
    private var courseId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_course)

        val firebaseUrl = "https://yoga-2f931-default-rtdb.asia-southeast1.firebasedatabase.app"
        dbRef = FirebaseDatabase.getInstance(firebaseUrl).getReference("courses")
        dbHelper = DatabaseHelper(this)

        initializeViews()
        setupTimePickerDialog()
        setupSpinner()

        courseId = intent.getIntExtra("COURSE_ID", -1)
        loadCourseDetails()

        setupClickListeners()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        dayOfWeekSpinner = findViewById(R.id.dayOfWeek)
        courseTimeInput = findViewById(R.id.courseTime)
        timeInputLayout = findViewById(R.id.timeInputLayout)
        capacity = findViewById(R.id.capacity)
        duration = findViewById(R.id.duration)
        price = findViewById(R.id.price)
        classType = findViewById(R.id.classType)
        description = findViewById(R.id.description)
        saveCourseButton = findViewById(R.id.saveCourseButton)
    }

    private fun setupTimePickerDialog() {
        timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                courseTimeInput.setText(formattedTime)
            },
            10,
            0,
            true
        )

        // Set click listeners for time picker
        courseTimeInput.setOnClickListener {
            timePickerDialog.show()
        }

        timeInputLayout.setEndIconOnClickListener {
            timePickerDialog.show()
        }
    }

    private fun setupSpinner() {
        val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, daysOfWeek)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dayOfWeekSpinner.adapter = adapter
    }

    private fun setupClickListeners() {
        saveCourseButton.setOnClickListener {
            if (validateInputs()) {
                updateCourseInSQLite()
                updateCourseInFirebase()
                Toast.makeText(this, "Course updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        backButton.setOnClickListener {
            val intent = Intent(this, ViewCoursesActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("Range")
    private fun loadCourseDetails() {
        courseId?.let {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM ${DatabaseHelper.TABLE_COURSES} WHERE ${DatabaseHelper.COLUMN_ID} = ?",
                arrayOf(it.toString())
            )
            if (cursor.moveToFirst()) {
                // Get the day value and set spinner selection
                val day = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DAY))
                val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                val position = daysOfWeek.indexOf(day)
                if (position != -1) {
                    dayOfWeekSpinner.setSelection(position)
                }

                courseTimeInput.setText(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME)))
                capacity.setText(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_CAPACITY)).toString())
                duration.setText(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DURATION)))
                price.setText(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PRICE)))
                classType.setText(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE)))
                description.setText(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DESCRIPTION)))
            }
            cursor.close()

            // Sync with Firebase
            dbRef.child(it.toString()).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        updateCourseInFirebase()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditCourseActivity, "Failed to check Firebase data", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun validateInputs(): Boolean {
        if (courseTimeInput.text.toString().isEmpty() || capacity.text.isEmpty() ||
            duration.text.isEmpty() || price.text.isEmpty() ||
            classType.text.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun updateCourseInSQLite() {
        courseId?.let {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_DAY, dayOfWeekSpinner.selectedItem.toString())
                put(DatabaseHelper.COLUMN_TIME, courseTimeInput.text.toString())
                put(DatabaseHelper.COLUMN_CAPACITY, capacity.text.toString().toInt())
                put(DatabaseHelper.COLUMN_DURATION, duration.text.toString())
                put(DatabaseHelper.COLUMN_PRICE, price.text.toString())
                put(DatabaseHelper.COLUMN_TYPE, classType.text.toString())
                put(DatabaseHelper.COLUMN_DESCRIPTION, description.text.toString())
            }
            db.update(
                DatabaseHelper.TABLE_COURSES,
                values,
                "${DatabaseHelper.COLUMN_ID} = ?",
                arrayOf(it.toString())
            )
            db.close()
        }
    }

    private fun updateCourseInFirebase() {
        courseId?.let {
            val courseMap = HashMap<String, Any>()
            courseMap["id"] = it
            courseMap["day"] = dayOfWeekSpinner.selectedItem.toString()
            courseMap["time"] = courseTimeInput.text.toString()
            courseMap["capacity"] = capacity.text.toString().toInt()
            courseMap["duration"] = duration.text.toString()
            courseMap["price"] = price.text.toString()
            courseMap["type"] = classType.text.toString()
            courseMap["description"] = description.text.toString()

            dbRef.child(it.toString()).setValue(courseMap)
                .addOnSuccessListener {
                    // Firebase update successful
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}