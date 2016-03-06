package com.nitdgp.arka.psync;
/**
 * @author  : Arka Prava
 * @version : 4 March 2016
 */
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

/**
 * The Discoverer module
 * Activity connects with other devices through WIFI and packets of data are send
 */
public class Discoverer extends ActionBarActivity implements WifiP2pManager.ChannelListener{

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pChannel;

    private WifiDirectReceiver wifiDirectReceiver;

    private List deviceList;
    ListView peerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discoverer);

        wifiP2pManager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        wifiP2pChannel = wifiP2pManager.initialize(this, getMainLooper(), this);

        registerWifiDirectReceiver();
        if(wifiDirectReceiver.wifiDirectEnabled() == false){
            displayToast("Turn on Wifi Direct");
        }

        if(isWifiDirectReceiverRegisteredAndFeatureEnabled()) {
            populatePeerList();
        }
    }

    private void populatePeerList() {
        deviceList = wifiDirectReceiver.getPeerList();
        if(deviceList.size()>0) {
            ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList);
            peerList.setAdapter(arrayAdapter);
        }else{
            displayToast("No Devices Found");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_discoverer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_register:
                onClickMenuRegister();
                return true;
            case R.id.menu_unregister:
                onClickMenuUnregister();
                return true;
            case R.id.menu_discover:
                onClickMenuDiscover();
                return true;
            case R.id.menu_connect:
                onClickMenuConnect();
                return true;
            default:return super.onOptionsItemSelected(item);
        }
    }

    public void onClickMenuRegister() {
        registerWifiDirectReceiver();
    }

    public void onClickMenuUnregister() {
        unregisterWifiDirectReceiver();
    }

    public void onClickMenuDiscover() {
        wifiP2pManager.discoverPeers(wifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                displayToast("Discover Peers Started");
            }

            @Override
            public void onFailure(int reason) {
                displayToast("Discover Peers Failed");
            }
        });
    }

    public void onClickMenuConnect() {
        if(isWifiDirectReceiverRegisteredAndFeatureEnabled()){
            WifiP2pDevice device = wifiDirectReceiver.getFirstDevice();
            if(device != null) {
                // initiate connection here
            }else{
                displayToast("No Device Currently available");
            }
        }
    }
    /**
     * Register the Broadcast Receiver
     */
    public void registerWifiDirectReceiver() {
        wifiDirectReceiver = new WifiDirectReceiver(wifiP2pManager, wifiP2pChannel, this);
        wifiDirectReceiver.registerReceiver();
    }

    public void unregisterWifiDirectReceiver() {
        if(wifiDirectReceiver != null) {
            wifiDirectReceiver.unregisterReceiver();
        }
        wifiDirectReceiver = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        wifiDirectReceiver = new WifiDirectReceiver(wifiP2pManager, wifiP2pChannel, this);
        wifiDirectReceiver.registerReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(wifiDirectReceiver != null) {
            wifiDirectReceiver.unregisterReceiver();
        }
        wifiDirectReceiver = null;
    }

    @Override
    public void onChannelDisconnected() {
        displayToast("Wifi Direct Channel Disconnected -- Reinitializing");
        reinitializeChannel();
    }

    private void reinitializeChannel() {
        wifiP2pChannel = wifiP2pManager.initialize(this, getMainLooper(), this);
        if(wifiP2pChannel != null){
            displayToast("Wifi Direct Channel Initialization : SUCCESS");
        } else {
            displayToast("Wifi Direct Channel Initialization : FAILED");
        }
    }

    private void displayToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * method tells us whether we have registered the broadcast receiver
     * and enabled wifi direct on device
     * @return
     */
    private boolean isWifiDirectReceiverRegisteredAndFeatureEnabled() {
        boolean isWifiDirectUsable = wifiDirectReceiver!=null && wifiDirectReceiver.wifiDirectEnabled();

        // show a message if not ready to be used
        if(isWifiDirectUsable == false){
            displayToast(wifiDirectReceiver==null?"Wifi Direct Broadcast Receiver not yet registered " :
                                                    "Wifi Direct Not Enabled on Phone");
        }
        return isWifiDirectUsable;
    }
}