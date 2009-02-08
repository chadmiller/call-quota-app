package org.chad.jeejah.callquota;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FreeContactsDb extends SQLiteOpenHelper {
    private static final String TAG = "SeeStats.FreeContactsDB";

    public FreeContactsDb(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        assert version == 1;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table freecontacts (number char(127), number_key char(127) unique);");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // nothing to do.
    }

}
/* vim: set et ai sta : */
