package com.modvault.app.ui;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;
import com.modvault.app.R;
import com.modvault.app.utils.ModIconLoader;
import java.io.File;
import java.util.List;

public class InstalledModsAdapter extends RecyclerView.Adapter<InstalledModsAdapter.ViewHolder> {
    public interface OnDeleteListener { void onDelete(Object mod); }
    public interface OnDisableListener { void onDisable(Object mod); }

    private final List<Object> mods;
    private final OnDeleteListener deleteListener;
    private final OnDisableListener disableListener;
    private boolean showDisable = true;
    private String currentType = "mods";

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

    public void setShowDisable(boolean show) { this.showDisable = show; }
    public void setCurrentType(String type) { this.currentType = type; }

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

        // Load icon - set placeholder first to prevent swapping during scroll
        if (holder.icon != null) {
            holder.icon.setImageResource(R.drawable.ic_mod_default);
            holder.icon.setTag(name); // tag with filename to detect recycled views
            ModIconLoader.FileType fileType = getFileType();
            final String tagName = name;
            final android.widget.ImageView iconView = holder.icon;
            if (mod instanceof DocumentFile) {
                ModIconLoader.load(iconView.getContext(), (DocumentFile) mod, fileType, new android.widget.ImageView(iconView.getContext()) {
                    @Override public void setImageBitmap(android.graphics.Bitmap bm) {
                        if (tagName.equals(iconView.getTag())) iconView.setImageBitmap(bm);
                    }
                    @Override public void setImageResource(int res) {
                        if (tagName.equals(iconView.getTag())) iconView.setImageResource(res);
                    }
                });
            } else if (mod instanceof File) {
                ModIconLoader.load(iconView.getContext(), (File) mod, fileType, new android.widget.ImageView(iconView.getContext()) {
                    @Override public void setImageBitmap(android.graphics.Bitmap bm) {
                        if (tagName.equals(iconView.getTag())) iconView.setImageBitmap(bm);
                    }
                    @Override public void setImageResource(int res) {
                        if (tagName.equals(iconView.getTag())) iconView.setImageResource(res);
                    }
                });
            }
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
        holder.btnDisable.setVisibility(showDisable ? android.view.View.VISIBLE : android.view.View.GONE);
        holder.btnDisable.setImageResource(isDisabled ? R.drawable.ic_play : R.drawable.ic_pause);
        holder.btnDisable.setColorFilter(isDisabled ? 0xFF4CAF50 : 0xFF888888);
        final Object modRef = mod;
        holder.btnDisable.setOnClickListener(v -> {
            if (disableListener != null) disableListener.onDisable(modRef);
        });

        holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(modRef));
    }

    @Override public int getItemCount() { return mods.size(); }

    private ModIconLoader.FileType getFileType() {
        if ("shaderpacks".equals(currentType)) return ModIconLoader.FileType.SHADER;
        if ("resourcepacks".equals(currentType)) return ModIconLoader.FileType.RESOURCEPACK;
        return ModIconLoader.FileType.MOD;
    }

    private String formatSize(long bytes) {
        if (bytes >= 1024 * 1024) return String.format("%.1f MB", bytes / (1024f * 1024f));
        return String.format("%.1f KB", bytes / 1024f);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        android.widget.ImageView icon;
        TextView name, size, typeBadge;
        ImageButton btnDelete, btnDisable;
        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.mod_filename);
            size = v.findViewById(R.id.mod_size);
            typeBadge = v.findViewById(R.id.mod_type_badge);
            icon = v.findViewById(R.id.mod_icon);
            btnDelete = v.findViewById(R.id.btn_delete_mod);
            btnDisable = v.findViewById(R.id.btn_disable_mod);
        }
    }
}
