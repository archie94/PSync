package com.nitdgp.arka.psync;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

/**
 * Created by arka on 5/4/16.
 */
public class ActiveDownloadsAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> fileNameList;
    private final List<Long> downloadedSizeList;
    private final List<Long> fileSizeList;

    public ActiveDownloadsAdapter(Context context, List<String> fileNameList, List<Long> downloadedSizeList,
                                  List<Long> fileSizeList) {
        this.context = context;
        this.fileNameList = fileNameList;
        this.downloadedSizeList = downloadedSizeList;
        this.fileSizeList = fileSizeList;
    }

    @Override
    public int getCount() {
        return fileNameList.size();
    }

    @Override
    public Object getItem(int position) {
        return fileNameList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    class ViewHolder {
        TextView fileName;
        TextView downloadedSize;
        TextView fileSize;

        public ViewHolder(View view) {
            fileName = (TextView)view.findViewById(R.id.active_download_row_filename);
            downloadedSize = (TextView)view.findViewById(R.id.active_download_row_downloaded);
            fileSize = (TextView)view.findViewById(R.id.active_download_row_filesize);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        ViewHolder viewHolder;

        if(rowView == null) {
            LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = layoutInflater.inflate(R.layout.active_download_row, parent, false);
            viewHolder = new ViewHolder(rowView);
            rowView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)rowView.getTag();
        }

        viewHolder.fileName.setText(fileNameList.get(position));
        viewHolder.downloadedSize.setText(Long.toString(downloadedSizeList.get(position)));
        viewHolder.fileSize.setText(Long.toString(fileSizeList.get(position)));

        return rowView;
    }
}
