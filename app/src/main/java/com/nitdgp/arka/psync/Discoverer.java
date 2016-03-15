package com.nitdgp.arka.psync;
/**
 * @author  : Arka Prava
 * @version : 4 March 2016
 */
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The Discoverer module
 */
public class Discoverer extends ActionBarActivity  {

    Button broadcast;
    Button stopBroadcast;
    Button listen;
    Button stopListen;
    ListView peerListView;

    private static final String SERVER_IP = "192.168.43.255";
    private static int PORT = 4446;
    private static boolean serverRunning = false;
    private static boolean clientRunning = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discoverer);

        /*
        Initialise
         */
        broadcast = (Button)findViewById(R.id.btn_broadcast);
        listen = (Button)findViewById(R.id.btn_start_client);
        stopBroadcast = (Button)findViewById(R.id.btn_stop_server);
        stopListen = (Button)findViewById(R.id.btn_stop_client);
        peerListView = (ListView)findViewById(android.R.id.list);
        /*
         * Check if device is connected via wifi
         */
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        /*
        If device is connected to a hotspot it will only receive the message
        If device is not connected assume it has hosted the hotspot -- it will broadcast the message
         */
        final ServerThread server = new ServerThread() ;
        final ClientThread client = new ClientThread();
        final Thread[] thread = new Thread[2];
        if(networkInfo.isConnected()){
            displayToast("Connected via wifi");
            listen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!clientRunning) {
                        thread[1] = new Thread(client);
                        thread[1].start();
                        clientRunning = true;
                    }
                }
            });
        }else{
            displayToast("Not connected");
            broadcast.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                   if (!serverRunning) {
                       thread[0] = new Thread(server);
                       thread[0].start();
                       serverRunning = true;
                   }
                }
            });
        }
        stopBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(serverRunning) {
                    server.stop();
                    serverRunning = false;
                }
            }
        });
        stopListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(clientRunning) {
                    client.stop();
                    //clientRunning = false;
                }
            }
        });
    }

    /**
     * A server that will write data on the buffer continuously
     */
    public class ServerThread implements Runnable {
        DatagramSocket datagramSocket;
        byte buffer[] = null;
        DatagramPacket datagramPacket;
        volatile boolean exit;

        public ServerThread() {
            exit = false;
        }

        @Override
        public void run() {
            try {
                datagramSocket = new DatagramSocket(PORT);
                datagramSocket.setBroadcast(true);
                buffer = "Msg from server".getBytes("UTF-8");
                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Server : Socket created", Toast.LENGTH_SHORT).show();
                    }
                });

                while(!exit) {
                    datagramPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(SERVER_IP), PORT);
                    datagramSocket.send(datagramPacket);
                    Discoverer.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Discoverer.this, "Server : packet send", Toast.LENGTH_SHORT).show();
                        }
                    });
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
            }
            datagramSocket.close();

            Discoverer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Discoverer.this, "Server stopped", Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void stop() {
            exit = true;
        }
    }

    /**
     * A client to listen for broadcasts
     */
    public class ClientThread implements Runnable{
        DatagramPacket datagramPacket;
        byte buffer[];
        DatagramSocket datagramSocket;
        volatile boolean exit;
        HashMap<String, Integer> peerList;

        public ClientThread(){
            exit = false;
            peerList = new HashMap<String, Integer>();
        }

        @Override
        public void run() {
            WifiManager wifiManager;
            wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            WifiManager.MulticastLock multicastLock = wifiManager.createMulticastLock("lock");
            multicastLock.acquire();
            try{
                datagramSocket = new DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"));
                datagramSocket.setBroadcast(true);

                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Client : socket created", Toast.LENGTH_SHORT).show();
                    }
                });

                while(!exit) {
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
                    Discoverer.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Discoverer.this, "Msg from " + datagramPacket.getAddress().getHostAddress(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    updatePeers(datagramPacket.getAddress().getHostAddress());
                } // end of while

            }catch (UnknownHostException e){

                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Client exception 1", Toast.LENGTH_SHORT).show();
                    }
                });
            }catch (IOException e){

                Discoverer.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Discoverer.this, "Client exception 2", Toast.LENGTH_SHORT).show();
                    }
                });
            } finally {
                datagramSocket.close();
            }

            multicastLock.release();

            Discoverer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Discoverer.this, "Client stopped", Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void stop() {
            exit = true;
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
            peerList.put(s, 0);
            updatePeerListView();
        }

        /**
         * Update the ListView of available peers
         */
        public void updatePeerListView() {
            Discoverer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<String > peer = new ArrayList<String>();
                    for(String s : peerList.keySet()) {
                        peerList.put(s, peerList.get(s)+1);
                        peer.add(s);
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(Discoverer.this, android.R.layout.simple_list_item_1, peer);
                    peerListView.setAdapter(arrayAdapter);
                }
            });
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void displayToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}