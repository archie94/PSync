package com.nitdgp.arka.psync;
/**
 * Created by bishakh on 3/26/16.
 */
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.ConcurrentHashMap;


public class Sync extends AppCompatActivity {
    Button broadcast;
    Button stopBroadcast;
    Button listen;
    Button stopListen;
    Button getFile;
    ListView peerListView;


    private static final String BROADCAST_IP = "192.168.43.255";
    private static int PORT = 4446;



    private WebServer webServer;
    Discoverer discoverer = new Discoverer(BROADCAST_IP, PORT, this);
    /* Methods */
    public void displayToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    final Handler activityHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Bundle bundle = msg.getData();
            Toast.makeText(Sync.this, bundle.getString("Test"), Toast.LENGTH_SHORT).show();
        }
    };


    /* CreateActivity */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discoverer);

        /*
        Initialise
         */
        broadcast = (Button)findViewById(R.id.btn_broadcast);
        listen = (Button)findViewById(R.id.btn_listen);
        stopBroadcast = (Button)findViewById(R.id.btn_stop_broadcast);
        stopListen = (Button)findViewById(R.id.btn_stop_listener);
        getFile = (Button)findViewById(R.id.btn_get_file);
        peerListView = (ListView)findViewById(android.R.id.list);

        /*
         * Check if device is connected via wifi
         */
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(networkInfo.isConnected()){
            displayToast("Connected via wifi");
        }else{
            displayToast("Not connected");
        }

//        final UpdatePeerListViewThread updateView = new UpdatePeerListViewThread(this);
        final Discoverer.BroadcastThread broadcastThread = discoverer.new BroadcastThread(BROADCAST_IP, PORT);
//        final ListenThread listenThread = new ListenThread();
        final Thread[] thread = new Thread[3];

//        thread[1] = new Thread(listenThread);

        /**
         * Start broadcasting if device is not already broadcasting
         */
        broadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverer.startDiscoverer();
            }
        });
        stopBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverer.stopDiscoverer();
            }
        });
        /**
         * Start listening for broadcasts if not already listening
         */
/*        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
*/                /*
                Check for any zombie thread waiting for broadcast
                If there is no zombie thread start a new thread to
                listen for broadcast
                else revive zombie thread
                 */
/*                if(!clientRunning ) {
                    if(thread[1].isAlive()){
                        listenThread.revive();
                    }else {
                        thread[1] = new Thread(listenThread);
                        thread[1].start();
                    }
                    thread[2] = new Thread(updateView);
                    thread[2].start();
                    clientRunning = true;
                }
            }
        });
        stopListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clientRunning) {
                    listenThread.stop();
                    updateView.stop();
                }
            }
        });
*/
        getFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    new FileTransporter().downloadFile();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        });
        /*
        Start HTTP server
         */
        webServer = new WebServer(8080);
        try {
            webServer.start();
        } catch(IOException ioe) {
            Log.w("Httpd", "The server could not start.");
        }
        Log.w("Httpd", "Web server initialized.");
    }
}


