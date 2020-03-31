package com.example.trackingapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class SlidingMenuAdapter extends BaseAdapter {

    private Context context;
    private List<ItemSlideMenu> item;

    public SlidingMenuAdapter(Context context, List<ItemSlideMenu> item) {
        this.context = context;
        this.item = item;
    }

    @Override
    public int getCount() {
        return item.size();
    }

    @Override
    public Object getItem(int position) {
        return item.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_sliding_menu, parent, false);
        }

        ImageView img = convertView.findViewById(R.id.item_img);
        TextView title = convertView.findViewById(R.id.item_title);

        ItemSlideMenu item_menu = item.get(position);
        img.setImageResource(item_menu.getImg_id());
        title.setText(item_menu.getTitle());

        return convertView;
    }
}
