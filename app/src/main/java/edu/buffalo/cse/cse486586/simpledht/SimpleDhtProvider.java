package edu.buffalo.cse.cse486586.simpledht;

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    static final String CONTENT_URI = "content://edu.buffalo.cse.cse486586.simpledht.provider";
    static final Uri PROVIDER_URI = Uri.parse(CONTENT_URI);
    static final String LOCAL_KEY = "\"@\"";
    static final String GLOBAL_KEY = "\"*\"";


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int ret = 0;
        Context context = getContext();
        MessengerDAO dao = new MessengerDAO(context);
        String value;

        /***************************************
         * HANDLE AS A SINGLE NODE PROVIDER IF NOT IN NETWORK
         ***************************************/
        boolean isInNetwork = false;
        int nodeCount = 1;
        try {
            SimpleDhtActivity.joinVariableSemaphore.acquire();
            isInNetwork = SimpleDhtActivity.IS_IN_NETWORK;
            nodeCount = SimpleDhtActivity.CURRENT_NODE_COUNT;
            SimpleDhtActivity.joinVariableSemaphore.release();
        } catch (InterruptedException e) {
            Log.e("ERROR","",e);
        }

        if (selection.equals(GLOBAL_KEY) || selection.equals(LOCAL_KEY)){

            Log.i("DELETE","deleting LOCAL table");
            try {
                dao.open();
                ret = dao.deleteMessage(selection,selectionArgs);
            } catch (SQLException e) {
                Log.e(SimpleDhtActivity.TAG,"ERROR DOING SQL STUFF", e);
            } finally {
                // dao.close();
            }

            if (selection.equals(GLOBAL_KEY) && selectionArgs.length == 1 && nodeCount > 1 && isInNetwork) {
                Log.i("DELETE","passing to successor");
                ContentValues values = new ContentValues();
                values.put("key",selectionArgs[0]);
                findPredecessor(ChordPojo.TYPE_DELETE,ChordPojo.TYPE_DELETE,values);    //Using this function to call delete on the next
            }
        } else if (selectionArgs != null && selectionArgs.length >= 1 && selectionArgs[0].equals("ready")) {

            Log.i("DELETE","deleting SINGLE row");
            try {
                dao.open();
                ret = dao.deleteMessage(selection,selectionArgs);
            } catch (SQLException e) {
                Log.e(SimpleDhtActivity.TAG,"ERROR DOING SQL STUFF", e);
            } finally {
                // dao.close();
            }

        } else if (!isInNetwork || nodeCount == 1) {
            Log.i("DELETE","deleting SINGLE row");
            try {
                dao.open();
                ret = dao.deleteMessage(selection,selectionArgs);
            } catch (SQLException e) {
                Log.e(SimpleDhtActivity.TAG,"ERROR DOING SQL STUFF", e);
            } finally {
                // dao.close();
            }
        } else {
            Log.i("DELETE","determining owner of SINGLE key");
            ContentValues values = new ContentValues();
            values.put("key",selection);
            findPredecessor(ChordPojo.FIND_PREDECESSOR,ChordPojo.TYPE_DELETE,values);    //Using this function to call delete on the next

        }
        return ret; // MIGHT HAVE TO CHANGE THIS TO RETURN WHAT DELETE ACTUALLY RETURNS, IF SO THEN HAVE TO MAKE IT RUN JUST LIKE QUERY()...
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.v("insert", values.toString());
        Context context = getContext();
        MessengerDAO dao = new MessengerDAO(context);

        String value;
        boolean isInNetwork = false;
        int numberOfNodes = 1;
        try {
            SimpleDhtActivity.joinVariableSemaphore.acquire();
            isInNetwork = SimpleDhtActivity.IS_IN_NETWORK;
            numberOfNodes = SimpleDhtActivity.CURRENT_NODE_COUNT;
            SimpleDhtActivity.joinVariableSemaphore.release();
        } catch (InterruptedException e) {
            Log.e("ERROR","",e);
        }

        if (((value = (String) values.get("predecessor")) != null
                && !value.equals("")) || !isInNetwork || numberOfNodes == 1) {
            try {
                Log.i(SimpleDhtActivity.TAG, "*******************INSERTING THE VALUE*******************");
                values.remove("predecessor");
                dao.open();
                dao.insertMessage(values);
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                // dao.close();
            }
        } else {
            Log.v("INSERT", "finding predecessor");
            findPredecessor(ChordPojo.FIND_PREDECESSOR, ChordPojo.TYPE_INSERT, values);
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        Log.i(SimpleDhtActivity.TAG, "Creating database for provider...");
        Context context = getContext();
        MessengerDAO dao = new MessengerDAO(context);
        try {
            dao.open();
            dao.recreateDatabase();
            dao.close();
            return true;

        } catch (SQLException e) {

            dao.close();
            return false;

        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Log.i("QUERY","QUERY INVOKED");
        Cursor cursor = null;
        Context context = getContext();
        MessengerDAO dao = new MessengerDAO(context);
        Log.i("QUERY ARGUMENTS",projection + " : " + selection + " : " + selectionArgs + " : " + sortOrder);


        /***************************************
         * HANDLE AS A SINGLE NODE PROVIDER IF NOT IN NETWORK
         ***************************************/
        boolean isInNetwork = false;
        int numberOfNodes = 1;
        try {
            SimpleDhtActivity.joinVariableSemaphore.acquire();
            isInNetwork = SimpleDhtActivity.IS_IN_NETWORK;
            numberOfNodes = SimpleDhtActivity.CURRENT_NODE_COUNT;
            SimpleDhtActivity.joinVariableSemaphore.release();
        } catch (InterruptedException e) {
            Log.e("ERROR","",e);
        }

        if (selection.equals(LOCAL_KEY) || selection.equals(GLOBAL_KEY)) {          //this block works fine, do not touch
             if (selection.equals(GLOBAL_KEY) && selectionArgs == null && isInNetwork && numberOfNodes > 1) {

                Log.i("QUERY","GLOBAL QUERY");
                ChordPojo pojo = new ChordPojo();
                pojo.setDestinationId(SimpleDhtActivity.MY_EMULATOR_PORT);
                pojo.setType(ChordPojo.TYPE_QUERY);
                pojo.setSecond_type(ChordPojo.TYPE_QUERY);
                ContentValues values = new ContentValues();
                values.put("key",selection);
                pojo.setValues(values);
                //pojo.setResults(list);
                sendMessageQueryAsyncTaskStyle(pojo);
                //new QueryAsyncTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,pojo);

                boolean loopForever = true;
                try {
                    Log.i("GLOBAL QUERY","SPINNING until pairList is set.");
                    while (loopForever) {
                        ServerTask.pairListSemaphore.acquire();
                        //Log.i("GLOBAL QUERY","SPINNING until pairList is set.");
                        if (ServerTask.pairList.size() >= 1) {
                            loopForever = false;
                            ServerTask.pairList.remove(0);
                        }
                        ServerTask.pairListSemaphore.release();
                        //Thread.yield();
                    }

                    Log.i("GLOBAL QUERY","creating return MatrixCursor with pairList");
                    MatrixCursor mc = new MatrixCursor(MessengerHelper.TABLE_PROJECTION);
                    ServerTask.pairListSemaphore.acquire();
                    for (int i = 0; i < ServerTask.pairList.size(); i ++) {
                        Pair p = ServerTask.pairList.get(i);
                        mc.addRow(new String[]{p.getKey(),p.getValue()});
                    }
                    ServerTask.pairList.clear();
                    ServerTask.pairListSemaphore.release();

                    cursor = mc;

                }catch (InterruptedException e) {
                    Log.e("LOCK ERROR", "failed to acquire semaphore", e);
                }

            } else {
                Log.i("QUERY","QUERYING local table for all values");
                try {
                    dao.open();
                    cursor = dao.queryMessage(projection, selection, selectionArgs, sortOrder);
                } catch (SQLException e) {
                    Log.e(SimpleDhtActivity.TAG,"ERROR DOING SQL STUFF", e);
                } finally {
                    // dao.close();
                }
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
            }
        } else if (selectionArgs != null && selectionArgs.length > 1 && selectionArgs[0].equals("successor")) {

            //this node is the successor for a single query
            Log.i("QUERY","QUERYING SUCCEEDING NODE FOR KEY");
            try {
                dao.open();
                cursor = dao.queryMessage(projection, selection, selectionArgs, sortOrder);
            } catch (SQLException e) {
                Log.e(SimpleDhtActivity.TAG,"ERROR DOING SQL STUFF", e);
            } finally {
                // dao.close();
            }

            Log.i("QUERY_RESULT","SENDING result back to asking process");
            ContentValues cv = new ContentValues();

            if (cursor != null && cursor.moveToFirst()) {
                String cursorKey = cursor.getString(cursor.getColumnIndex("key"));
                String cursorValue = cursor.getString(cursor.getColumnIndex("value"));
                cv.put("key",cursorKey);
                cv.put("value",cursorValue);
                Log.i("QUERY VALUES",cursorKey + ":" + cursorValue);
            } else {
                Log.e("QUERY_ERROR","failed to retrieve value from cursor");
            }
            cursor.close();

            String port = selectionArgs[1];
            ChordPojo pojo = new ChordPojo();
            pojo.setType(ChordPojo.QUERY_RESULT);
            pojo.setDestinationId(port);
            pojo.setValues(cv);
            this.sendMessageQueryAsyncTaskStyle(pojo);
            //new QueryAsyncTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,pojo);

        } else if (!isInNetwork || isInNetwork && numberOfNodes == 1) {
            //this node is the successor for a single query
            Log.i("QUERY","QUERYING SUCCEEDING NODE FOR KEY IN NON-DHT");
            try {
                dao.open();
                cursor = dao.queryMessage(projection, selection, selectionArgs, sortOrder);
            } catch (SQLException e) {
                Log.e(SimpleDhtActivity.TAG,"ERROR DOING SQL STUFF", e);
            } finally {
                if (cursor != null && cursor.moveToFirst()) {
                    Log.i("CURSOR VALUE",cursor.getString(cursor.getColumnIndex("key")) + ":" + cursor.getString(cursor.getColumnIndex(("value"))));
                } else {
                    Log.e("CURSOR VALUE","FAILED TO GET SINGLE ROW");
                }
                // dao.close();
            }
        } else {
            ContentValues values = new ContentValues();
            values.put("key",selection);
            findPredecessor(ChordPojo.FIND_PREDECESSOR,ChordPojo.TYPE_QUERY,values);

            String value = "";
            boolean loopForever = true;
            String key = selection;
            Log.i("QUERY","SPINNING until value is set in serverTask's queryMap");
            while (loopForever) {
                try {
                    ServerTask.queryMapSemaphore.acquire();
                    value = ServerTask.queryMap.get(key);
                    if (value != null) {
                        loopForever = false;
                    }
                    ServerTask.queryMapSemaphore.release();
                } catch (InterruptedException e) {
                    Log.e("QUERY","FAILED DURING SEMAPHORE ACQUIRE",e);
                }
            }

            Log.i("QUERY","manually creating CURSOR");
            MatrixCursor mc = new MatrixCursor(MessengerHelper.TABLE_PROJECTION);
            mc.addRow(new String[]{key,value});
            cursor = mc;

            Log.i("QUERY","CURSOR CREATED SUCCESSFULLY!! removing key from queryMap");
            try {
                ServerTask.queryMapSemaphore.acquire();
                value = ServerTask.queryMap.remove(key);
                ServerTask.queryMapSemaphore.release();
            } catch (InterruptedException e) {
                Log.e("QUERY","FAILED DURING SEMAPHORE ACQUIRE",e);
            }

        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private void findPredecessor(int type,int secondType, ContentValues values) {

        ChordPojo pojo = new ChordPojo();
        pojo.setType(type);
        pojo.setSecond_type(secondType);
        pojo.setValues(values);
        pojo.setDestinationId(SimpleDhtActivity.MY_EMULATOR_PORT);
        sendMessageQueryAsyncTaskStyle(pojo);
        //new QueryAsyncTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,pojo);   maybe go back to this later?

    }

    protected Void sendMessageQueryAsyncTaskStyle(ChordPojo... params) {
        ChordPojo pojo = params[0];
        try {
            int remotePort;
                    /*
                    this following if block determines the only case in which we connect to a port that isnt the successor of this node.
                     */
            if (pojo.type == ChordPojo.QUERY_RESULT
                    || pojo.type == ChordPojo.SUCCESSOR_PREDECESSOR_UPDATE
                    || pojo.type == ChordPojo.NODE_JOIN_REQUEST) {
                Log.i("NEW SOCKET","QUERY_RESULT || SUCCESSOR_PREDECESSOR_UPDATE || NODE_JOIN_REQUEST");
                remotePort =  2 * Integer.parseInt(pojo.destinationId);
            } else {
                Log.i("NEW SOCKET","SUCCESSOR");
                remotePort = 2 * Integer.parseInt(SimpleDhtActivity.SUCCESSOR_PORT);
            }

            byte[] self = {10,0,2,2};

            Socket socket;
            socket = new Socket(InetAddress.getByAddress(self),remotePort);

            OutputStream stream = socket.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(stream);
            oos.writeObject(pojo);
            oos.flush();

            oos.close();
            stream.close();
            socket.close();
            Log.i(SimpleDhtActivity.TAG, "sent message to successor");
        } catch (Exception e) {
            Log.e(SimpleDhtActivity.TAG,"exception" + e.getMessage(),e);
        }
        return null;
    }

}
