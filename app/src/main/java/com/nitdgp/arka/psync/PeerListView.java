package com.nitdgp.arka.psync;
/**
 * Created by arka on 16/3/16.
 */
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;


public class PeerListView extends BaseAdapter{
    private final Context context;
    private final List<String> peer;
    private final List<Integer> counter;

    public PeerListView(Context context, List<String > peer, List<Integer> counter) {
        this.context = context;
        this.peer = peer;
        this.counter = counter;
    }

    @Override
    public int getCount() {
        return peer.size();
    }

    @Override
    public Object getItem(int position) {
        return peer.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    class ViewHolder {
        TextView peerIP;
        TextView peerCounter;

        public ViewHolder(View view){
            peerIP = (TextView)view.findViewById(R.id.peer_listview_row_peerIP);
            peerCounter = (TextView)view.findViewById(R.id.peer_listview_row_peerCounter);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        ViewHolder viewHolder;

        if(rowView == null) {
            LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = layoutInflater.inflate(R.layout.peer_listview_row, parent, false);
            viewHolder = new ViewHolder(rowView);
            rowView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)rowView.getTag();
        }

        viewHolder.peerIP.setText(peer.get(position));
        viewHolder.peerCounter.setText(Integer.toString(counter.get(position)));

        return rowView;
    }
}
