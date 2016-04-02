package com.nitdgp.arka.psync;
/**
 * Created by arka on 19/3/16.
 */
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The File Transporter module : request a file from a peer node
 */
public class FileTransporter {

    public ConcurrentHashMap<Thread, ResumeDownloadThread> ongoingDownloadThreads = new ConcurrentHashMap<Thread, ResumeDownloadThread>();

    String syncDirectory;

    public FileTransporter(String syncDirectory){
        this.syncDirectory = syncDirectory;
    }


    public void downloadFile(String fileID, String fileName, String peerIP, long startByte, long endByte) throws MalformedURLException {
        File f = new File(Environment.getExternalStorageDirectory() + syncDirectory + "/" + fileName);
        URL fileUrl = new URL("http://"+ peerIP +":8080/getFile/" + fileID);
        ResumeDownloadThread resumeDownloadThread = new ResumeDownloadThread(fileUrl , fileID, f, startByte, endByte);
        Thread t = new Thread(resumeDownloadThread);
        ongoingDownloadThreads.put(t, resumeDownloadThread);
        Log.d("DEBUG:", "MISSING FILES ONGOING" + ongoingDownloadThreads.keySet());
        t.start();
    }


    class ResumeDownloadThread implements Runnable {
        URL url;
        File outputFile;
        long startByte, endByte;
        final int BUFFER_SIZE = 10240;

        String fileID;
        boolean mIsFinished = false;
        boolean DOWNLOADING = true;
        boolean mState = true;
        long presentByte;
        public boolean isRunning = false;

        public ResumeDownloadThread(URL url, String fileID, File outputFile, long startByte, long endByte){
            this.url = url;
            this.outputFile = outputFile;
            this.startByte = startByte;
            this.endByte = endByte;
            this.isRunning = false;
            this.presentByte = startByte;
            this.fileID = fileID;

        }

        @Override
        public void run() {
            this.isRunning = true;
            BufferedInputStream in = null;
            RandomAccessFile raf = null;

            try {
                String byteRange;
                isRunning = true;
                // open Http connection to URL
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                Log.d("DEBUG:FILE TRANSPORTER", "URl is" + url);


                // set the range of byte to download
                if(endByte < 0) {
                    byteRange = startByte + "-" /*+ endByte*/;
                }
                else {
                    if(endByte < startByte){
                        throw new IllegalArgumentException();
                    }
                    byteRange = startByte + "-" + endByte;
                }
                //conn.setRequestProperty("Range", "bytes=" + byteRange);
                connection.setRequestMethod("GET");
                connection.setConnectTimeout( 5*1000);
                connection.setReadTimeout(5*1000);

                Log.d("DEBUG:FILE TRANSPORTER", "Connection created" + byteRange);

                // connect to server
                connection.connect();
                Log.d("DEBUG:FILE TRANSPORTER", "Callled connect with timeout " + connection.getConnectTimeout());
                Log.d("DEBUG:FILE TRANSPORTER", ""+connection.getResponseCode());

                // Make sure the response code is in the 200 range.
                if (connection.getResponseCode() / 100 != 2) {
                    Log.d("DEBUG:FILE TRANSPORTER", "error : Response code out of 200 range");
                }

                Log.d("DEBUG:FILE TRANSPORTER", "Response code : " + connection.getResponseCode());
                // get the input stream
                in = new BufferedInputStream(connection.getInputStream());

                // open the output file and seek to the start location
                raf = new RandomAccessFile(outputFile, "rw");
                raf.seek(startByte);

                byte data[] = new byte[BUFFER_SIZE];
                int numBytesRead;
                while(/*(mState == DOWNLOADING) &&*/ ((numBytesRead = in.read(data,0,BUFFER_SIZE)) != -1))
                {
                    // write to buffer
                    raf.write(data,0,numBytesRead);
                    this.presentByte = this.presentByte + numBytesRead;
                    // increase the startByte for resume later
                    startByte += numBytesRead;
                    // increase the downloaded size
                    //Log.d("DEBUG:FILE TRANSPORTER", "Fetching  data " + startByte);
                }

                if (mState == DOWNLOADING) {
                    mIsFinished = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("DEBUG:FILE TRANSPORTER", "Connection not established" + e);
            } finally {
                isRunning = false;
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {}
                }

                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {}
                }
                this.isRunning = false;
            }
        }

        public long getPresentByte(){
            return this.presentByte;
        }
    }

    /**
     * Thread to fetch the list of files from a peer
     */
    class ListFetcher implements Runnable {
        URL url;
        String peerAddress;
        final int BUFFER_SIZE = 10240;
        Controller controller;

        boolean mIsFinished = false;
        boolean DOWNLOADING = true;
        boolean mState = true;

        public ListFetcher(Controller controller, URL url, String peerAddress){
            this.url = url;
            this.peerAddress = peerAddress;
            this.controller = controller;
        }

        @Override
        public void run() {
            BufferedInputStream in = null;

            try {
                // open Http connection to URL
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                Log.d("DEBUG:FILE TRANSPORTER", "URl is" + url);


                // set the range of byte to download

                String byteRange = "0-";

                //conn.setRequestProperty("Range", "bytes=" + byteRange);
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5 * 1000);
                connection.setReadTimeout(5 * 1000);

                Log.d("DEBUG:FILE TRANSPORTER", "Connection created" + byteRange);

                // connect to server
                connection.connect();
                Log.d("DEBUG:FILE TRANSPORTER", "Callled connect with timeout " + connection.getConnectTimeout());
                Log.d("DEBUG:FILE TRANSPORTER", "" + connection.getResponseCode());

                // Make sure the response code is in the 200 range.
                if (connection.getResponseCode() / 100 != 2) {
                    Log.d("DEBUG:FILE TRANSPORTER", "error : Response code out of 200 range");
                }

                Log.d("DEBUG:FILE TRANSPORTER", "Response code : " + connection.getResponseCode());
                // get the input stream
                in = new BufferedInputStream(connection.getInputStream());
                ObjectInputStream objectInputStream = new ObjectInputStream(in);

                ConcurrentHashMap fileTableHashMap;
                fileTableHashMap = (ConcurrentHashMap<String, FileTable>) objectInputStream.readObject();
                controller.peerFilesFetched(peerAddress, fileTableHashMap);
                //Gson gson = new Gson();
                //Log.d("DEBUG:FILE TRANSPORTER", "List Json: " + gson.toJson(fileTableHashMap).toString());


            } catch (Exception e) {
                e.printStackTrace();
                Log.d("DEBUG:FILE TRANSPORTER", "Connection not established" + e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {}
                }
            }
        }
    }
}
