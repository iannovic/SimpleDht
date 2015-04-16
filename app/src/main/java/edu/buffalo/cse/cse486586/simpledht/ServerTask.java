package edu.buffalo.cse.cse486586.simpledht;

import android.app.Activity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Created by ianno_000 on 3/26/2015.
 */
public class ServerTask extends AsyncTask<ServerSocket,String,Void> {

    private Activity activity;
    public static Map<String,String> queryMap;
    public static Semaphore queryMapSemaphore;


    public static LinkedList<Pair> pairList;
    public static Semaphore pairListSemaphore;
    private ServerTaskHelper helper;

    public ServerTask(Activity activity) {

        queryMap = new HashMap<String,String>();
        queryMapSemaphore = new Semaphore(1);

        pairList = new LinkedList<Pair>();
        pairListSemaphore = new Semaphore(1);

        if (SimpleDhtActivity.MY_EMULATOR_PORT.equals("5554")) {
            helper = new ServerTaskHelper();
        }

        this.activity = activity;
    }
    @Override
    protected Void doInBackground(ServerSocket ... sockets) {
        ServerSocket serverSocket = sockets[0];

        while (true) {
            try {
                Socket socket = serverSocket.accept();
                Log.i("INFO","FOUND A NEW CONNECTION!");
                InputStream is = socket.getInputStream();
                while (is.available() == 0) {
                    //spin until something is sent.
                }

                ObjectInputStream ois = new ObjectInputStream(is);
                ChordPojo pojo = (ChordPojo) ois.readObject();

                ois.close();
                is.close();
                socket.close();

                switch(pojo.type) {
                    case ChordPojo.TYPE_INSERT:
                        activity.getContentResolver().insert(SimpleDhtProvider.PROVIDER_URI, pojo.getValues());
                        break;

                    case ChordPojo.TYPE_DELETE:
                        Log.i("DELETE",pojo.getDestinationId() + ":" + SimpleDhtActivity.MY_EMULATOR_PORT);
                        if (!pojo.getDestinationId().equals(SimpleDhtActivity.MY_EMULATOR_PORT)) {          //If the destinationID is equal then we successfully already went around the entire chord
                            Log.i("DELETE", "calling delete");
                            String key = (String) pojo.getValues().get("key");
                            String ready = "";
                            if (pojo.isReadyToInsert()) {
                                ready = "ready";
                            }
                            String[] selectionArgs = {ready};
                            Log.i("DELETE", "LENGTH: " + selectionArgs.length);
                            activity.getContentResolver().delete(SimpleDhtProvider.PROVIDER_URI, key, selectionArgs);

                            if (pojo.getValues().get("key").equals(SimpleDhtProvider.GLOBAL_KEY)) {
                                Log.i("DELETE","passing GLOBAL to SUCCESSOR");//this statement needs to be here instead of in the provider to maintain the pojo destinationID for closure
                                sendPojoToSuccessor(pojo);
                            }
                        } else {
                            Log.i("DELETE", "ENDING GLOBAL DELETE SEQUENCE");
                        }
                        break;

                    case ChordPojo.FIND_PREDECESSOR:
                        try {
                            String keyId = genHash((String)pojo.getValues().get("key"));
                            String nodeId = genHash(SimpleDhtActivity.MY_EMULATOR_PORT);
                            String successorId = genHash(SimpleDhtActivity.SUCCESSOR_PORT);
                            Log.i("CHORD_INFO","myPort" + SimpleDhtActivity.MY_EMULATOR_PORT + "successor:" + SimpleDhtActivity.SUCCESSOR_PORT + "keyId: "+ (String)pojo.getValues().get("key"));
                            Log.i("CHORD_INFO","keyId.compareTo(nodeId): " + keyId.compareTo(nodeId) + " keyId.compareTo(Successor): " + keyId.compareTo(successorId)
                                    + " nodeId.compareTo(Successor): "+ nodeId.compareTo(successorId));
                            if ((keyId.compareTo(nodeId) > 0 && keyId.compareTo(successorId) < 0 && keyId.compareTo(successorId) < 0)
                                || (keyId.compareTo(nodeId) > 0 && keyId.compareTo(successorId) > 0 && nodeId.compareTo(successorId) > 0)
                                || (keyId.compareTo(nodeId) < 0 && keyId.compareTo(successorId) < 0 && nodeId.compareTo(successorId) > 0)) {
                                Log.i("EXPRESSIONS","first: " + (keyId.compareTo(nodeId) > 0 && keyId.compareTo(successorId) < 0) +
                                        " second: " + (keyId.compareTo(nodeId) < 0 && (keyId).compareTo(successorId) < 0) +
                                        " third: " +(keyId.compareTo(nodeId) > 0 && keyId.compareTo(successorId) > 0 && nodeId.compareTo(successorId) > 0));

                                pojo.readyToInsert = true;
                                pojo.setType(pojo.getSecond_type());
                                sendPojoToSuccessor(pojo);
                                Log.i("FIND_PREDECESSOR","passing to successor READY TO INSERT/DELETE/QUERY");
                            } else if (!pojo.isReadyToInsert()) {
                                sendPojoToSuccessor(pojo);
                                Log.i("FIND_PREDECESSOR", "passing to successor");
                            }
                        } catch (NoSuchAlgorithmException e) {
                            Log.e(SimpleDhtActivity.TAG, "exception",e);
                        }
                        break;
                    case ChordPojo.TYPE_QUERY:
                        Log.i("QUERY","query serverTask begin");
                        String key = (String)pojo.getValues().get("key");
                        List<Pair> list = pojo.getResults();
                        if (key.equals(SimpleDhtProvider.GLOBAL_KEY)) {

                            Log.i("QUERY","determined to be GLOBAL query");
                            Cursor cursor = activity.getContentResolver().query(SimpleDhtProvider.PROVIDER_URI,MessengerHelper.TABLE_PROJECTION,key,new String[]{key,""},null);
                            if (cursor != null && cursor.moveToFirst()) {
                                Pair pair = new Pair(cursor.getString(cursor.getColumnIndex("key")),cursor.getString(cursor.getColumnIndex("value")));
                                list.add(pair);
                                while (cursor.moveToNext()) {
                                    pair = new Pair(cursor.getString(cursor.getColumnIndex("key")),cursor.getString(cursor.getColumnIndex("value")));
                                    list.add(pair);
                                }
                            }
                            cursor.close();

                            if (pojo.destinationId.equals(SimpleDhtActivity.MY_EMULATOR_PORT)) {
                                Log.i("QUERY","determined to be returning the GLOBAL query");
                                //MOVE VALUES TO THE STATIC MAP OF ARRAY LISTS
                                try {
                                    pairListSemaphore.acquire();
                                    pairList = (LinkedList<Pair>) pojo.getResults().clone();
                                    pairList.add(0,new Pair("ready","ready"));
                                    pairListSemaphore.release();
                                    Thread.yield();
                                    Log.i("QUERY","SEMAPHORE released. all set. ");
                                } catch (InterruptedException e) {
                                    Log.e("LOCK ERROR","failed to acquire semaphore",e);
                                }

                            } else {
                                sendPojoToSuccessor(pojo);
                            }

                        } else {
                            String successor = "successor";
                            String destinationId = pojo.getDestinationId();
                            String[] selectionArgs = {successor,destinationId};
                            Log.i("QUERY",selectionArgs.toString());
                            activity.getContentResolver().query(SimpleDhtProvider.PROVIDER_URI, MessengerHelper.TABLE_PROJECTION, key, selectionArgs, null);
                        }
                        break;

                    case ChordPojo.QUERY_RESULT:
                        Log.i("QUERY_RESULT","beginning query result case");
                        String pojoKey = (String) pojo.getValues().get("key");
                        String value = (String) pojo.getValues().get("value");
                        Log.i("QUERY_RESULT",pojoKey + ":" + value);
                        try {
                            queryMapSemaphore.acquire();
                            queryMap.put(pojoKey,value);
                            queryMapSemaphore.release();
                            Log.i("QUERY_RESULT","inserted KV pair into queryMap");
                        } catch (InterruptedException e) {
                           Log.e(SimpleDhtActivity.TAG,"SEMAPHORE ISSUE",e);
                        }
                        break;

                    case ChordPojo.NODE_JOIN_REQUEST:
                        Log.i("NODE_JOIN_REQUEST","beginning to handle JOIN request");
                        helper.addNode(pojo.getNodeId());
                        List<ChordPojo> updatedNodeList = helper.getNodeChanges();
                        for (int i = 0; i < updatedNodeList.size(); i ++) {
                            Log.i("NODE_JOIN_REQUEST","SENDING UPDATE TO: " + updatedNodeList.get(i).getDestinationId());
                            new QueryAsyncTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,updatedNodeList.get(i));
                        }
                        break;

                    case ChordPojo.SUCCESSOR_PREDECESSOR_UPDATE:
                        Log.i("SP_UPDATE","beginning to handle SP UPDATE");
                        try {
                            SimpleDhtActivity.joinVariableSemaphore.acquire();
                            SimpleDhtActivity.CURRENT_NODE_COUNT = pojo.getCurrentNodeCount();
                            SimpleDhtActivity.IS_IN_NETWORK = true;
                            SimpleDhtActivity.PREDECESSOR_PORT = pojo.getPredecessor();
                            SimpleDhtActivity.SUCCESSOR_PORT = pojo.getSuccessor();
                            SimpleDhtActivity.joinVariableSemaphore.release();
                            Log.i("SP_UPDATE","MyNodePort:" + SimpleDhtActivity.MY_EMULATOR_PORT + " CurrentNodeCount:" + SimpleDhtActivity.CURRENT_NODE_COUNT
                                   + " Successor:" + SimpleDhtActivity.SUCCESSOR_PORT + " Predecessor:" + SimpleDhtActivity.PREDECESSOR_PORT );
                        } catch (InterruptedException e) {
                            Log.e(SimpleDhtActivity.TAG, "SEMAPHORE ISSUE", e);
                        }

                        break;

                }
            } catch (IOException e) {
                Log.e(SimpleDhtActivity.TAG, "IO Exception while accepting new socket on listening port",e);
            } catch (ClassNotFoundException e) {
                Log.e(SimpleDhtActivity.TAG, "ClassNotFoundException",e);
            }
        }

    }

    private class SuccessorSendAsyncTask extends AsyncTask<ChordPojo,String,Void> {

        @Override
        protected Void doInBackground(ChordPojo... params) {

            return null;
        }
    }

    public void sendPojoToSuccessor(ChordPojo pojo) {
        try {
            byte[] self = {10,0,2,2};
            int remote_port = 2 * Integer.parseInt(SimpleDhtActivity.SUCCESSOR_PORT);
            Socket socket;
            socket = new Socket(InetAddress.getByAddress(self),remote_port);

            OutputStream stream = socket.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(stream);
            oos.writeObject(pojo);
            oos.flush();

            oos.close();
            stream.close();
            socket.close();
            Log.i(SimpleDhtActivity.TAG,"sent message to successor from serverTask");
        } catch (Exception e) {
            Log.e(SimpleDhtActivity.TAG,"exception" + e.getMessage(),e);
        }
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
}
