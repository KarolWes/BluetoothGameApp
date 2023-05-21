package com.example.bluetoothgame

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.P)
class DBInternal(context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int):
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {
    val dateFormatter: DateTimeFormatter =  DateTimeFormatter.ofPattern("dd-MM-yyyy")
    companion object {
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "internal.db"
        val TABLE_NAME = "internal"
        val COUNTER = "counter"


        val USER = "username"
        val EMAIL = "email"
        val TOKEN = "token"
        val TOKEN_DATE = "token_generation_date"
        val REF_RATE = "refresh_rate"
        val PAIRED = "scan_paired"
        val NONAME = "scan_no_name"

    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_INTERN_TABLE = ("CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME +
                "(" +
                USER + " TEXT PRIMARY KEY," +
                EMAIL + " TEXT," +
                TOKEN + " TEXT," +
                TOKEN_DATE + " TEXT," +
                REF_RATE + " INTEGER," +
                PAIRED + " INTEGER," +
                NONAME + " INTEGER" +
                ")")
        db.execSQL(CREATE_INTERN_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun getVals():ArrayList<String>? {
        val q = "SELECT * FROM $TABLE_NAME"
        val db = this.writableDatabase
        val cursor = db.rawQuery(q, null)
        var ans: ArrayList<String>? = null
        if(cursor.moveToFirst()){
            val user = cursor.getString(0)
            Log.i("sql", user)
            val email = cursor.getString(1)
            Log.i("sql", email)
            val token = cursor.getString(2)
            Log.i("sql", token)
            val ref = cursor.getString(3)
            Log.i("sql", ref)
            val p = cursor.getString(4)
            Log.i("sql", p)
            val noName = cursor.getString(5)
            Log.i("sql", noName)
            ans = arrayListOf(user, email, token, ref, p, noName)
            cursor.close()
        }
        db.close()
        return ans
    }

    fun getToken():String{
        val q = "SELECT $TOKEN $TOKEN_DATE FROM $TABLE_NAME"
        val db = this.writableDatabase
        val cursor = db.rawQuery(q, null)
        var t = ""
        var d: LocalDate? = null
        if(cursor.moveToFirst()){
            t = cursor.getString(0)
            d = LocalDate.parse(cursor.getString(1), dateFormatter)
            cursor.close()
        }
        db.close()
        val now = LocalDate.now()
        if(Period.between(d, now).days >= 7)
        {
            t = ""
        }
        return t
    }

    fun updateParameters(user:String, params:ArrayList<Int>){
        val old = this.getVals()!!
        val values = ContentValues()
        values.put(USER, old[0])
        values.put(EMAIL, old[1])
        values.put(TOKEN, old[2])

        values.put(REF_RATE, params[0])
        values.put(PAIRED, params[1])
        values.put(NONAME, params[2])
        val db = this.writableDatabase
        db.update(TABLE_NAME, values, "$USER=?", arrayOf(user))
        db.close()
    }

    fun setDefault(ref_rate:Int, paired: Int, no_name: Int){
        val values = ContentValues()
        values.put(USER, "")
        values.put(EMAIL, "")
        values.put(TOKEN, "")

        values.put(REF_RATE, ref_rate)
        Log.i("sql", ref_rate.toString())
        values.put(PAIRED, paired)
        Log.i("sql", paired.toString())
        values.put(NONAME, no_name)
        Log.i("sql", values.toString())

        val db = this.writableDatabase
        val res = db.insert(TABLE_NAME,null, values)
        Log.i("sql", "insert status: $res")
        db.close()
    }

    fun logIn(user:String, email:String, token:String){
        val old = this.getVals()!!
        val values = ContentValues()
        values.put(USER, user)
        values.put(EMAIL, email)
        values.put(TOKEN, token)
        values.put(TOKEN_DATE, LocalDateTime.now().toString())
        values.put(REF_RATE, old[3])
        values.put(PAIRED, old[4])
        values.put(NONAME, old[5])

        val db = this.writableDatabase
        this.clear()
        db.insert(TABLE_NAME,null, values)
        db.close()
    }

    fun logOut(user: String){
        val old = this.getVals()!!
        val values = ContentValues()
        values.put(USER, "")
        values.put(EMAIL, "")
        values.put(TOKEN, "")

        values.put(REF_RATE, old[3])
        values.put(PAIRED, old[4])
        values.put(NONAME, old[5])
        val db = this.writableDatabase
        db.update(TABLE_NAME, values, "$USER=?", arrayOf(user))
        db.close()
    }

    fun exists():Boolean{
        val ans = getVals()
        return ans != null
    }

    fun clear(){
        val db = this.writableDatabase;
        onUpgrade(db, 1, 2)
    }
}

