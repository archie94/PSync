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
import android.net.wifi.p2p.WifiP2pManager;

/**
 * A BroadcastReceiver class to listen to the change in System's WiFi P2P state
 */
class WifiDirectReceiver extends BroadcastReceiver {

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pChannel;
    private Discoverer discoverer;

    private boolean isWifiDirectEnabled = false;
    private IntentFilter intentFilter = null;


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
            discoverer.resetData();
        }
    }

    private void handleP2pPeersChanged(Intent intent) {
        /*
        Request the actual list of peers from wifi p2p manager
         */
        if(wifiP2pManager != null ) {
            wifiP2pManager.requestPeers(wifiP2pChannel,
                    (WifiP2pManager.PeerListListener) discoverer.getFragmentManager().findFragmentById(R.id.discoverer_frag_list));
        }
    }

    private void handleP2pConnectionChanged(Intent intent) {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        if(networkInfo != null && networkInfo.isConnected()) {
            /*
             we are connected with the other device, request connection
             info to find group owner IP
            */
            DeviceDetailFragment fragment = (DeviceDetailFragment) discoverer
                    .getFragmentManager().findFragmentById(R.id.discoverer_frag_detail);
            wifiP2pManager.requestConnectionInfo(wifiP2pChannel, fragment);
        }else {
            // display a toast that network is lost
            discoverer.displayToast("Network lost");
            discoverer.resetData();
        }
    }

    private void handleP2pThisDeviceChanged(Intent intent) {
        DeviceListFragment fragment =
                (DeviceListFragment)discoverer.getFragmentManager().findFragmentById(R.id.discoverer_frag_list);
        fragment.updateThisDevice((WifiP2pDevice)intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
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
}