package com.example.bluetoothgame

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.LocalDate
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

        val ID = "user_id"
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
                ID + " TEXT PRIMARY KEY, "+
                USER + " TEXT, " +
                EMAIL + " TEXT, " +
                TOKEN + " TEXT, " +
                TOKEN_DATE + " TEXT, " +
                REF_RATE + " INTEGER, " +
                PAIRED + " INTEGER, " +
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
            val id = cursor.getString(0)
            val user = cursor.getString(1)
            Log.i("sql", user)
            val email = cursor.getString(2)
            Log.i("sql", email)
            val token = cursor.getString(3)
            Log.i("sql", token)
            val tokenDate = cursor.getString(4)
            Log.i("sql", tokenDate)
            val ref = cursor.getString(5)
            Log.i("sql", ref)
            val p = cursor.getString(6)
            Log.i("sql", p)
            val noName = cursor.getString(7)
            Log.i("sql", noName)
            ans = arrayListOf(id, user, email, token, tokenDate, ref, p, noName)
            cursor.close()
        }
        db.close()
        return ans
    }

    fun getToken():String{
        val q = "SELECT $TOKEN, $TOKEN_DATE FROM $TABLE_NAME"
        val db = this.writableDatabase
        val cursor = db.rawQuery(q, null)
        var t = ""
        var d: LocalDate? = null
        if(cursor.moveToFirst()){
            t = cursor.getString(0)
            if(t != ""){
                d = LocalDate.parse(cursor.getString(1))
            }
            cursor.close()
        }
        db.close()
        if(t == ""){
            return t
        }
        val now = LocalDate.now()
        if(Period.between(d, now).days >= 7)
        {
            t = ""
        }
        Log.i("sql", "Login token -> $t")
        return t
    }

    fun updateParameters(userId:String, params:ArrayList<Int>){
        val old = this.getVals()!!
        val values = ContentValues()
        values.put(ID, old[0])
        values.put(USER, old[1])
        values.put(EMAIL, old[2])
        values.put(TOKEN, old[3])
        values.put(TOKEN_DATE, old[4])

        values.put(REF_RATE, params[0])
        values.put(PAIRED, params[1])
        values.put(NONAME, params[2])
        val db = this.writableDatabase
        db.update(TABLE_NAME, values, "$ID=?", arrayOf(userId))
        db.close()
    }

    fun setDefault(ref_rate:Int, paired: Int, no_name: Int){
        val values = ContentValues()
        values.put(ID, "")
        values.put(USER, "")
        values.put(EMAIL, "")
        values.put(TOKEN, "")
        values.put(TOKEN_DATE, "")

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

    fun logIn(id:String, user:String, email:String, token:String){
        val old = this.getVals()!!
        val values = ContentValues()
        values.put(ID, id)
        values.put(USER, user)
        values.put(EMAIL, email)
        values.put(TOKEN, token)
        values.put(TOKEN_DATE, LocalDate.now().toString())
        values.put(REF_RATE, old[5])
        values.put(PAIRED, old[6])
        values.put(NONAME, old[7])

        val db = this.writableDatabase
        this.clear()
        val res = db.insert(TABLE_NAME,null, values)
        Log.i("sql", "Log in status -> $res")
        db.close()
    }

    fun logOut(id: String){
        val old = this.getVals()!!
        val values = ContentValues()
        values.put(ID, "")
        values.put(USER, "")
        values.put(EMAIL, "")
        values.put(TOKEN, "")
        values.put(TOKEN_DATE, "")

        values.put(REF_RATE, old[5])
        values.put(PAIRED, old[6])
        values.put(NONAME, old[7])
        val db = this.writableDatabase
        db.update(TABLE_NAME, values, "$ID=?", arrayOf(id))
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


class DBOwnedDevices(context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int):
    SQLiteOpenHelper(context, DBOwnedDevices.DATABASE_NAME, factory, DBOwnedDevices.DATABASE_VERSION){
    companion object {
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "owned.db"
        val TABLE_NAME = "owned"
        val COUNTER = "counter"

        val ID = "user_id"
        val MAC = "mac_address"
        val NAME = "device_name"

    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_OWNED_TABLE = ("CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME +
                "(" +
                ID + " TEXT PRIMARY KEY, "+
                MAC + " TEXT, " +
                NAME + " TEXT" +
                ")")
        db.execSQL(CREATE_OWNED_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun put(user: String, device: Device){
        if(isRegistered(device.address) == null){
            val values = ContentValues()
            values.put(ID, user)
            values.put(MAC, device.address)
            values.put(NAME, device.name)

            val db = this.writableDatabase
            val res = db.insert(TABLE_NAME,null, values)
            Log.i("sql", "Device registering status -> $res")
            db.close()
        }

    }

    fun isRegistered(adr:String): String? {
        val q = "SELECT ID FROM $TABLE_NAME WHERE $MAC ='$adr'"
        val db = this.writableDatabase
        val cursor = db.rawQuery(q, null)
        var id:String? = null
        if(cursor.moveToFirst()){
            id = cursor.getString(0)
        }
        db.close()
        return id
    }

    fun getAll(user:String): MutableList<Device> {
        val q = "SELECT * FROM $TABLE_NAME WHERE $ID ='$user'"
        val db = this.writableDatabase
        val cursor = db.rawQuery(q, null)
        var ans = mutableListOf<Device>()
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast) {
                val adr: String = cursor.getString(1)
                val name: String = cursor.getString(2)
                val dev = Device(name, adr, true)
                ans.add(dev)
                cursor.moveToNext()
            }
        }
        db.close()
        return ans
    }
    fun remove(adr:String){
        val db = this.writableDatabase
        db.delete(TABLE_NAME, MAC + "=?", arrayOf<String>(adr))
        db.close()
    }
}

