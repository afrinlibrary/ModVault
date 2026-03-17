package com.modvault.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.modvault.app.R;
import java.io.File;
import java.util.List;

public class InstanceAdapter extends RecyclerView.Adapter<InstanceAdapter.ViewHolder> {
    public interface OnSelectListener { void onSelect(File modsFolder, String instanceName); }
    private final List<File> instances;
    private final OnSelectListener listener;
    private final Context ctx;

    public InstanceAdapter(Context ctx, List<File> instances, OnSelectListener listener) {
        this.ctx = ctx;
        this.instances = instances;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_instance, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File instance = instances.get(position);
        String absPath = instance.getAbsolutePath();
        if (absPath.startsWith("/saf_instance/")) {
            // SAF instance - extract URI and name
            String withoutPrefix = absPath.substring("/saf_instance/".length());
            int lastSlash = withoutPrefix.lastIndexOf("/");
            String uriStr = withoutPrefix.substring(0, lastSlash);
            String name = withoutPrefix.substring(lastSlash + 1);
            holder.name.setText(name);
            holder.path.setText(uriStr.length() > 50 ? "..." + uriStr.substring(uriStr.length()-50) : uriStr);
            holder.select.setOnClickListener(v -> {
                android.net.Uri uri = android.net.Uri.parse(uriStr);
                listener.onSelect(instance, name); // pass marker file
            });
        } else {
            holder.name.setText(instance.getName());
            holder.path.setText(instance.getAbsolutePath());
            holder.select.setOnClickListener(v -> {
                listener.onSelect(instance, instance.getName());
            });
        }
    }

    @Override public int getItemCount() { return instances.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, path;
        Button select;
        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.instance_name);
            path = v.findViewById(R.id.instance_path);
            select = v.findViewById(R.id.btn_select_instance);
        }
    }
}
