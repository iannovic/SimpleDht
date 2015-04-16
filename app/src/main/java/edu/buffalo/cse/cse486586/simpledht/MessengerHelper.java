package edu.buffalo.cse.cse486586.simpledht;

/**
 * Created by ianno_000 on 2/24/2015.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class MessengerHelper extends SQLiteOpenHelper{

    /*
        Followed tutorial from this URL:
                http://www.vogella.com/tutorials/AndroidSQLite/article.html
     */

    public static final String DATABASE_NAME = "messenger";
    public static final String TABLE_NAME = "messages";
    public static final String TABLE_COLUMN_ID = "key";
    public static final String TABLE_COLUMN_MESSAGE = "value";
    public static final String[] TABLE_PROJECTION = {TABLE_COLUMN_ID,TABLE_COLUMN_MESSAGE};

    public static final int DATABASE_VERSION = 1;
    private static final String MESSENGER_TABLE_CREATE =
                     "CREATE TABLE messages ( "
                        + "key TEXT PRIMARY KEY,"
                        + "value TEXT NOT NULL);";

    MessengerHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(MESSENGER_TABLE_CREATE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

}