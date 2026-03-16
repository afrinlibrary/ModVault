package com.modvault.app;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.modvault.app.api.CurseForgeApi;
import com.modvault.app.api.ModrinthApi;
import com.modvault.app.model.ModResult;
import com.modvault.app.model.ModVersion;
import com.modvault.app.ui.VersionAdapter;
import com.modvault.app.utils.ModDownloader;
import com.modvault.app.utils.PrefManager;
import java.util.ArrayList;
import java.util.List;

public class ModDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MOD = "mod_json";
    public static final String EXTRA_PROJECT_TYPE = "project_type";
    public static final String EXTRA_SOURCE = "source";

    private ModResult mod;
    private String projectType, source;
    private boolean includeSnapshots;
    private String gameVersion, loader;
    private ModDownloader downloader;
    private PrefManager prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ModrinthApi api = new ModrinthApi();
    private final CurseForgeApi cfApi = new CurseForgeApi();

    private View contentDescription, contentGallery, contentVersions;
    private TextView tabDescription, tabGallery, tabVersions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod_detail);

        downloader = new ModDownloader(this);
        prefs = new PrefManager(this);

        String modJson = getIntent().getStringExtra(EXTRA_MOD);
        projectType = getIntent().getStringExtra(EXTRA_PROJECT_TYPE);
        source = getIntent().getStringExtra(EXTRA_SOURCE);
        includeSnapshots = getIntent().getBooleanExtra("include_snapshots", false);
        gameVersion = getIntent().getStringExtra("game_version") != null ? getIntent().getStringExtra("game_version") : "";
        loader = getIntent().getStringExtra("loader") != null ? getIntent().getStringExtra("loader") : "";

        if (modJson == null) { finish(); return; }
        mod = new com.google.gson.Gson().fromJson(modJson, ModResult.class);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_detail_title)).setText(mod.title);

        // Icon
        ImageView icon = findViewById(R.id.detail_icon);
        if (mod.iconUrl != null && !mod.iconUrl.isEmpty()) {
            Glide.with(this).load(mod.iconUrl).placeholder(R.drawable.ic_mod_default).into(icon);
        } else {
            icon.setImageResource(R.drawable.ic_mod_default);
        }

        ((TextView) findViewById(R.id.detail_title)).setText(mod.title);
        String typeLabel = "resourcepack".equals(projectType) ? "Resource Pack"
                         : "shader".equals(projectType) ? "Shader" : "Mod";
        ((TextView) findViewById(R.id.detail_type_badge)).setText(typeLabel);
        ((TextView) findViewById(R.id.detail_downloads)).setText(formatNumber(mod.downloads));
        ((TextView) findViewById(R.id.detail_followers)).setText(formatNumber(mod.followers));

        // Categories
        ChipGroup chipGroup = findViewById(R.id.detail_categories);
        if (mod.categories != null) {
            for (String cat : mod.categories) {
                Chip chip = new Chip(this);
                chip.setText(cat);
                chip.setChipBackgroundColorResource(android.R.color.transparent);
                chip.setTextColor(0xFFB87333);
                chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(0xFFB87333));
                chip.setChipStrokeWidth(1f);
                chip.setClickable(false);
                chipGroup.addView(chip);
            }
        }

        // Tab views
        tabDescription = findViewById(R.id.tab_description);
        tabGallery = findViewById(R.id.tab_gallery);
        tabVersions = findViewById(R.id.tab_versions);
        contentDescription = findViewById(R.id.content_description);
        contentGallery = findViewById(R.id.content_gallery);
        contentVersions = findViewById(R.id.content_versions);

        tabDescription.setOnClickListener(v -> switchTab(0));
        tabGallery.setOnClickListener(v -> switchTab(1));
        tabVersions.setOnClickListener(v -> switchTab(2));

        // Description content
        ((TextView) findViewById(R.id.detail_description)).setText(mod.description);

        // Fetch full project details
        if (!"curseforge".equals(source)) {
            api.getProject(mod.projectId, new ModrinthApi.Callback<ModResult>() {
                public void onSuccess(ModResult fullMod) {
                    handler.post(() -> {
                        ((TextView) findViewById(R.id.detail_followers)).setText(formatNumber(fullMod.followers));
                        ((TextView) findViewById(R.id.detail_downloads)).setText(formatNumber(fullMod.downloads));
                        if (fullMod.description != null)
                            ((TextView) findViewById(R.id.detail_description)).setText(fullMod.description);
                        loadGallery(fullMod);
                    });
                }
                public void onError(String error) {}
            });
        }

        // Load versions
        loadVersions();
    }

    private void loadVersions() {
        ProgressBar progress = findViewById(R.id.detail_versions_progress);
        RecyclerView versionsRecycler = findViewById(R.id.detail_versions_recycler);
        versionsRecycler.setLayoutManager(new LinearLayoutManager(this));
        progress.setVisibility(View.VISIBLE);

        if ("curseforge".equals(source)) {
            cfApi.getLatestFile(mod.projectId, "", "", fileObj -> {
                handler.post(() -> {
                    progress.setVisibility(View.GONE);
                    if (fileObj == null) return;
                    String fileId = fileObj.get("id").getAsString();
                    String fileName = fileObj.get("fileName").getAsString();
                    cfApi.getDownloadUrl(mod.projectId, fileId, url -> {
                        handler.post(() -> {
                            ModVersion fakeVersion = new ModVersion();
                            fakeVersion.versionNumber = fileName;
                            fakeVersion.versionType = "release";
                            fakeVersion.dependencies = new ArrayList<>();
                            ModVersion.VersionFile file = new ModVersion.VersionFile();
                            file.url = url; file.filename = fileName; file.primary = true;
                            fakeVersion.files = java.util.Arrays.asList(file);
                            versionsRecycler.setAdapter(new VersionAdapter(
                                java.util.Arrays.asList(fakeVersion), (v, f) -> startDownload(v, f)));
                        });
                    }, err -> {});
                });
            }, error -> handler.post(() -> progress.setVisibility(View.GONE)));
        } else {
            api.getVersions(mod.projectId, gameVersion, loader, versions -> {
                handler.post(() -> {
                    progress.setVisibility(View.GONE);
                    if (versions == null || versions.isEmpty()) {
                        Toast.makeText(this, "No versions found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<ModVersion> filtered = new ArrayList<>();
                    for (ModVersion v : versions) {
                        String vType = v.versionType != null ? v.versionType : "release";
                        if ("release".equals(vType) || includeSnapshots) filtered.add(v);
                    }
                    versionsRecycler.setAdapter(new VersionAdapter(filtered, (v, f) -> startDownload(v, f)));
                });
            }, error -> handler.post(() -> {
                progress.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to load versions", Toast.LENGTH_SHORT).show();
            }));
        }
    }

    private void loadGallery(ModResult fullMod) {
        RecyclerView galleryRecycler = findViewById(R.id.gallery_recycler);
        TextView noGallery = findViewById(R.id.tv_no_gallery);
        if (fullMod == null || fullMod.gallery == null || fullMod.gallery.isEmpty()) {
            noGallery.setVisibility(View.VISIBLE);
            galleryRecycler.setVisibility(View.GONE);
        } else {
            noGallery.setVisibility(View.GONE);
            galleryRecycler.setVisibility(View.VISIBLE);
            galleryRecycler.setLayoutManager(new GridLayoutManager(this, 2));
            List<String> urls = new ArrayList<>();
            for (ModResult.GalleryItem item : fullMod.gallery) {
                if (item != null && item.url != null) urls.add(item.url);
            }
            galleryRecycler.setAdapter(new com.modvault.app.ui.GalleryAdapter(this, urls));
        }
    }

    private void switchTab(int index) {
        contentDescription.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        contentGallery.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        contentVersions.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        tabDescription.setTextColor(index == 0 ? 0xFFB87333 : 0xFF888888);
        tabGallery.setTextColor(index == 1 ? 0xFFB87333 : 0xFF888888);
        tabVersions.setTextColor(index == 2 ? 0xFFB87333 : 0xFF888888);
        tabDescription.setTypeface(null, index == 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabGallery.setTypeface(null, index == 1 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabVersions.setTypeface(null, index == 2 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void startDownload(ModVersion version, ModVersion.VersionFile file) {
        String subFolder = "resourcepack".equals(projectType) ? "resourcepacks"
                         : "shader".equals(projectType) ? "shaderpacks" : "mods";
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Installing " + mod.title);
        progress.setMessage("Downloading\u2026");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(100);
        progress.setCancelable(false);
        progress.show();
        ModDownloader.DownloadCallback callback = new ModDownloader.DownloadCallback() {
            public void onProgress(String fileName, int percent) {
                handler.post(() -> { progress.setMessage(fileName); progress.setProgress(percent); });
            }
            public void onSuccess(String fileName) {
                handler.post(() -> {
                    progress.dismiss();
                    Toast.makeText(ModDetailActivity.this, mod.title + " installed!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
            public void onError(String error) {
                handler.post(() -> {
                    progress.dismiss();
                    Toast.makeText(ModDetailActivity.this, "Install failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        };
        Uri instanceUri = prefs.getInstanceUri();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R
                && instanceUri != null && "content".equals(instanceUri.getScheme())) {
            downloader.downloadMod(file, instanceUri, subFolder, version.dependencies, "", "", callback);
        } else {
            java.io.File instanceDir = instanceUri != null ? new java.io.File(instanceUri.getPath()) : null;
            if (instanceDir == null) { progress.dismiss(); return; }
            java.io.File targetDir = new java.io.File(instanceDir, subFolder);
            if (!targetDir.exists()) targetDir.mkdirs();
            downloader.downloadMod(file, targetDir, version.dependencies, "", "", callback);
        }
    }

    private String formatNumber(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000f);
        if (n >= 1_000) return String.format("%.1fK", n / 1_000f);
        return String.valueOf(n);
    }
}
