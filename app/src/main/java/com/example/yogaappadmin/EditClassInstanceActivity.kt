package com.example.yogaappadmin

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import java.text.SimpleDateFormat
import java.util.*

class EditClassInstanceActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var dbRef: DatabaseReference
    private lateinit var courseSpinner: Spinner
    private lateinit var classDateEditText: EditText
    private lateinit var calendarButton: ImageButton
    private lateinit var teacherNameEditText: EditText
    private lateinit var additionalCommentsEditText: EditText
    private lateinit var saveClassInstanceButton: Button
    private var instanceId: Int = -1
    private var firebaseId: String = ""
    private val courseMap = mutableMapOf<String, CourseData>()
    private var selectedDayOfWeek: Int = Calendar.MONDAY

    data class CourseData(
        val id: Int,
        val firebaseId: String,
        val type: String,
        val day: String,
        val time: String,
        val capacity: Int,
        val duration: String,
        val price: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_class_instance)

        dbRef = FirebaseDatabase.getInstance("https://yoga-2f931-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("class_instances")
        dbHelper = DatabaseHelper(this)

        initializeViews()
        setupDateSelection()

        // Get both SQLite ID and Firebase ID from Intent
        instanceId = intent.getIntExtra("INSTANCE_ID", -1)
        firebaseId = intent.getStringExtra("FIREBASE_ID") ?: ""

        loadCoursesIntoSpinner()

        if (instanceId != -1) {
            loadClassInstanceDetails()
        }

        courseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedCourse = courseSpinner.selectedItem.toString()
                updateSelectedDayOfWeek(selectedCourse)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        saveClassInstanceButton.text = "Update Class Instance"
        saveClassInstanceButton.setOnClickListener {
            updateClassInstance()
        }
    }

    private fun initializeViews() {
        courseSpinner = findViewById(R.id.courseSpinner)
        classDateEditText = findViewById(R.id.classDate)
        calendarButton = findViewById(R.id.calendarButton)
        teacherNameEditText = findViewById(R.id.teacherName)
        additionalCommentsEditText = findViewById(R.id.additionalComments)
        saveClassInstanceButton = findViewById(R.id.saveClassInstanceButton)
    }

    private fun setupDateSelection() {
        val clickListener = View.OnClickListener {
            showCustomDatePicker()
        }

        calendarButton.setOnClickListener(clickListener)
        classDateEditText.setOnClickListener(clickListener)
    }

    private fun showCustomDatePicker() {
        val calendar = Calendar.getInstance()

        // Parse existing date if available
        val existingDate = classDateEditText.text.toString()
        if (existingDate.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = dateFormat.parse(existingDate)
                date?.let {
                    calendar.time = it
                }
            } catch (e: Exception) {
                // If parsing fails, use current date
            }
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                if (selectedCalendar.get(Calendar.DAY_OF_WEEK) == selectedDayOfWeek) {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    classDateEditText.setText(dateFormat.format(selectedCalendar.time))
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.setOnShowListener { dialog ->
            val datePicker = (dialog as DatePickerDialog).datePicker

            val validationCalendar = Calendar.getInstance()

            datePicker.minDate = System.currentTimeMillis() - 1000

            val maxDate = Calendar.getInstance().apply {
                add(Calendar.YEAR, 1)
            }
            datePicker.maxDate = maxDate.timeInMillis

            datePicker.setOnDateChangedListener { _, year, month, dayOfMonth ->
                validationCalendar.set(year, month, dayOfMonth)
                if (validationCalendar.get(Calendar.DAY_OF_WEEK) != selectedDayOfWeek) {
                    // Find the next valid date
                    while (validationCalendar.get(Calendar.DAY_OF_WEEK) != selectedDayOfWeek) {
                        validationCalendar.add(Calendar.DAY_OF_MONTH, 1)
                    }
                    datePicker.updateDate(
                        validationCalendar.get(Calendar.YEAR),
                        validationCalendar.get(Calendar.MONTH),
                        validationCalendar.get(Calendar.DAY_OF_MONTH)
                    )
                }
            }
        }

        datePickerDialog.show()
    }

    private fun updateSelectedDayOfWeek(selectedCourse: String) {
        val dayOfWeek = selectedCourse.split(" - ")[1].split(" at ")[0]
        selectedDayOfWeek = when (dayOfWeek) {
            "Monday" -> Calendar.MONDAY
            "Tuesday" -> Calendar.TUESDAY
            "Wednesday" -> Calendar.WEDNESDAY
            "Thursday" -> Calendar.THURSDAY
            "Friday" -> Calendar.FRIDAY
            "Saturday" -> Calendar.SATURDAY
            "Sunday" -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
    }

    @SuppressLint("Range")
    private fun loadCoursesIntoSpinner() {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_COURSES}", null)

        val courses = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val courseId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID))
            val type = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE))
            val day = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DAY))
            val time = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME))
            val capacity = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_CAPACITY))
            val duration = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DURATION))
            val price = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PRICE))

            val courseDetails = "$type - $day at $time"
            courses.add(courseDetails)

            courseMap[courseDetails] = CourseData(
                id = courseId,
                firebaseId = courseId.toString(),
                type = type,
                day = day,
                time = time,
                capacity = capacity,
                duration = duration,
                price = price
            )
        }
        cursor.close()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, courses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        courseSpinner.adapter = adapter
    }

    @SuppressLint("Range")
    private fun loadClassInstanceDetails() {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT i.*, c.${DatabaseHelper.COLUMN_TYPE}, c.${DatabaseHelper.COLUMN_DAY}, c.${DatabaseHelper.COLUMN_TIME} " +
                    "FROM ${DatabaseHelper.TABLE_INSTANCES} i " +
                    "JOIN ${DatabaseHelper.TABLE_COURSES} c ON i.${DatabaseHelper.COLUMN_COURSE_ID} = c.${DatabaseHelper.COLUMN_ID} " +
                    "WHERE i.${DatabaseHelper.COLUMN_ID} = ?",
            arrayOf(instanceId.toString())
        )

        if (cursor.moveToFirst()) {
            val date = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_INSTANCE_DATE))
            val teacher = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TEACHER))
            val comments = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_COMMENTS))
            val type = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE))
            val day = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DAY))
            val time = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIME))

            val courseDetails = "$type - $day at $time"
            val coursePosition = (courseSpinner.adapter as ArrayAdapter<String>).getPosition(courseDetails)

            classDateEditText.setText(date)
            teacherNameEditText.setText(teacher)
            additionalCommentsEditText.setText(comments)
            courseSpinner.setSelection(coursePosition)
        }

        cursor.close()
    }

    private fun updateClassInstance() {
        val selectedCourse = courseSpinner.selectedItem.toString()
        val courseData = courseMap[selectedCourse] ?: return
        val classDate = classDateEditText.text.toString().trim()
        val teacherName = teacherNameEditText.text.toString().trim()
        val additionalComments = additionalCommentsEditText.text.toString().trim()

        if (classDate.isEmpty() || teacherName.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Update in SQLite
        updateInSQLite(courseData.id, classDate, teacherName, additionalComments)

        // Update in Firebase
        updateInFirebase(courseData, classDate, teacherName, additionalComments)
    }

    private fun updateInSQLite(courseId: Int, classDate: String, teacherName: String, additionalComments: String) {
        val db = dbHelper.writableDatabase
        val contentValues = ContentValues().apply {
            put(DatabaseHelper.COLUMN_COURSE_ID, courseId)
            put(DatabaseHelper.COLUMN_INSTANCE_DATE, classDate)
            put(DatabaseHelper.COLUMN_TEACHER, teacherName)
            put(DatabaseHelper.COLUMN_COMMENTS, additionalComments)
        }

        try {
            db.update(
                DatabaseHelper.TABLE_INSTANCES,
                contentValues,
                "${DatabaseHelper.COLUMN_ID} = ?",
                arrayOf(instanceId.toString())
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Error updating SQLite: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            db.close()
        }
    }

    private fun updateInFirebase(
        courseData: CourseData,
        classDate: String,
        teacherName: String,
        additionalComments: String
    ) {
        val classInstanceMap = hashMapOf<String, Any>(
            "courseId" to courseData.firebaseId,
            "courseType" to courseData.type,
            "courseDay" to courseData.day,
            "courseTime" to courseData.time,
            "courseCapacity" to courseData.capacity,
            "courseDuration" to courseData.duration,
            "coursePrice" to courseData.price,
            "classDate" to classDate,
            "teacher" to teacherName,
            "comments" to additionalComments,
            "sqliteId" to instanceId
        )

        if (firebaseId.isNotEmpty()) {
            dbRef.child(firebaseId).updateChildren(classInstanceMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Class instance updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // If no Firebase ID exists, create new entry
            val newRef = dbRef.push()
            classInstanceMap["id"] = newRef.key ?: ""

            newRef.setValue(classInstanceMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Class instance updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}