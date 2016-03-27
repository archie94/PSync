package com.nitdgp.arka.psync;

/**
 * Created by arka on 20/3/16.
 */

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    ConcurrentHashMap<String, FileTable> fileTableHashMap = new ConcurrentHashMap<String, FileTable>();
    final String DATABASE_NAME = "fileDB.txt";
    final String DATABASE_PATH = Environment.getExternalStorageDirectory() + "/www/database/" + DATABASE_NAME;
    final File FILES_PATH = new File(Environment.getExternalStorageDirectory() + "/www/sync/");

    /**
     * Class to save file description
     */
    private class FileTable implements java.io.Serializable{
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
            this.destination = destination;
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
        Log.d("DEBUG", "FileManager Add to DB: " + fileName);
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
    public void readDB() {
        Log.d("DEBUG", "FileManager reading from fileDB");
        List <FileTable> fileList = null;
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
                Log.d("DEBUG", "FileManaager Add to DB from fileDB: " + fileList.get(i).getFileName());
            }
        }
    }

    /**
     * Traverse the folder and add / remove files
     */
    public void updateFromFolder(){
        // get all files in sync folder
        ArrayList<File> files = findFiles(FILES_PATH);
        //Log.d("DEBUG", "FileManaager Files in sync: " + files.toString());

        // Add file to database if not already present
        for(File file: files){
            String fileID = getFileIDFromPath(file);
            if(fileTableHashMap.get(fileID) == null){
                long fileSize = file.length();
                List seq = new ArrayList();
                seq.add(0, 0);
                seq.add(1, fileSize);
                String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                String ttl = "NONE";
                String destination = "DB";
                enterFile(fileID, file.getName(), seq, fileSize, 1, timeStamp, ttl, destination, false);
            }

        }

        // add , remove or update database
    }

    /**
     * Returns the list of files available in sync directory
     * @param files_path    : the sync directory
     * @return              : the list of files in sync directory
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

    private String getFileIDFromPath(File file){
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] md5sum = md.digest(file.getName().getBytes());
        // Create Hex String
        StringBuffer hexString = new StringBuffer();

        for (int i=0; i<md5sum.length; i++) {
            hexString.append(Integer.toHexString(0xFF & md5sum[i]));
        }

        return hexString.toString();


    }


}
