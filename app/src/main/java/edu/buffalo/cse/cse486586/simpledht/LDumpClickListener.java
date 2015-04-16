package edu.buffalo.cse.cse486586.simpledht;


import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import android.view.View;

/**
 * Created by ianno_000 on 3/29/2015.
 */
public class LDumpClickListener implements View.OnClickListener {

    Activity activity;
    public LDumpClickListener(Activity activity) {
        this.activity = activity;
    }
    @Override
    public void onClick(View v) {
        Cursor cursor = activity.getContentResolver().query(SimpleDhtProvider.PROVIDER_URI,null,SimpleDhtProvider.LOCAL_KEY,null,null,null);
        Log.v("LDUMP","=====================LDUMP======================");
        cursor.moveToFirst();
        if (cursor != null && cursor.moveToFirst()) {
            while (!cursor.isLast()) {
                int key = cursor.getColumnIndex("key");
                int value = cursor.getColumnIndex("value");
                Log.v("LDUMP",cursor.getString(key) + ":" + cursor.getString(value));
                cursor.moveToNext();
            }
        } else {
            Log.e("LDUMP","failed");
        }
        Log.v("LDUMP","================================================");
        cursor.close();
    }
}
