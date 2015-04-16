package edu.buffalo.cse.cse486586.simpledht;

import android.os.AsyncTask;
import android.util.Log;

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by ianno_000 on 3/30/2015.
 */
class QueryAsyncTask extends AsyncTask<ChordPojo,String,Void> {

    @Override
    protected Void doInBackground(ChordPojo... params) {
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
