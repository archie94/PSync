package com.nitdgp.arka.psync;
/**
 * @author  : Arka Prava
 * @version : 4 March 2016
 */
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

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
    Context mContext;

    public Discoverer(String BROADCAST_IP, int PORT, Context mContext) {
        this.BROADCAST_IP = BROADCAST_IP;
        this.PORT = PORT;
        this.mContext = mContext;

        broadcastThread = new BroadcastThread(BROADCAST_IP, PORT);
        listenThread = new ListenThread();
        thread[0] = new Thread(broadcastThread);
        thread[1] = new Thread(listenThread);
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

    public void startDiscoverer(){
        startBroadcast();
        startListener();
    }

    public void stopDiscoverer(){
        stopBroadcast();
        stopListener();
    }


    /**
     * Thread to broadcast Datagram packets
     */
    public class BroadcastThread implements Runnable {
        String BROADCAST_IP = "192.168.43.255";
        int PORT = 4446;
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
                    datagramSocket.send(datagramPacket);
                    /*Discoverer.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Discoverer.this, "Server : packet send", Toast.LENGTH_SHORT).show();
                        }
                    });*/
                    Log.d("DEBUG", "Broadcast Packet Sent");
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
            /*
            Discoverer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Discoverer.this, "Broadcasting stopped", Toast.LENGTH_SHORT).show();
                }
            });
            */
            Log.d("DEBUG", "Broadcasting Stopped");
        }

        public void stop() {

            this.exit = true;
        }
    }


    /**
     * Thread to listen for broadcasts
     */
    public class ListenThread implements Runnable{
        DatagramPacket datagramPacket;
        byte buffer[];
        DatagramSocket datagramSocket;
        volatile boolean exit;
        volatile boolean isRunning;
        public volatile ConcurrentHashMap<String, Integer> peerList;


        public ListenThread(){

            this.exit = false;
            this.isRunning = false;
            this.peerList = new ConcurrentHashMap<String, Integer>();
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

                /*Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Client : socket created", Toast.LENGTH_SHORT).show();
                    }
                });
                */
                Log.d("DEBUG", "ListenerThread Start");
                this.isRunning = true;
                while(!this.exit) {
                    buffer = new byte[15000];
                    datagramPacket = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(datagramPacket);
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
                    updatePeers(datagramPacket.getAddress().getHostAddress());
                } // end of while
            }catch (UnknownHostException e){

                /*Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Client exception 1", Toast.LENGTH_SHORT).show();
                    }
                });
                */
            }catch (IOException e){

                /*Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Client exception 2", Toast.LENGTH_SHORT).show();
                    }
                });
                */
            } finally {
                datagramSocket.close();
            }
            this.exit = false;
            this.isRunning = false;
            multicastLock.release();
            Log.d("DEBUG", "ListenerThread Stop");
            /*Discoverer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Discoverer.this, "Listening stopped", Toast.LENGTH_SHORT).show();
                }
            });
            */
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
            Log.d("DEBUG", "ListenerThread Receive:" + s);
            peerList.put(s, 0);
        }
    }

    /**
     * Thread to update the ListView of available peers
     */
/*    public class UpdatePeerListViewThread implements Runnable {
        boolean exit = false;

        Context context;
        public UpdatePeerListViewThread(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            while (!exit) {
                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        List<String> peer = new ArrayList<String>();
                        List<Integer> counter = new ArrayList<Integer>();
                        for (String s : peerList.keySet()) {
                            if(peerList.get(s) >= 10) {
                                peerList.remove(s);
                            } else {
                                peerList.put(s, peerList.get(s) + 1);
                                peer.add(s);
                                counter.add(peerList.get(s));
                            }
                        }
                        PeerListView peerListRow = new PeerListView(context, peer, counter);
                        peerListView.setAdapter(peerListRow);
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            exit = false;
        }

        public void stop() {
            exit = true;
        }
    }

    public void displayToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
*/
}