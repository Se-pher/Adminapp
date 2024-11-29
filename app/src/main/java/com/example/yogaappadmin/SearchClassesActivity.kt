package com.example.yogaappadmin

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class SearchClassesActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var dbRef: DatabaseReference
    private lateinit var teacherSearch: EditText
    private lateinit var dateSearch: EditText
    private lateinit var calendarButton: ImageButton
    private lateinit var dayOfWeekSpinner: Spinner
    private lateinit var searchResultsListView: ListView
    private lateinit var backButton: Button
    private lateinit var searchTypeGroup: RadioGroup
    private lateinit var dateSearchLayout: LinearLayout
    private lateinit var dayOfWeekLayout: LinearLayout
    private lateinit var dateSearchButton: Button
    private lateinit var dayOfWeekSearchButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_classes)

        // Initialize Firebase
        dbRef = FirebaseDatabase.getInstance("https://yoga-2f931-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("class_instances")

        initializeViews()
        setupDayOfWeekSpinner()
        setupListeners()
        setupDatePicker()
    }

    private fun initializeViews() {
        dbHelper = DatabaseHelper(this)
        teacherSearch = findViewById(R.id.teacherSearch)
        dateSearch = findViewById(R.id.dateSearch)
        calendarButton = findViewById(R.id.calendarButton)
        dayOfWeekSpinner = findViewById(R.id.dayOfWeekSpinner)
        searchResultsListView = findViewById(R.id.searchResultsListView)
        backButton = findViewById(R.id.backButton)
        searchTypeGroup = findViewById(R.id.searchTypeGroup)
        dateSearchLayout = findViewById(R.id.dateSearchLayout)
        dayOfWeekLayout = findViewById(R.id.dayOfWeekLayout)
        dateSearchButton = findViewById(R.id.dateSearchButton)
        dayOfWeekSearchButton = findViewById(R.id.dayOfWeekSearchButton)
    }

    private fun setupDayOfWeekSpinner() {
        val days = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, days)
        dayOfWeekSpinner.adapter = adapter
    }

    private fun setupListeners() {
        teacherSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performTeacherSearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            updateSearchVisibility(checkedId)
        }

        dateSearchButton.setOnClickListener {
            val selectedDate = dateSearch.text.toString()
            if (selectedDate.isNotEmpty()) {
                performDateSearch(selectedDate)
            } else {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            }
        }

        dayOfWeekSearchButton.setOnClickListener {
            performDayOfWeekSearch(dayOfWeekSpinner.selectedItem.toString())
        }

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        searchResultsListView.setOnItemClickListener { _, _, position, _ ->
            val selectedClass = searchResultsListView.adapter.getItem(position) as ClassDetails
            showClassDetails(selectedClass)
        }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateSearch.setText(dateFormat.format(calendar.time))
        }

        val showDatePicker = {
            DatePickerDialog(
                this,
                datePickerDialog,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dateSearch.setOnClickListener { showDatePicker() }
        calendarButton.setOnClickListener { showDatePicker() }
    }

    private fun updateSearchVisibility(checkedId: Int) {
        teacherSearch.visibility = View.GONE
        dateSearchLayout.visibility = View.GONE
        dayOfWeekLayout.visibility = View.GONE

        when (checkedId) {
            R.id.radioTeacher -> teacherSearch.visibility = View.VISIBLE
            R.id.radioDate -> dateSearchLayout.visibility = View.VISIBLE
            R.id.radioDayOfWeek -> dayOfWeekLayout.visibility = View.VISIBLE
        }
    }

    private fun performTeacherSearch(searchText: String) {
        if (searchText.isEmpty()) {
            updateListView(emptyList())
            return
        }

        dbRef.orderByChild("teacher")
            .startAt(searchText)
            .endAt(searchText + "\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val results = mutableListOf<ClassDetails>()
                    for (childSnapshot in snapshot.children) {
                        val classInstance = extractClassDetailsFromSnapshot(childSnapshot)
                        classInstance?.let { results.add(it) }
                    }
                    updateListView(results)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SearchClassesActivity,
                        "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun performDateSearch(date: String) {
        dbRef.orderByChild("classDate")
            .equalTo(date)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val results = mutableListOf<ClassDetails>()
                    for (childSnapshot in snapshot.children) {
                        val classInstance = extractClassDetailsFromSnapshot(childSnapshot)
                        classInstance?.let { results.add(it) }
                    }
                    updateListView(results)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SearchClassesActivity,
                        "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun performDayOfWeekSearch(dayOfWeek: String) {
        dbRef.orderByChild("courseDay")
            .equalTo(dayOfWeek)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val results = mutableListOf<ClassDetails>()
                    for (childSnapshot in snapshot.children) {
                        val classInstance = extractClassDetailsFromSnapshot(childSnapshot)
                        classInstance?.let { results.add(it) }
                    }
                    updateListView(results)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SearchClassesActivity,
                        "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun extractClassDetailsFromSnapshot(snapshot: DataSnapshot): ClassDetails? {
        return try {
            ClassDetails(
                id = snapshot.child("id").value as String? ?: "",
                date = snapshot.child("classDate").value as String? ?: "",
                teacher = snapshot.child("teacher").value as String? ?: "",
                comments = snapshot.child("comments").value as String? ?: "",
                courseId = snapshot.child("courseId").value as String? ?: "",
                type = snapshot.child("courseType").value as String? ?: "",
                capacity = (snapshot.child("courseCapacity").value as Long?)?.toInt() ?: 0,
                duration = snapshot.child("courseDuration").value as String? ?: "",
                price = snapshot.child("coursePrice").value as String? ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun updateListView(results: List<ClassDetails>) {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_2,
            android.R.id.text1,
            results
        ).apply {
            setDropDownViewResource(android.R.layout.simple_list_item_2)
        }
        searchResultsListView.adapter = adapter
    }

    private fun showClassDetails(classDetails: ClassDetails) {
        Toast.makeText(
            this,
            "Class: ${classDetails.date}\nTeacher: ${classDetails.teacher}\nType: ${classDetails.type}",
            Toast.LENGTH_LONG
        ).show()
    }

    data class ClassDetails(
        val id: String,
        val date: String,
        val teacher: String,
        val comments: String,
        val courseId: String,
        val type: String,
        val capacity: Int,
        val duration: String,
        val price: String
    ) {
        override fun toString(): String {
            return "$date - $teacher - $type"
        }
    }
}