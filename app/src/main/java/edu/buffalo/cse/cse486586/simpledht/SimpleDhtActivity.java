package edu.buffalo.cse.cse486586.simpledht;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class SimpleDhtActivity extends Activity {

    static final String TAG = SimpleDhtActivity.class.getSimpleName();

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final String EMULATOR_PORT0 = "5554";
    static final String EMULATOR_PORT1 = "5556";
    static final String EMULATOR_PORT2 = "5558";
    static final String EMULATOR_PORT3 = "5560";
    static final String EMULATOR_PORT4 = "5562";
    static final String[] EMULATOR_PORTS_ARRAY = {EMULATOR_PORT0,EMULATOR_PORT1,EMULATOR_PORT2,EMULATOR_PORT3,EMULATOR_PORT4};
    static final String[] REMOTE_PORTS_ARRAY = {REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4};
    static final int SERVER_PORT = 10000;

    static int MAX_NUMBER_OF_PROCESSES = 5;
    static String PREDECESSOR_PORT;
    static String SUCCESSOR_PORT;
    static String MY_REMOTE_PORT;
    static String MY_EMULATOR_PORT;

    /******************************************
        JOIN HANDLING VARIABLES
     *******************************************/
    static Semaphore joinVariableSemaphore;
    static boolean IS_5554 = false;
    static int CURRENT_NODE_COUNT = 1;
    static boolean IS_IN_NETWORK = false;
    /********************************************
        END OF JOIN VARIABLES
     *******************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_simple_dht_main);
        
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver()));

        joinVariableSemaphore = new Semaphore(1);
        /****************************************
            ADD LISTENER TO LDUMP
         *****************************************/
        findViewById(R.id.button1).setOnClickListener(new LDumpClickListener(this));

        /****************************************
         ADD LISTENER TO GDUMP
         *****************************************/
        findViewById(R.id.button2).setOnClickListener(new GDumpClickListener(this));

        /************************************
         SET UP SERVER SOCKET
         ************************************/
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        MY_EMULATOR_PORT = String.valueOf((Integer.parseInt(portStr)));
        MY_REMOTE_PORT = myPort;

        /*****************************************
         * If its 5554, it handles all node join requests
         *****************************************/
        if (MY_EMULATOR_PORT.equals("5554")) {
            try {
                joinVariableSemaphore.acquire();
                IS_5554 = true;
                IS_IN_NETWORK = true;
                joinVariableSemaphore.release();
            } catch (InterruptedException e) {
                Log.e("ERROR","error",e);
            }

        } else {
            ChordPojo pojo = new ChordPojo();
            pojo.setDestinationId("5554");
            pojo.setNodeId(MY_EMULATOR_PORT);
            pojo.setType(ChordPojo.NODE_JOIN_REQUEST);
            new QueryAsyncTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,pojo);
            //REQUEST TO JOIN THE NODE NETWORK
        }
        Log.i(this.TAG,"emulator port is :" + MY_EMULATOR_PORT + " remote port is : " + MY_REMOTE_PORT);

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            Log.e(TAG,e.getMessage());
            return;
        }
        /************************************
           END OF SERVER SOCKET SETUP
         ************************************/

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }
}
