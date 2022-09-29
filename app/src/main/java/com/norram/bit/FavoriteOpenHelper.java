package com.norram.bit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FavoriteOpenHelper extends SQLiteOpenHelper {
    static String DBName = "FAVORITE_DB";
    static int VERSION = 1; // Database version (onUpgrade method is run when growing in value)

    public FavoriteOpenHelper(Context context) {
        super(context, DBName, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE FAVORITE_TABLE (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url TEXT, " +
                "name TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS FAVORITE_TABLE");
        onCreate(db);
    }
}
