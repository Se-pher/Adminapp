package com.example.yogaappadmin

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class ViewClassInstancesActivity : AppCompatActivity() {

    private lateinit var instancesListView: ListView
    private lateinit var deleteInstanceButton: Button
    private lateinit var editInstanceButton: Button
    private lateinit var backButton: Button
    private var selectedInstancePosition: Int = -1
    private val instancesList = mutableListOf<ClassInstance>()
    private lateinit var database: DatabaseReference
    private lateinit var dbHelper: DatabaseHelper

    data class ClassInstance(
        val id: String = "",
        val firebaseId: String = "",
        val sqliteId: Int = -1,
        val date: String = "",
        val teacher: String = "",
        val courseId: Int = -1,
        val comments: String = "",
        val courseName: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_class_instances)

        database = FirebaseDatabase.getInstance("https://yoga-2f931-default-rtdb.asia-southeast1.firebasedatabase.app")
            .reference.child("class_instances")

        dbHelper = DatabaseHelper(this)
        initializeViews()
        setupListeners()
        loadClassInstances()
    }

    private fun initializeViews() {
        instancesListView = findViewById(R.id.instancesListView)
        deleteInstanceButton = findViewById(R.id.deleteInstanceButton)
        editInstanceButton = findViewById(R.id.editInstanceButton)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupListeners() {
        instancesListView.setOnItemClickListener { _, _, position, _ ->
            selectedInstancePosition = position
            highlightSelectedItem()
        }

        deleteInstanceButton.setOnClickListener {
            if (selectedInstancePosition != -1) {
                val instance = instancesList[selectedInstancePosition]
                deleteClassInstance(instance)
            } else {
                Toast.makeText(this, "Please select an instance first", Toast.LENGTH_SHORT).show()
            }
        }

        editInstanceButton.setOnClickListener {
            if (selectedInstancePosition != -1) {
                val instance = instancesList[selectedInstancePosition]
                val intent = Intent(this, EditClassInstanceActivity::class.java).apply {
                    putExtra("INSTANCE_ID", instance.sqliteId)
                    putExtra("FIREBASE_ID", instance.firebaseId)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select an instance first", Toast.LENGTH_SHORT).show()
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun highlightSelectedItem() {
        (instancesListView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    @SuppressLint("Range")
    private fun loadClassInstances() {
        val localInstances = loadFromSQLite()

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                instancesList.clear()
                instancesList.addAll(localInstances)

                for (instanceSnapshot in snapshot.children) {
                    try {
                        val firebaseId = instanceSnapshot.key ?: continue

                        // Safely convert sqliteId from String or Long to Int
                        val sqliteId = when (val sqliteIdValue = instanceSnapshot.child("sqliteId").value) {
                            is Long -> sqliteIdValue.toInt()
                            is String -> sqliteIdValue.toIntOrNull() ?: -1
                            is Int -> sqliteIdValue
                            else -> -1
                        }

                        // Skip if already loaded from SQLite
                        if (instancesList.any { it.sqliteId == sqliteId }) continue

                        // Safely convert courseId from String or Long to Int
                        val courseId = when (val courseIdValue = instanceSnapshot.child("courseId").value) {
                            is Long -> courseIdValue.toInt()
                            is String -> courseIdValue.toIntOrNull() ?: -1
                            is Int -> courseIdValue
                            else -> -1
                        }

                        val instance = ClassInstance(
                            firebaseId = firebaseId,
                            sqliteId = sqliteId,
                            date = instanceSnapshot.child("date").getValue(String::class.java) ?: "",
                            teacher = instanceSnapshot.child("teacher").getValue(String::class.java) ?: "",
                            courseId = courseId,
                            comments = instanceSnapshot.child("comments").getValue(String::class.java) ?: "",
                            courseName = getCourseNameById(courseId)
                        )
                        instancesList.add(instance)
                    } catch (e: Exception) {
                        // Log the error but continue processing other instances
                        e.printStackTrace()
                        Toast.makeText(
                            this@ViewClassInstancesActivity,
                            "Error loading instance: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                updateListView()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ViewClassInstancesActivity,
                    "Error loading instances: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    @SuppressLint("Range")
    private fun loadFromSQLite(): List<ClassInstance> {
        val localInstances = mutableListOf<ClassInstance>()
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery("""
            SELECT i.*, c.${DatabaseHelper.COLUMN_DAY}, c.${DatabaseHelper.COLUMN_TIME}
            FROM ${DatabaseHelper.TABLE_INSTANCES} i
            LEFT JOIN ${DatabaseHelper.TABLE_COURSES} c 
            ON i.${DatabaseHelper.COLUMN_COURSE_ID} = c.${DatabaseHelper.COLUMN_ID}
        """, null)

        cursor.use {
            while (it.moveToNext()) {
                val sqliteId = it.getInt(it.getColumnIndex(DatabaseHelper.COLUMN_ID))
                val courseId = it.getInt(it.getColumnIndex(DatabaseHelper.COLUMN_COURSE_ID))
                val day = it.getString(it.getColumnIndex(DatabaseHelper.COLUMN_DAY))
                val time = it.getString(it.getColumnIndex(DatabaseHelper.COLUMN_TIME))

                localInstances.add(ClassInstance(
                    sqliteId = sqliteId,
                    date = it.getString(it.getColumnIndex(DatabaseHelper.COLUMN_INSTANCE_DATE)),
                    teacher = it.getString(it.getColumnIndex(DatabaseHelper.COLUMN_TEACHER)),
                    courseId = courseId,
                    comments = it.getString(it.getColumnIndex(DatabaseHelper.COLUMN_COMMENTS)),
                    courseName = "$day at $time"
                ))
            }
        }
        return localInstances
    }

    @SuppressLint("Range")
    private fun getCourseNameById(courseId: Int): String {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("""
            SELECT ${DatabaseHelper.COLUMN_DAY}, ${DatabaseHelper.COLUMN_TIME}
            FROM ${DatabaseHelper.TABLE_COURSES}
            WHERE ${DatabaseHelper.COLUMN_ID} = ?
        """, arrayOf(courseId.toString()))

        cursor.use {
            if (it.moveToFirst()) {
                val day = it.getString(it.getColumnIndex(DatabaseHelper.COLUMN_DAY))
                val time = it.getString(it.getColumnIndex(DatabaseHelper.COLUMN_TIME))
                return "$day at $time"
            }
        }
        return "Unknown Course"
    }

    private fun updateListView() {
        val displayList = instancesList.map { instance ->
            """
            Date: ${instance.date}
            Teacher: ${instance.teacher}
            Course: ${instance.courseName}
            ${if (instance.comments.isNotEmpty()) "Comments: ${instance.comments}" else ""}
            """.trimIndent()
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_activated_1,
            displayList
        )
        instancesListView.adapter = adapter
    }

    private fun deleteClassInstance(instance: ClassInstance) {
        // Delete from SQLite
        if (instance.sqliteId != -1) {
            val db = dbHelper.writableDatabase
            try {
                db.delete(
                    DatabaseHelper.TABLE_INSTANCES,
                    "${DatabaseHelper.COLUMN_ID} = ?",
                    arrayOf(instance.sqliteId.toString())
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Error deleting from SQLite: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Delete from Firebase
        if (instance.firebaseId.isNotEmpty()) {
            database.child(instance.firebaseId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Instance deleted successfully", Toast.LENGTH_SHORT).show()
                    selectedInstancePosition = -1
                    loadClassInstances()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error deleting from Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Instance deleted from local database", Toast.LENGTH_SHORT).show()
            selectedInstancePosition = -1
            loadClassInstances()
        }
    }

    override fun onResume() {
        super.onResume()
        loadClassInstances()
        selectedInstancePosition = -1
    }
}