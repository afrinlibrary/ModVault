package com.modvault.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;
import com.modvault.app.R;
import java.io.File;
import java.util.List;

public class InstalledModsAdapter extends RecyclerView.Adapter<InstalledModsAdapter.ViewHolder> {
    public interface OnDeleteListener { void onDelete(Object mod); }
    public interface OnDisableListener { void onDisable(Object mod); }

    private final List<Object> mods;
    private final OnDeleteListener deleteListener;
    private final OnDisableListener disableListener;

    public InstalledModsAdapter(List<Object> mods, OnDeleteListener deleteListener) {
        this.mods = mods;
        this.deleteListener = deleteListener;
        this.disableListener = null;
    }

    public InstalledModsAdapter(List<Object> mods, OnDeleteListener deleteListener, OnDisableListener disableListener) {
        this.mods = mods;
        this.deleteListener = deleteListener;
        this.disableListener = disableListener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_installed_mod, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object mod = mods.get(position);
        String name = "";
        long size = 0;

        if (mod instanceof DocumentFile) {
            DocumentFile df = (DocumentFile) mod;
            name = df.getName() != null ? df.getName() : "";
            size = df.length();
        } else if (mod instanceof File) {
            File f = (File) mod;
            name = f.getName();
            size = f.length();
        }

        holder.name.setText(name);
        holder.size.setText(formatSize(size));

        // Type badge
        boolean isDisabled = name.endsWith(".disabled");
        String ext = isDisabled ? ".disabled" : name.endsWith(".jar") ? ".jar" : ".zip";
        holder.typeBadge.setText(ext);
        holder.typeBadge.setTextColor(isDisabled ? 0xFF888888 : 0xFF2D7D46);

        // Dim disabled mods
        holder.itemView.setAlpha(isDisabled ? 0.5f : 1f);

        // Disable/enable button
        holder.btnDisable.setImageResource(isDisabled ? R.drawable.ic_play : R.drawable.ic_pause);
        holder.btnDisable.setColorFilter(isDisabled ? 0xFF4CAF50 : 0xFF888888);
        final Object modRef = mod;
        holder.btnDisable.setOnClickListener(v -> {
            if (disableListener != null) disableListener.onDisable(modRef);
        });

        holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(modRef));
    }

    @Override public int getItemCount() { return mods.size(); }

    private String formatSize(long bytes) {
        if (bytes >= 1024 * 1024) return String.format("%.1f MB", bytes / (1024f * 1024f));
        return String.format("%.1f KB", bytes / 1024f);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, size, typeBadge;
        ImageButton btnDelete, btnDisable;
        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.mod_filename);
            size = v.findViewById(R.id.mod_size);
            typeBadge = v.findViewById(R.id.mod_type_badge);
            btnDelete = v.findViewById(R.id.btn_delete_mod);
            btnDisable = v.findViewById(R.id.btn_disable_mod);
        }
    }
}
