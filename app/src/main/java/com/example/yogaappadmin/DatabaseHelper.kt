package com.example.yogaappadmin

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "YogaCourses.db"
        private const val DATABASE_VERSION = 3  // Tăng version lên để trigger onUpgrade
        const val TABLE_COURSES = "courses"
        const val TABLE_INSTANCES = "class_instances"
        const val TABLE_USERS = "users"

        // Course Table Columns
        const val COLUMN_ID = "_id"
        const val COLUMN_DAY = "day"
        const val COLUMN_TIME = "time"
        const val COLUMN_CAPACITY = "capacity"
        const val COLUMN_DURATION = "duration"
        const val COLUMN_PRICE = "price"
        const val COLUMN_TYPE = "type"
        const val COLUMN_DESCRIPTION = "description"

        // Class Instance Table Columns
        const val COLUMN_INSTANCE_DATE = "date"
        const val COLUMN_TEACHER = "teacher"
        const val COLUMN_COMMENTS = "comments"
        const val COLUMN_COURSE_ID = "course_id"

        // User Table Columns
        const val COLUMN_USER_ID = "_id"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_ROLE = "role"  // Thêm cột role
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Courses Table
        val createCoursesTable = """
            CREATE TABLE $TABLE_COURSES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DAY TEXT NOT NULL,
                $COLUMN_TIME TEXT NOT NULL,
                $COLUMN_CAPACITY INTEGER NOT NULL,
                $COLUMN_DURATION TEXT NOT NULL,
                $COLUMN_PRICE TEXT NOT NULL,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT
            )
        """.trimIndent()

        // Create Class Instances Table
        val createInstancesTable = """
            CREATE TABLE $TABLE_INSTANCES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_INSTANCE_DATE TEXT NOT NULL,
                $COLUMN_TEACHER TEXT NOT NULL,
                $COLUMN_COMMENTS TEXT,
                $COLUMN_COURSE_ID INTEGER,
                FOREIGN KEY($COLUMN_COURSE_ID) REFERENCES $TABLE_COURSES($COLUMN_ID)
            )
        """.trimIndent()

        // Create Users Table với cột role
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EMAIL TEXT NOT NULL UNIQUE,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_ROLE TEXT NOT NULL DEFAULT 'admin'
            )
        """.trimIndent()

        db.execSQL(createCoursesTable)
        db.execSQL(createInstancesTable)
        db.execSQL(createUsersTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Xử lý upgrade cẩn thận để không mất dữ liệu
        if (oldVersion < 3) {
            // Backup data from users table if it exists
            try {
                // Create temp table with new schema
                db.execSQL("""
                    CREATE TABLE users_backup (
                        _id INTEGER PRIMARY KEY AUTOINCREMENT,
                        email TEXT NOT NULL UNIQUE,
                        password TEXT NOT NULL,
                        role TEXT NOT NULL DEFAULT 'admin'
                    )
                """.trimIndent())

                // Copy data from old table to temp table
                db.execSQL("""
                    INSERT INTO users_backup (email, password)
                    SELECT email, password FROM users
                """.trimIndent())

                // Drop old table
                db.execSQL("DROP TABLE users")

                // Rename temp table to users
                db.execSQL("ALTER TABLE users_backup RENAME TO users")
            } catch (e: Exception) {
                // If anything goes wrong, just recreate the table
                db.execSQL("DROP TABLE IF EXISTS users")
                db.execSQL("""
                    CREATE TABLE $TABLE_USERS (
                        $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COLUMN_EMAIL TEXT NOT NULL UNIQUE,
                        $COLUMN_PASSWORD TEXT NOT NULL,
                        $COLUMN_ROLE TEXT NOT NULL DEFAULT 'admin'
                    )
                """.trimIndent())
            }
        }
    }

    // Register new user with role
    fun registerUser(email: String, password: String, role: String = "admin"): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_ROLE, role)
        }

        return try {
            db.insertOrThrow(TABLE_USERS, null, contentValues)
            true
        } catch (e: Exception) {
            false
        } finally {
            db.close()
        }
    }

    // Login user
    fun loginUser(email: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_USER_ID),
            "$COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(email, password),
            null,
            null,
            null
        )

        val result = cursor.count > 0
        cursor.close()
        db.close()
        return result
    }

    // Check if email exists
    fun isEmailExists(email: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_USER_ID),
            "$COLUMN_EMAIL = ?",
            arrayOf(email),
            null,
            null,
            null
        )

        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    // Get user role
    fun getUserRole(email: String): String? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ROLE),
            "$COLUMN_EMAIL = ?",
            arrayOf(email),
            null,
            null,
            null
        )

        var role: String? = null
        if (cursor.moveToFirst()) {
            val roleColumnIndex = cursor.getColumnIndex(COLUMN_ROLE)
            if (roleColumnIndex != -1) {
                role = cursor.getString(roleColumnIndex)
            }
        }
        cursor.close()
        db.close()
        return role
    }
}