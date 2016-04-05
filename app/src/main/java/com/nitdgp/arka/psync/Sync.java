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
import java.util.ArrayList;
import java.util.List;


public class Sync extends AppCompatActivity {
    Button startSync;
    Button stopSync;
    Button listen;
    Button stopListen;
    Button getFile;
    ListView peerListView;
    ListView activeDownloadsListView;


    private static final String BROADCAST_IP = "192.168.43.255";
    private static final int PORT = 4446;
    private static final int syncInterval = 5;
    private static final int maxRunningDownloads = 5;

    private static String syncDirectory = "/www/sync/";
    private static String databaseDirectory = "/www/database/";
    private static String databaseName = "fileDB.txt";

    PeerListUIThread peerListUIThread = new PeerListUIThread(this);
    ActiveDownloadsListUIThread activeDownloadsListUIThread = new ActiveDownloadsListUIThread(this);

    private WebServer webServer;
    Discoverer discoverer = new Discoverer(BROADCAST_IP, PORT, this);
    FileManager fileManager = new FileManager(databaseName, databaseDirectory, syncDirectory);
    FileTransporter fileTransporter = new FileTransporter(syncDirectory);
    Controller controller = new Controller(discoverer, fileManager, fileTransporter, syncInterval, maxRunningDownloads);
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
        startSync = (Button)findViewById(R.id.btn_start_sync);
        listen = (Button)findViewById(R.id.btn_listen);
        stopSync = (Button)findViewById(R.id.btn_stop_sync);
        stopListen = (Button)findViewById(R.id.btn_stop_listener);
        getFile = (Button)findViewById(R.id.btn_get_file);
        peerListView = (ListView)findViewById(android.R.id.list);
        activeDownloadsListView = (ListView)findViewById(R.id.listView);

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

        /**
         * Start broadcasting if device is not already broadcasting
         * Start listening for broadcasts if not already listening
         */
        startSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverer.startDiscoverer();
                fileManager.startFileManager();
                controller.startController();
                startPeerListUIThread();
                startActiveDownloadsListUIThread();
            }
        });
        stopSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverer.stopDiscoverer();
                fileManager.stopFileManager();
                controller.stopController();
                stopPeerListUIThread();
                stopActiveDownloadsListUIThread();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
        Start HTTP server
         */
        webServer = new WebServer(8080, controller);
        try {
            webServer.start();
        } catch(IOException ioe) {
            Log.w("Httpd", "The server could not start.");
        }
        Log.w("Httpd", "Web server initialized.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        webServer.stop();
    }

    void startPeerListUIThread() {
        if(!peerListUIThread.isRunning) {
            Thread updatePeerListView = new Thread(peerListUIThread);
            updatePeerListView.start();
        }
    }

    void stopPeerListUIThread(){
        if(peerListUIThread.isRunning) {
            peerListUIThread.stop();
        }
    }

    void startActiveDownloadsListUIThread() {
        if(!activeDownloadsListUIThread.isRunning) {
            Thread updateActiveDownloadsList = new Thread(activeDownloadsListUIThread);
            updateActiveDownloadsList.start();
        }
    }

    void stopActiveDownloadsListUIThread() {
        if(activeDownloadsListUIThread.isRunning) {
            activeDownloadsListUIThread.stop();
        }
    }

    /**
     * A Thread to update the ListView showing list of peers
     */
    public class PeerListUIThread implements Runnable {
        Context context;
        boolean exit;
        boolean isRunning;

        public PeerListUIThread(Context context) {
            this.context = context;
            exit = false;
        }

        @Override
        public void run() {
            exit = false;
            isRunning = true;
            while (!exit) {
                Sync.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final List<String> address = new ArrayList<>();
                        final List<Integer> counter = new ArrayList<>();
                        for (String s : discoverer.peerList.keySet()) {
                            address.add(s);
                            counter.add(discoverer.peerList.get(s));
                        }
                        PeerListView peerListRow = new PeerListView(context, address, counter);
                        peerListView.setAdapter(peerListRow);
                    }
                });
                // Update UI after every 1 second
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            exit = true;
            isRunning = false;
        }

        public void stop() {
            exit = true;
        }
    }

    /**
     * A Thread to update the ListView showing list of ongoing downloads
     */
    public class ActiveDownloadsListUIThread implements Runnable {
        Context context;
        boolean exit;
        boolean isRunning;

        public ActiveDownloadsListUIThread(Context context) {
            this.context = context;
            exit = false;
        }

        @Override
        public void run() {
            exit = false;
            isRunning = true;
            while (!exit) {
                Sync.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final List<String> fileNameList = new ArrayList<String>();
                        final List<Long> downloadedSizeList = new ArrayList<Long>();
                        final List<Long> fileSizeList = new ArrayList<Long>();
                        for(Thread t : fileTransporter.ongoingDownloadThreads.keySet()) {
                            fileNameList.add(fileTransporter.ongoingDownloadThreads.get(t).fileID);
                            downloadedSizeList.add(fileTransporter.ongoingDownloadThreads.get(t).getPresentByte());
                            fileSizeList.add((long)0);
                        }
                        ActiveDownloadsAdapter activeDownloadsAdapter = new ActiveDownloadsAdapter(context,
                                fileNameList, downloadedSizeList, fileSizeList);
                        activeDownloadsListView.setAdapter(activeDownloadsAdapter);
                    }
                });
                // Update UI after every 1 second
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            exit = true;
            isRunning = false;
        }

        public void stop() {
            exit = true;
        }
    }
}