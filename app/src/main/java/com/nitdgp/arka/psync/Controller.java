package com.nitdgp.arka.psync;

/**
 * Created by arka on 27/3/16.
 */

import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Controller module : Core module that takes care
 * of the role based and device priority scheduling
 */
public class Controller {

    Discoverer discoverer;
    FileManager fileManager;
    FileTransporter fileTransporter;
    int syncInterval;
    ControllerThread controllerThread = new ControllerThread(this);
    Thread mcontrollerThread = new Thread(controllerThread);
    int maxRunningDownloads;

    ConcurrentHashMap<String, ConcurrentHashMap<String, FileTable>> remotePeerFileTableHashMap;
    ConcurrentHashMap<String, ConcurrentHashMap<String, FileTable>> missingFileTableHashMap;
    /*
    missingFileTableHashMap Format :
    ----------------------------------------------------
    | Peer Address | File ID | File Table for the file |
    ----------------------------------------------------
     */
    ConcurrentHashMap<String, ConcurrentHashMap<String, Thread>> ongoingDownloads;
    /*
    ongoingDownloads Format :
    --------------------------------------------
    | Peer Address | File ID | Download Thread |
    --------------------------------------------
     */

    public Controller(Discoverer discoverer, FileManager fileManager, FileTransporter fileTransporter, int syncInterval, int maxRunningDownloads) {
        this.discoverer = discoverer;
        this.fileManager = fileManager;
        this.syncInterval = syncInterval;
        this.fileTransporter = fileTransporter;
        this.maxRunningDownloads = maxRunningDownloads;
        remotePeerFileTableHashMap = new ConcurrentHashMap<>();
        missingFileTableHashMap = new ConcurrentHashMap<>();
        controllerThread = new ControllerThread(this);
        mcontrollerThread  = new Thread(controllerThread);
        ongoingDownloads = new ConcurrentHashMap<>();
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

    public String urlResolver(String  uri){
        String parameter = uri.substring(1);
        Log.d("DEBUG", "Controller URL Request recv: " + parameter);
        if(parameter.equals("list")){
            return fileManager.DATABASE_PATH;
        }
        else {
            return "";
        }
    }

    /**
     * Collect the remote file info from the available peers
     * Called when ListFetcher thread has received the file list from peer
     * @param peerAddress : the address of the current peer
     * @param remoteFiles : the fileTable of the current peer
     */
    void peerFilesFetched(String peerAddress, ConcurrentHashMap<String, FileTable> remoteFiles) {
        Gson gson = new Gson();
        Log.d("DEBUG: ", "Controller file fetch Response code : " + gson.toJson(remoteFiles).toString());
        remotePeerFileTableHashMap.put(peerAddress, remoteFiles);
    }

    /**
     * Find the missing / incomplete files from peers
     */
    void findMissingFiles() {
        long endByte;
        /*
        Iterate over all peers
         */
        for(String peers : remotePeerFileTableHashMap.keySet()) {
            /*
            Iterate over all files
             */
            for( String files : remotePeerFileTableHashMap.get(peers).keySet()) {
                /*
                Find whether the peer has any file which is missing in device
                 */
                endByte = 0;
                boolean isMissing = false;
                for( String myFiles : fileManager.fileTableHashMap.keySet()) {
                    if(files.equals(myFiles) == true) { // check whether file is same as remote file
                        if(fileManager.fileTableHashMap.get(myFiles).getSequence().get(1) ==
                                fileManager.fileTableHashMap.get(myFiles).getFileSize()) { // complete file available
                            isMissing = false;
                            break;
                        } else if(fileManager.fileTableHashMap.get(myFiles).getSequence().get(1) <
                                remotePeerFileTableHashMap.get(peers).get(files).getSequence().get(1)){
                            isMissing = true;
                            endByte = fileManager.fileTableHashMap.get(myFiles).getSequence().get(1);
                            break;
                        }
                    }else {
                        isMissing = true;
                        endByte = 0;
                    }
                }
                if(isMissing) { // file is missing
                    if(missingFileTableHashMap.get(peers) == null) { // this is first missing file from current peer
                        missingFileTableHashMap.put(peers, new ConcurrentHashMap<String, FileTable>());
                        missingFileTableHashMap.get(peers).put(files, remotePeerFileTableHashMap.get(peers).get(files));
                    }else {                                         // there are one or more missing file with current peer
                        missingFileTableHashMap.get(peers).put(files, remotePeerFileTableHashMap.get(peers).get(files));
                    }
                    List<Long> seq = new ArrayList<Long>();
                    seq.add(0, new Long(0));
                    seq.add(0, endByte);
                    missingFileTableHashMap.get(peers).get(files).setSequence(seq);
                }
            }
        }
    }

    /**
     * Remove the missing files from the peers which have expired
     */
    void removeExpiredMissingFiles() {
        for(String peer : missingFileTableHashMap.keySet()) {
            if( discoverer.peerList.get(peer) == null) { // the peer has expired
                missingFileTableHashMap.remove(peer);
            }
        }
    }

    /**
     * Remove the peer which has expired
     */
    void removeExpiredRemoteFiles() {
        for(String peer : remotePeerFileTableHashMap.keySet()) {
            if( discoverer.peerList.get(peer) == null) { // the peer has expired
                remotePeerFileTableHashMap.remove(peer);
            }
        }
    }


    /*
     * Remove completed downloads from fileTransporter ngoingDownloadThreads list
     */
    void manageOngoingDownloads(){
        for(Thread t : fileTransporter.ongoingDownloadThreads.keySet()){
            FileTransporter.ResumeDownloadThread downloadRunnable = fileTransporter.ongoingDownloadThreads.get(t);
            if(downloadRunnable.isRunning){
                fileManager.setEndSequence(downloadRunnable.fileID, downloadRunnable.getPresentByte());
            }
            else {
                fileManager.setEndSequence(downloadRunnable.fileID, downloadRunnable.getPresentByte());
                fileTransporter.ongoingDownloadThreads.remove(t);
            }
        }
    }

    void startDownloadingMissingFiles(){
        if(fileTransporter.ongoingDownloadThreads.size() < maxRunningDownloads){
            for(String p : missingFileTableHashMap.keySet()){
                if(fileTransporter.ongoingDownloadThreads.size() < maxRunningDownloads){
                    break;
                }
                for(String fileID : missingFileTableHashMap.get(p).keySet()){
                    if(fileTransporter.ongoingDownloadThreads.size() < maxRunningDownloads){
                        break;
                    }
                    boolean ongoing = false;
                    for(Thread t : fileTransporter.ongoingDownloadThreads.keySet()){
                        if(fileTransporter.ongoingDownloadThreads.get(t).fileID.equals(fileID)){
                            ongoing = true;
                            break;
                        }
                    }
                    if(!ongoing){
                        try {
                            fileTransporter.downloadFile(fileID, missingFileTableHashMap.get(p).get(fileID).getFileName(),p, missingFileTableHashMap.get(p).get(fileID).getSequence().get(1), -1);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * Thread to fetch the file list from all the available peers
     * Find the missing files from the peers
     */
    public class ControllerThread implements Runnable {
        boolean exit = true;
        boolean isRunning = false;
        Controller controller;

        public ControllerThread(Controller controller) {
            this.controller = controller;
        }

        @Override
        public void run() {
            exit = false;
            isRunning = true;
            Log.d("DEBUG: ", "Controller thread running : " );
            while (!exit) {

                for(String s : discoverer.peerList.keySet()) {
                    try {
                        new Thread(fileTransporter.new ListFetcher(controller, new URL("http://"+s+":8080/list"), s)).start();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }

                /*
                Remove the peers which have expired and the missing files corresponding to them
                Then find the missing files from the available peers
                 */
                removeExpiredMissingFiles();
                removeExpiredRemoteFiles();
                findMissingFiles();
                Gson gson = new Gson();
                Log.d("DEBUG: ", "Controller thread missing files : " + gson.toJson(missingFileTableHashMap).toString());
                manageOngoingDownloads();
                startDownloadingMissingFiles();

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
}
