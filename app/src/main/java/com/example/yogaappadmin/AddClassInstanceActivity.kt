package com.example.yogaappadmin

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import java.text.SimpleDateFormat
import java.util.*

class AddClassInstanceActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var dbRef: DatabaseReference
    private lateinit var courseSpinner: Spinner
    private lateinit var classDateEditText: EditText
    private lateinit var calendarButton: ImageButton
    private lateinit var teacherNameEditText: EditText
    private lateinit var additionalCommentsEditText: EditText
    private lateinit var saveClassInstanceButton: Button
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
        courseSpinner = findViewById(R.id.courseSpinner)
        classDateEditText = findViewById(R.id.classDate)
        calendarButton = findViewById(R.id.calendarButton)
        teacherNameEditText = findViewById(R.id.teacherName)
        additionalCommentsEditText = findViewById(R.id.additionalComments)
        saveClassInstanceButton = findViewById(R.id.saveClassInstanceButton)

        loadCoursesIntoSpinner()
        setupDateSelection()

        courseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View, position: Int, id: Long) {
                val selectedCourse = courseSpinner.selectedItem.toString()
                updateSelectedDayOfWeek(selectedCourse)
                // Clear the date when course changes
                classDateEditText.text.clear()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        saveClassInstanceButton.setOnClickListener {
            saveClassInstance()
        }
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

        // Create custom date picker dialog
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                // Only set the date if it matches the required day of week
                if (selectedCalendar.get(Calendar.DAY_OF_WEEK) == selectedDayOfWeek) {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    classDateEditText.setText(dateFormat.format(selectedCalendar.time))
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Customize the date picker to disable dates that don't match the day of week
        datePickerDialog.setOnShowListener { dialog ->
            // Get the calendar view from the date picker
            val datePicker = (dialog as DatePickerDialog).datePicker

            // Create a calendar for date validation
            val validationCalendar = Calendar.getInstance()

            // Set the minimum date to today
            datePicker.minDate = System.currentTimeMillis() - 1000

            // Set the maximum date to 1 year from now
            val maxDate = Calendar.getInstance().apply {
                add(Calendar.YEAR, 1)
            }
            datePicker.maxDate = maxDate.timeInMillis

            // Set date change listener to enforce day of week restriction
            datePicker.setOnDateChangedListener { _, year, month, dayOfMonth ->
                validationCalendar.set(year, month, dayOfMonth)
                if (validationCalendar.get(Calendar.DAY_OF_WEEK) != selectedDayOfWeek) {
                    // Find the next valid date
                    while (validationCalendar.get(Calendar.DAY_OF_WEEK) != selectedDayOfWeek) {
                        validationCalendar.add(Calendar.DAY_OF_MONTH, 1)
                    }
                    // Update the date picker to show the next valid date
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

    // ... Rest of the code remains the same (loadCoursesIntoSpinner, saveClassInstance, etc.) ...

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

    private fun saveClassInstance() {
        val selectedCourse = courseSpinner.selectedItem.toString()
        val courseData = courseMap[selectedCourse] ?: return
        val classDate = classDateEditText.text.toString().trim()
        val teacherName = teacherNameEditText.text.toString().trim()
        val additionalComments = additionalCommentsEditText.text.toString().trim()

        if (classDate.isEmpty() || teacherName.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        saveToSQLite(courseData.id, classDate, teacherName, additionalComments)
        saveToFirebase(courseData, classDate, teacherName, additionalComments)
    }

    private fun saveToSQLite(
        courseId: Int,
        classDate: String,
        teacherName: String,
        additionalComments: String
    ) {
        val db = dbHelper.writableDatabase
        val contentValues = android.content.ContentValues().apply {
            put(DatabaseHelper.COLUMN_COURSE_ID, courseId)
            put(DatabaseHelper.COLUMN_INSTANCE_DATE, classDate)
            put(DatabaseHelper.COLUMN_TEACHER, teacherName)
            put(DatabaseHelper.COLUMN_COMMENTS, additionalComments)
        }
        db.insert(DatabaseHelper.TABLE_INSTANCES, null, contentValues)
        db.close()
    }

    private fun saveToFirebase(
        courseData: CourseData,
        classDate: String,
        teacherName: String,
        additionalComments: String
    ) {
        val instanceKey = dbRef.push().key

        instanceKey?.let {
            val classInstance = HashMap<String, Any>().apply {
                put("id", instanceKey)
                put("courseId", courseData.firebaseId)
                put("courseType", courseData.type)
                put("courseDay", courseData.day)
                put("courseTime", courseData.time)
                put("courseCapacity", courseData.capacity)
                put("courseDuration", courseData.duration)
                put("coursePrice", courseData.price)
                put("classDate", classDate)
                put("teacher", teacherName)
                put("comments", additionalComments)
            }

            dbRef.child(it).setValue(classInstance)
                .addOnSuccessListener {
                    Toast.makeText(this, "Class instance saved successfully.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save to Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}