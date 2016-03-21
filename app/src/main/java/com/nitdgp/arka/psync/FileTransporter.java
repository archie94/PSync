package com.nitdgp.arka.psync;
/**
 * Created by arka on 19/3/16.
 */
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The File Transporter module : request a file from a peer node
 */
public class FileTransporter {

    public void downloadFile() throws MalformedURLException {
        File f = new File(Environment.getExternalStorageDirectory()
                + "/www/out1.mp4");
        ResumeDownloadThread resumeDownloadThread = new ResumeDownloadThread(
                new URL("http://192.168.0.149:8000/1.mp4"), f, 0, 0);
        new Thread(resumeDownloadThread).start();
    }

    class ResumeDownloadThread implements Runnable {
        URL url;
        File outputFile;
        long startByte, endByte;
        final int BUFFER_SIZE = 10240;

        boolean mIsFinished = false;
        boolean DOWNLOADING = true;
        boolean mState = true;

        public ResumeDownloadThread(URL url, File outputFile, long startByte, long endByte){
            this.url = url;
            this.outputFile = outputFile;
            this.startByte = startByte;
            this.endByte = endByte;
        }

        @Override
        public void run() {
            BufferedInputStream in = null;
            RandomAccessFile raf = null;

            try {
                // open Http connection to URL
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                Log.d("DEBUG:FILE TRANSPORTER", "URl is" + url);


                // set the range of byte to download
                String byteRange = startByte + "-" /*+ endByte*/;
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
                    // increase the startByte for resume later
                    startByte += numBytesRead;
                    // increase the downloaded size
                    Log.d("DEBUG:FILE TRANSPORTER", "Fetching  data " + startByte);
                }

                if (mState == DOWNLOADING) {
                    mIsFinished = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("DEBUG:FILE TRANSPORTER", "Connection not established" + e);
            } finally {
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
            }
        }
    }
}
