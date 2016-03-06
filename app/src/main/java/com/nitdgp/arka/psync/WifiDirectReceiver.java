package com.nitdgp.arka.psync;

/**
 * Created by arka on 6/3/16.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import java.util.ArrayList;
import java.util.List;

/**
 * A BroadcastReceiver class to listen to the change in System's WiFi P2P state
 */
class WifiDirectReceiver extends BroadcastReceiver implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener{

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pChannel;
    private Discoverer discoverer;

    private boolean isWifiDirectEnabled = false;
    private IntentFilter intentFilter = null;

    private List peers = new ArrayList();
    private WifiP2pDevice[] wifiP2pDevices;

    public WifiDirectReceiver(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel wifiP2pChannel, Discoverer discoverer) {
        super();
        this.discoverer = discoverer;
        this.wifiP2pManager = wifiP2pManager;
        this.wifiP2pChannel = wifiP2pChannel;
    }

    public void registerReceiver() {
        discoverer.registerReceiver(this, getIntentFilter());
    }

    public void unregisterReceiver() {
        discoverer.unregisterReceiver(this);
    }

    public boolean wifiDirectEnabled() {
        return isWifiDirectEnabled;
    }

    /**
     * Handle each P2P state change
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(intent.getAction())){
            /*
                Determine if WIFI P2P mode is enabled or not
                Alert the activity
             */
            handleP2pStateChanged(intent);
        } else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(intent.getAction())) {
            /*
                The peer list has changed
             */
            handleP2pPeersChanged(intent);
        }else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(intent.getAction())){
            /*
                Connection state changed
             */
            handleP2pConnectionChanged(intent);
        }else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(intent.getAction())) {
            handleP2pThisDeviceChanged(intent);
        }
    }

    private void handleP2pStateChanged(Intent intent) {
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            // Wifi Direct is enabled on device
            isWifiDirectEnabled = true;
        } else {
            // Wifi Direct is disabled on device
            isWifiDirectEnabled = false;
        }
    }

    private void handleP2pPeersChanged(Intent intent) {
        /*
        Request the actual list of peers
         */
        wifiP2pManager.requestPeers(wifiP2pChannel, this);
    }

    private void handleP2pConnectionChanged(Intent intent) {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        if(networkInfo != null && networkInfo.isConnected()) {
            wifiP2pManager.requestConnectionInfo(wifiP2pChannel, this);
        }else {
            // display a toast that network is lost
        }
    }

    private void handleP2pThisDeviceChanged(Intent intent) {
        WifiP2pDevice thisDevice = intent.getParcelableExtra(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        // Log state of device here
    }

    private IntentFilter getIntentFilter() {
        if(intentFilter == null){
            intentFilter = new IntentFilter();
            // Indicates a change in WiFi P2P status
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            // Indicate a change in the list of available peers
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            // Indicates the state of WiFi P2P connectivity has changed
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            // Indicate that devices details have changed
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        }
        return intentFilter;
    }

    public WifiP2pDevice getFirstDevice(){
        if(wifiP2pDevices != null) {
            return wifiP2pDevices[0];
        }else{
            return null;
        }
    }
    public List getPeerList(){
        return peers;
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peersList) {
        if(peersList!=null && peersList.getDeviceList()!=null && peersList.getDeviceList().size()>0) {
            wifiP2pDevices = new WifiP2pDevice[peersList.getDeviceList().size()];
            wifiP2pDevices = peersList.getDeviceList().toArray(wifiP2pDevices);
        }
        else{
            wifiP2pDevices = null;
        }
        // Out with the old, in with the new
        peers.clear();
        peers.addAll(peersList.getDeviceList());

        /*
            Notify the adapter view for the change
         */

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if(info.groupFormed){
            if(info.isGroupOwner){
                // open a server socket
            } else{
                // open a client socket to info.groupowner address
            }

        }
    }
}