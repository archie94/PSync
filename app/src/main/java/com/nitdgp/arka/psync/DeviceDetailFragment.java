package com.nitdgp.arka.psync;
/**
 * Created by arka on 7/3/16.
 */
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class DeviceDetailFragment extends Fragment implements WifiP2pManager.ConnectionInfoListener {

    private static final String SERVER_IP = "192.168.49.1";
    private static int PORT = 8988;
    private static boolean serverRunning = false;

    private View contentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo wifiP2pInfo;
    ProgressDialog progressDialog = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.device_detail,null);
        contentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
                wifiP2pConfig.deviceAddress = device.deviceAddress;
                wifiP2pConfig.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press Back to cancel",
                        "Connecting to " + device.deviceAddress, true, true);
                ((DeviceListFragment.DeviceActionListener)getActivity()).connect(wifiP2pConfig);
            }
        });

        contentView.findViewById(R.id.btn_disconnect).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceListFragment.DeviceActionListener) getActivity()).disconnect();
                    }
        });

        contentView.findViewById(R.id.btn_start_client).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //write a message
            }
        });

        return contentView;
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) contentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) contentView.findViewById(R.id.device_info);
        view.setText(device.toString());
    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        contentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) contentView.findViewById(R.id.device_address);
        view.setText("");
        view = (TextView) contentView.findViewById(R.id.device_info);
        view.setText("");
        view = (TextView) contentView.findViewById(R.id.group_owner);
        view.setText("");
        view = (TextView) contentView.findViewById(R.id.status_text);
        view.setText("");
        contentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.wifiP2pInfo = info;
        this.getView().setVisibility(View.VISIBLE);

        // the owner IP is known
        TextView view = (TextView) contentView.findViewById(R.id.group_owner);
        view.setText("" + ((info.isGroupOwner == true) ? "Group Owner" : "Not Group Owner"));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) contentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        contentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);

        if (!serverRunning){
            new ServerAsyncTask(getActivity(), contentView.findViewById(R.id.status_text)).execute();
            serverRunning = true;
        }

        // hide the connect button
        contentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }
    /**
     * A simple server socket that accepts connection and
     * write some data on the buffer
     */
    public class ServerAsyncTask extends AsyncTask {

        private Context context;
        private TextView statusText;

        DatagramSocket datagramSocket;
        byte buffer[] = new byte[256];
        DatagramPacket datagramPacket;

        public ServerAsyncTask(Context context, View statusText){
            this.context = context;
            this.statusText = (TextView)statusText;
            datagramPacket = new DatagramPacket(buffer, buffer.length);
            try {
                datagramSocket = new DatagramSocket(PORT, InetAddress.getByName(SERVER_IP));
            } catch (SocketException e) {
                Log.d("socket creation error", "SocketException");
                e.printStackTrace();
            } catch (UnknownHostException e) {
                Log.d("socket creation error", "UnknownHostException");
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(Object[] params) {
            try{
                datagramPacket.setLength(buffer.length);
                datagramSocket.receive(datagramPacket);
                // display the packet received as a Toast here
                serverRunning = false;
                return buffer.toString();
            }catch (IOException e) {
                Log.d("error","datagram error");
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            // display a success or failure msg
            datagramSocket.close();
        }

        @Override
        protected void onPreExecute() {
            // display a msg on opening a socket
        }
    }

}
