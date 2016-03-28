package com.nitdgp.arka.psync;

/**
 * Created by arka on 27/3/16.
 */

/**
 * The Controller module : Core module that takes care
 of the role based and device priority scheduling
 */
public class Controller {

    Communicator communicator;
    Discoverer discoverer;
    FileManager fileManager;
    boolean syncComplete;

    public Controller(Communicator communicator, Discoverer discoverer, FileManager fileManager) {
        this.communicator = communicator;
        this.discoverer = discoverer;
        this.fileManager = fileManager;
        this.syncComplete = false;
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
