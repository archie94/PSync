package com.nitdgp.arka.psync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class SyncService extends Service {

    private static final String BROADCAST_IP = "192.168.43.255";
    private static final int PORT = 4446;
    private static final int syncInterval = 5;
    private static final int maxRunningDownloads = 5;

    private static String syncDirectory = "/www/sync/";
    private static String databaseDirectory = "/www/database/";
    private static String databaseName = "fileDB.txt";

    private WebServer webServer;
    private Discoverer discoverer;
    private FileManager fileManager;
    private FileTransporter fileTransporter;
    private Controller controller;

    public SyncService() {
    }

    @Override
    public void onCreate() {
        discoverer = new Discoverer(BROADCAST_IP, PORT, this);
        fileManager = new FileManager(databaseName, databaseDirectory, syncDirectory);
        fileTransporter = new FileTransporter(syncDirectory);
        controller = new Controller(discoverer, fileManager, fileTransporter, syncInterval, maxRunningDownloads);
        webServer = new WebServer(8080, controller);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        discoverer.startDiscoverer();
        fileManager.startFileManager();
        controller.startController();
        try {
            webServer.start();
        } catch(IOException ioe) {
            Log.w("Httpd", "The server could not start.");
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public boolean stopService(Intent name) {
        discoverer.stopDiscoverer();
        fileManager.stopFileManager();
        controller.stopController();
        webServer.stop();
        return super.stopService(name);
    }
}
