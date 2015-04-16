package edu.buffalo.cse.cse486586.simpledht;

import android.app.Activity;
import android.database.Cursor;
import android.util.Log;
import android.view.View;

/**
 * Created by ianno_000 on 3/30/2015.
 */
public class GDumpClickListener implements View.OnClickListener {

    Activity activity;

    public GDumpClickListener(Activity activity) {
        this.activity = activity;
    }
    @Override
    public void onClick(View v) {
        Log.i("QUERY","starting global query");
        Cursor cursor = activity.getContentResolver().query(SimpleDhtProvider.PROVIDER_URI,MessengerHelper.TABLE_PROJECTION,null,new String[]{SimpleDhtProvider.GLOBAL_KEY},null);
        Log.v("GDUMP","=====================GDUMP======================");
        if (cursor != null && cursor.moveToFirst()) {
            int key = cursor.getColumnIndex("key");
            int value = cursor.getColumnIndex("value");
            Log.v("GDUMP",cursor.getString(key) + ":" + cursor.getString(value));
            while (cursor.moveToNext()) {
                key = cursor.getColumnIndex("key");
                value = cursor.getColumnIndex("value");
                Log.v("GDUMP",cursor.getString(key) + ":" + cursor.getString(value));
            }
        } else {
            Log.e("GDUMP","failed");
        }
        Log.v("GDUMP","================================================");
        cursor.close();
        //activity.getContentResolver().delete(SimpleDhtProvider.PROVIDER_URI,null,new String[]{SimpleDhtProvider.GLOBAL_KEY});
    }
}
