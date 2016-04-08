package com.nitdgp.arka.psync;
/**
 * @author  : Arka Prava
 * @version : 4 March 2016
 */
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The Discoverer module : Find Peers in communication ranges
 */
public class Discoverer {

    String BROADCAST_IP;
    int PORT;
    final Thread[] thread = new Thread[3];
    final BroadcastThread broadcastThread;
    final ListenThread listenThread;
    final PeerExpiryThread peerExpiryThread;

    Context mContext;
    volatile ConcurrentHashMap<String, Integer> peerList;

    public Discoverer(String BROADCAST_IP, int PORT, Context mContext) {
        this.BROADCAST_IP = BROADCAST_IP;
        this.PORT = PORT;
        this.mContext = mContext;

        peerList = new ConcurrentHashMap<String, Integer>();
        broadcastThread = new BroadcastThread(BROADCAST_IP, PORT);
        listenThread = new ListenThread();
        peerExpiryThread = new PeerExpiryThread();
        thread[0] = new Thread(broadcastThread);
        thread[1] = new Thread(listenThread);
        thread[2] = new Thread(peerExpiryThread);
    }

    public void startBroadcast(){
        if (!broadcastThread.isRunning) {
            thread[0] = new Thread(broadcastThread);
            thread[0].start();
        }
    }

    public void stopBroadcast() {
        if(broadcastThread.isRunning) {
            broadcastThread.stop();
        }
    }

    public void startListener() {
        /*
        Check for any zombie thread waiting for broadcast
        If there is no zombie thread start a new thread to
        listen for broadcast
        else revive zombie thread
        */
        if (!listenThread.isRunning) {
            if (thread[1].isAlive()) {
                listenThread.revive();
            } else {
                thread[1] = new Thread(listenThread);
                thread[1].start();
            }
        }
    }

    public void stopListener() {
        if (!listenThread.exit) {
            listenThread.stop();
        }
    }

    public void startPeerExpiry(){
        if (!peerExpiryThread.isRunning) {
            thread[2] = new Thread(peerExpiryThread);
            thread[2].start();
        }
    }

    public void stopPeerExpiry() {
        if(peerExpiryThread.isRunning) {
            peerExpiryThread.stop();
        }
    }

    public void startDiscoverer(){
        startBroadcast();
        startListener();
        startPeerExpiry();
    }

    public void stopDiscoverer(){
        stopBroadcast();
        stopListener();
        stopPeerExpiry();
    }


    /**
     * Thread to broadcast Datagram packets
     */
    public class BroadcastThread implements Runnable {
        String BROADCAST_IP;
        int PORT;
        DatagramSocket datagramSocket;
        byte buffer[] = null;
        DatagramPacket datagramPacket;
        volatile boolean exit;
        volatile boolean isRunning;

        public BroadcastThread(String BROADCAST_IP, int PORT) {
            this.BROADCAST_IP = BROADCAST_IP;
            this.PORT = PORT;
            this.exit = false;
            this.isRunning = false;
        }

        @Override
        public void run() {
            try {
                datagramSocket = new DatagramSocket();
                datagramSocket.setBroadcast(true);
                buffer = "Msg from server".getBytes("UTF-8");
                this.isRunning = true;
                while(!this.exit) {
                    datagramPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(BROADCAST_IP), PORT);
                    try {
                        datagramSocket.send(datagramPacket);
                        Log.d("DEBUG", "Broadcast Packet Sent");
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        Log.d("DEBUG", "Broadcast Packet Sending Failed");
                    }
                    Thread.sleep(3000);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch(UnknownHostException e){
                e.printStackTrace();
            }catch(IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                datagramSocket.close();
            }
            this.exit = false;
            this.isRunning = false;

            Log.d("DEBUG", "Broadcast Thread Stopped");
        }

        public void stop() {
            this.exit = true;
        }
    }


    /**
     * Thread to listen for broadcasts
     * While closing this thread we may reach a zombie state as the thread may be blocked on receive
     */
    public class ListenThread implements Runnable{
        DatagramPacket datagramPacket;
        byte buffer[];
        DatagramSocket datagramSocket;
        volatile boolean exit;
        volatile boolean isRunning;


        public ListenThread(){
            this.exit = false;
            this.isRunning = false;
        }

        @Override
        public void run() {
            WifiManager wifiManager;
            wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
            WifiManager.MulticastLock multicastLock = wifiManager.createMulticastLock("lock");
            multicastLock.acquire();
            try{
                datagramSocket = new DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"));
                datagramSocket.setBroadcast(true);
                datagramSocket.setSoTimeout(200);

                Log.d("DEBUG", "ListenerThread Start");
                this.isRunning = true;
                while(!this.exit) {
                    buffer = new byte[15000];

                    boolean willUpdatePeer = false;     // will update peer list only if we receive a datagram packet
                    datagramPacket = new DatagramPacket(buffer, buffer.length);
                    try {
                        datagramSocket.receive(datagramPacket);
                        willUpdatePeer = true;          // datagram packet received will update peer list
                    }catch (IOException e) {
                        /*
                        This exception will be caught when we do not receive a datagram packet
                         */
                    }

                    byte[] data = datagramPacket.getData();
                    InputStreamReader inputStreamReader = new InputStreamReader(new ByteArrayInputStream(data), Charset.forName("UTF-8"));

                    final StringBuilder stringBuilder = new StringBuilder();
                    try {
                        for (int value; (value = inputStreamReader.read()) != -1; ) {
                            stringBuilder.append((char) value);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(willUpdatePeer) { // update peer list only when datagram packet is received
                        updatePeers(datagramPacket.getAddress().getHostAddress());
                    }
                } // end of while
            }catch (UnknownHostException e){
            }catch (IOException e){
                e.printStackTrace();
            } finally {
                try {
                    datagramSocket.close();
                }catch (NullPointerException e){

                }
            }
            this.exit = false;
            this.isRunning = false;
            multicastLock.release();
        }

        public void stop() {
            this.exit = true;
        }

        /**
         * Handle zombie state of listening thread
         */
        public void revive() {
            this.exit = false;
        }

        /**
         * Update list of peers
         * @param s the ip address of the current peer
         */
        public void updatePeers(String s) {
            /*
            Put the ip address in the table
            Set its counter to 0
             */
            Log.d("DEBUG", "ListenerThread Receive Broadcaset:" + s);
            peerList.put(s, 0);
        }
    }

    /*
     * Thread to expire peers after T seconds of inactivity
     */
    public class PeerExpiryThread implements Runnable {
        boolean exit = true;
        boolean isRunning = false;

        @Override
        public void run() {
            exit = false;
            isRunning = true;
            while (!exit) {
                for (String s : peerList.keySet()) {
                    if(peerList.get(s) >= 10) {
                        peerList.remove(s);
                        Log.d("DEBUG", "PeerExpiryThread Remove:" + s);
                    } else {
                        peerList.put(s, peerList.get(s) + 1);
                    }
                }
                try {
                    Thread.sleep(1000);
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