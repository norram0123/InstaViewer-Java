package com.norram.bit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryOpenHelper extends SQLiteOpenHelper {
    static String DBName = "HISTORY_DB";
    static int VERSION = 1; // Database version (onUpgrade method is run when growing in value)

    public HistoryOpenHelper(Context context) {
        super(context, DBName, null, VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE HISTORY_TABLE (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url TEXT, " +
                "name TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS HISTORY_TABLE");
        onCreate(db);
    }
}
