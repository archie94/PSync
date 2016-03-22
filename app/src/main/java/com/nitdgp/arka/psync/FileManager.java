package com.nitdgp.arka.psync;

/**
 * Created by arka on 20/3/16.
 */

import android.os.Environment;

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

/**
 * File Table and File List Format:
 * ---------------------------------------------------------------------------------------------------------------------
 * |  File ID  |  File Name  |  Sequence  |  File Size  | Priority | Timestamp | TTL | Destination | DestReachedStatus |
 * ---------------------------------------------------------------------------------------------------------------------
 */
public class FileManager {


    HashMap<String, FileTable> fileTableHashMap;
    final String DATABASE_NAME = "fileDB.txt";
    final String DATABASE_PATH = Environment.getExternalStorageDirectory() + "/www/" + DATABASE_NAME;

    /**
     * Class to save file description
     */
    class FileTable {
        String fileID;
        String fileName;
        List sequence;
        double fileSize;
        String priority;
        String timestamp;
        String ttl;
        String destination;
        boolean destinationReachedStatus;

        public FileTable(String fileID, String fileName, List sequence, double fileSize, String priority,
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

        String getPriority(){
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
    public void enterFile(String fileID, String fileName, List sequence, double fileSize, String priority,
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
        fileTableHashMap = new HashMap<String, FileTable>();
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
}
