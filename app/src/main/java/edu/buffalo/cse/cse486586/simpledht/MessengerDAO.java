package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.sql.SQLException;

/**
 * Created by ianno_000 on 2/24/2015.
 */

public class MessengerDAO {

    /*
        based off of the DAO from http://www.vogella.com/tutorials/AndroidSQLite/article.html
     */
    private SQLiteDatabase db;
    private MessengerHelper helper;

    private String[] columns = {MessengerHelper.TABLE_COLUMN_ID, MessengerHelper.TABLE_COLUMN_MESSAGE};

    public MessengerDAO(Context context) {
        helper = new MessengerHelper(context);
    }

    public void open() throws SQLException {
        db = helper.getWritableDatabase();
    }
    public void close() {
        helper.close();
    }

    public Long insertMessage(ContentValues values) {
        // ContentValues row = new ContentValues();
       // row.put(MessengerHelper.TABLE_COLUMN_MESSAGE, message);
        Long newId = db.replace(MessengerHelper.TABLE_NAME, "", values);
        //Long newId = db.insert(MessengerHelper.TABLE_NAME, "", values);
        if (newId > -1) {
            String delimiter = MessengerHelper.TABLE_COLUMN_ID + " = ?";
            Cursor cursor = db.query(MessengerHelper.TABLE_NAME, columns, delimiter, new String[]{(String)values.get("key")}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
               Log.v("CSE486",cursor.getString(cursor.getColumnIndex("key")) + ":" + cursor.getString(cursor.getColumnIndex("key")));
            } else {
                Log.e("CSE486","failed to insert");
            }
            cursor.close();
        }
        else
        {
            Log.e("CSE486","failed to insert message");
        }
        return newId;
    }

    public boolean recreateDatabase() {
        helper.onUpgrade(db,MessengerHelper.DATABASE_VERSION,MessengerHelper.DATABASE_VERSION);
        return true;
    }
    public Cursor queryMessage(String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        Cursor cursor = null;

        if (selection.equals(SimpleDhtProvider.LOCAL_KEY) || selection.equals(SimpleDhtProvider.GLOBAL_KEY)) {
            Log.i("DAO_QUERY","ALL");
           cursor =  db.query(MessengerHelper.TABLE_NAME,MessengerHelper.TABLE_PROJECTION,null,null,null,null,null);
        } else {
            Log.i("DAO_QUERY","SINGLE: " + selection);
            String selectClause = "key = ?";
            cursor = db.query(MessengerHelper.TABLE_NAME,MessengerHelper.TABLE_PROJECTION,selectClause,new String[]{selection},null,null,null);
        }

        return cursor;
    }

    public int deleteMessage(String selection, String[] selectionArgs) {

        int ret = 0;

        if (selection.equals(SimpleDhtProvider.LOCAL_KEY) || selection.equals(SimpleDhtProvider.GLOBAL_KEY)) {
            Log.i("DAO_DELETE","ALL");
           ret = db.delete(MessengerHelper.TABLE_NAME,null,null);
        } else {
            Log.i("DAO_DELETE",selection);
            String selection2 = "key = ?";
            ret = db.delete(MessengerHelper.TABLE_NAME,selection2,new String[]{selection});
        }

        return ret;
    }
}
