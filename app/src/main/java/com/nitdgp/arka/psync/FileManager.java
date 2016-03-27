package com.nitdgp.arka.psync;

/**
 * Created by arka on 20/3/16.
 */

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File Table and File List Format:
 * ---------------------------------------------------------------------------------------------------------------------
 * |  File ID  |  File Name  |  Sequence  |  File Size  | Priority | Timestamp | TTL | Destination | DestReachedStatus |
 * ---------------------------------------------------------------------------------------------------------------------
 */
public class FileManager {

    ConcurrentHashMap<String, FileTable> fileTableHashMap;
    final String DATABASE_NAME = "fileDB.txt";
    final String DATABASE_PATH = Environment.getExternalStorageDirectory() + "/www/Database/" + DATABASE_NAME;
    final File FILES_PATH = new File(Environment.getExternalStorageDirectory() + "/www/sync/");

    /**
     * Class to save file description
     */
    class FileTable {
        private String fileID;
        private String fileName;
        private List sequence;
        private double fileSize;
        private int priority;
        private String timestamp;
        private String ttl;
        private String destination;
        private boolean destinationReachedStatus;

        public FileTable(String fileID, String fileName, List sequence, double fileSize, int priority,
                         String timestamp, String ttl, String destination, boolean destinationReachedStatus){
            this.fileID = fileID;
            this.fileName = fileName;
            this.sequence = sequence;
            this.fileSize = fileSize;
            this.priority = priority;
            this.timestamp = timestamp;
            this.ttl = ttl;
            this.destination =destination;
            this.destinationReachedStatus = destinationReachedStatus;
        }

        String getFileID(){
            return this.fileID;
        }

        String getFileName(){
            return this.fileName;
        }

        double getFileSize(){
            return this.fileSize;
        }

        int getPriority(){
            return this.priority;
        }

        String getTimestamp(){
            return this.timestamp;
        }

        String getTtl(){
            return this.ttl;
        }

        String getDestination(){
            return this.destination;
        }

        boolean getDestinationReachedStatus(){
            return this.destinationReachedStatus;
        }

        void setTtl(String ttl) {
            this.ttl = ttl;
        }

        void setSequence(List sequence){
            this.sequence = sequence;
        }

        void setDestinationReachedStatus(boolean status){
            this.destinationReachedStatus = status;
        }
    }

    /**
     * Store file description
     * @param fileID
     * @param fileName
     * @param sequence
     * @param fileSize
     * @param priority
     * @param timestamp
     * @param ttl
     * @param destination
     * @param destinationReachedStatus
     */
    public void enterFile(String fileID, String fileName, List sequence, double fileSize, int priority,
                          String timestamp, String ttl, String destination, boolean destinationReachedStatus){
        FileTable newFileInfo = new FileTable( fileID, fileName, sequence, fileSize, priority, timestamp,
                ttl, destination, destinationReachedStatus);
        fileTableHashMap.put( fileID, newFileInfo);
        writeDB();
    }

    /**
     * Serialize data
     */
    private void writeDB() {
        try{
            List <FileTable> fileList = new ArrayList<FileTable>();
            for(String s : fileTableHashMap.keySet()) {
                fileList.add(fileTableHashMap.get(s));
            }
            FileOutputStream fileOutputStream = new FileOutputStream(DATABASE_PATH);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(fileList);
            objectOutputStream.close();
            fileOutputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deserialize data
     */
    private void readDB() {
        List <FileTable> fileList = null;
        fileTableHashMap = new ConcurrentHashMap<String, FileTable>();
        try{
            FileInputStream fileInputStream = new FileInputStream(DATABASE_PATH);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            fileList = (List<FileTable>) objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if(fileList != null) {
            for (int i = 0; i < fileList.size(); i++) {
                fileTableHashMap.put(fileList.get(i).getFileID(), fileList.get(i));
            }
        }
    }

    /**
     * Traverse the folder and add / remove files
     */
    private void updateFromFolder(){
        ArrayList<File> files = findFiles(FILES_PATH);      // get all files
        // add , remove or update database
    }

    /**
     * Returns the list of files available in working directory
     * @param files_path    : the working directory
     * @return              : the list of files in working directory
     */
    private ArrayList<File> findFiles(File files_path) {
        ArrayList<File> files = new ArrayList<File>();
        File[] allFile = files_path.listFiles();
        for(File file : allFile) {
            // ignore if it is a directory
            if(!file.isDirectory()) {
                files.add(file);
            }
        }
        return files;
    }


}
