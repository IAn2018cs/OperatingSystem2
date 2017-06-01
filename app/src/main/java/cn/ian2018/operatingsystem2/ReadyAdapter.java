package cn.ian2018.operatingsystem2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by 陈帅 on 2017/4/24/024.
 */

public class ReadyAdapter extends BaseAdapter {
    private Context context;
    private List<PCB> list;

    public ReadyAdapter(Context context, List<PCB> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public PCB getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PCB pcb = getItem(position);
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_ready,parent,false);

            viewHolder = new ViewHolder();
            viewHolder.tv_id = (TextView) convertView.findViewById(R.id.tv_id);
            viewHolder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.tv_id.setText("id：" + pcb.id + "\t");
        viewHolder.tv_name.setText(pcb.name + "进程");

        return convertView;
    }

    static class ViewHolder {
        TextView tv_id;
        TextView tv_name;
    }
}
