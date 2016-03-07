package com.nitdgp.arka.psync;
/**
 * @author  : Arka Prava
 * @version : 4 March 2016
 */
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

/**
 * The Discoverer module
 * Activity connects with other devices through WIFI and packets of data are send
 * The application needs to register a BroadcastReceiver for notification of WiFi state and
 * related events
 */
public class Discoverer extends ActionBarActivity implements WifiP2pManager.ChannelListener, DeviceListFragment.DeviceActionListener {

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
    }

    /**
     * Register the Broadcast Receiver with the intent values to be matched
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_discoverer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_startP2p:
                if(wifiP2pManager != null && wifiP2pChannel != null){
                    /*
                     Since this is the system wireless settings activity, it's
                     not going to send us a result. We will be notified by
                     WiFiDeviceBroadcastReceiver instead.
                    */
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }else {
                    displayToast("Channel or manager is null");
                }
                return true;
            case R.id.menu_register:
                onClickMenuRegister();
                return true;
            case R.id.menu_unregister:
                onClickMenuUnregister();
                return true;
            case R.id.menu_discover:
                onClickMenuDiscover();
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
        if(isWifiDirectReceiverRegisteredAndFeatureEnabled() == false){
            return;
        }
        final DeviceListFragment fragment =
                (DeviceListFragment)getFragmentManager().findFragmentById(R.id.discoverer_frag_list);
        fragment.onInitiateDiscovery();
        wifiP2pManager.discoverPeers(wifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                displayToast("Discovery Initiated");
            }

            @Override
            public void onFailure(int reason) {
                displayToast("Discovery Failed");
            }
        });
    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.discoverer_frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.discoverer_frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }

    public void displayToast(String msg) {
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
            displayToast(wifiDirectReceiver == null ? "Wifi Direct Broadcast Receiver not yet registered " :
                    "Wifi Direct Not Enabled on Phone");
        }
        return isWifiDirectUsable;
    }

    /**
     * Overridem method of DeviceListFragment.DeviceActionListener
     */
    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.discoverer_frag_detail);
        fragment.showDetails(device);
    }

    /**
     * Overridem method of DeviceListFragment.DeviceActionListener
     */
    @Override
    public void cancelDisconnect() {
        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if(wifiP2pManager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.discoverer_frag_list);
            if(fragment.getDevice() == null || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            }else if(fragment.getDevice().status == WifiP2pDevice.AVAILABLE ||
                    fragment.getDevice().status == WifiP2pDevice.INVITED) {
                wifiP2pManager.cancelConnect(wifiP2pChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        displayToast("Aborting connection");
                    }

                    @Override
                    public void onFailure(int reason) {
                        displayToast("Cancel abort request failed. Code :" + reason);
                    }
                });
            }
        }
    }

    /**
     * Overridem method of DeviceListFragment.DeviceActionListener
     */
    @Override
    public void connect(WifiP2pConfig config) {
        wifiP2pManager.connect(wifiP2pChannel,config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WifiDirectReceiver will notify
            }

            @Override
            public void onFailure(int reason) {
                displayToast("Connection failed . Retry");
            }
        });
    }

    /**
     * Overridem method of DeviceListFragment.DeviceActionListener
     */
    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.discoverer_frag_detail);
        fragment.resetViews();
        wifiP2pManager.removeGroup(wifiP2pChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                //Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
            }

        });

    }


    @Override
    public void onChannelDisconnected() {
        displayToast("Wifi Direct Channel Disconnected -- Reinitializing");
        resetData();
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

}