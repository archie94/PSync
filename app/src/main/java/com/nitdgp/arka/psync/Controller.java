package com.nitdgp.arka.psync;

/**
 * Created by arka on 27/3/16.
 */

import android.util.Log;

import java.io.File;

/**
 * The Controller module : Core module that takes care
 of the role based and device priority scheduling
 */
public class Controller {

    Discoverer discoverer;
    FileManager fileManager;
    FileTransporter fileTransporter;
    int syncInterval;
    ControllerThread controllerThread = new ControllerThread();
    Thread mcontrollerThread = new Thread(controllerThread);

    public Controller(Discoverer discoverer, FileManager fileManager, FileTransporter fileTransporter, int syncInterval) {
        this.discoverer = discoverer;
        this.fileManager = fileManager;
        this.syncInterval = syncInterval;
        this.fileTransporter = fileTransporter;
    }


    public void startController(){
        if(!controllerThread.isRunning){
            mcontrollerThread = new Thread(controllerThread);
            mcontrollerThread.start();
        }
    }

    public void stopController(){
        if(controllerThread.isRunning){
            controllerThread.stop();
        }
    }


    public class ControllerThread implements Runnable {
        boolean exit = true;
        boolean isRunning = false;

        @Override
        public void run() {
            exit = false;
            isRunning = true;
            while (!exit) {

                try {
                    Thread.sleep(syncInterval * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            exit = false;
            isRunning = false;
        }

        public void stop() {
            exit = true;
        }
    }

    /**
     * calls sendFileList of all peers
     */
    void requestRemoteFiles() {
        for(String peerAddress : discoverer.peerList.keySet()) {
            sendFileList(peerAddress);
        }
    }

    void sendFileList(String peerAddress) {

    }

}
