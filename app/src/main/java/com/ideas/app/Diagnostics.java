package com.ideas.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Diagnostics {
    private final Context context;
    private final Bootstrap bootstrap;

    public Diagnostics(Context context) {
        this.context = context.getApplicationContext();
        this.bootstrap = new Bootstrap(context);
    }

    public File export() throws IOException {
        File outDir = new File(context.getExternalCacheDir(), "diagnostics");
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IOException("Cannot create diagnostics dir");
        }
        File zip = new File(outDir, "ideas-diagnostics.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            addDir(bootstrap.getLogsDir(), zos, "logs");
            addFile(new File(bootstrap.getRuntimeDir(), "server.log"), zos, "server.log");
            addFile(new File(bootstrap.getRuntimeDir(), "proot-debug.log"), zos, "proot-debug.log");
            addFile(new File(bootstrap.getRuntimeDir(), "server-debug.log"), zos, "server-debug.log");
            addText(zos, "env.txt", getEnvInfo());
        }
        return zip;
    }

    public void share() throws IOException {
        File zip = export();
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", zip);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/zip");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(share, "Share IDEAS diagnostics"));
    }

    private void addDir(File dir, ZipOutputStream zos, String prefix) throws IOException {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile()) {
                addFile(f, zos, prefix + "/" + f.getName());
            }
        }
    }

    private void addFile(File f, ZipOutputStream zos, String name) throws IOException {
        if (f == null || !f.exists()) return;
        try (FileInputStream fis = new FileInputStream(f)) {
            ZipEntry ze = new ZipEntry(name);
            zos.putNextEntry(ze);
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) > 0) {
                zos.write(buf, 0, n);
            }
            zos.closeEntry();
        }
    }

    private void addText(ZipOutputStream zos, String name, String text) throws IOException {
        ZipEntry ze = new ZipEntry(name);
        zos.putNextEntry(ze);
        if (text != null) {
            zos.write(text.getBytes());
        }
        zos.closeEntry();
    }

    private String getEnvInfo() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("nativeLibraryDir=").append(context.getApplicationInfo().nativeLibraryDir).append("\n");
        File loader = new File(context.getApplicationInfo().nativeLibraryDir, "libloader.so");
        sb.append("libloader.exists=").append(loader.exists()).append(" size=").append(loader.length()).append("\n");
        File proot = new File(context.getApplicationInfo().nativeLibraryDir, "libproot.so");
        sb.append("libproot.exists=").append(proot.exists()).append(" size=").append(proot.length()).append("\n");
        File rt = bootstrap.getRuntimeDir();
        sb.append("runtimeDir=").append(rt.getAbsolutePath()).append(" exists=").append(rt.exists()).append("\n");
        sb.append("rootfs/bin/node exists=").append(new File(bootstrap.getRootfsDir(), "usr/bin/node").exists()).append("\n");
        sb.append("rootfs/bin/busybox exists=").append(new File(bootstrap.getRootfsDir(), "bin/busybox").exists()).append("\n");
        return sb.toString();
    }
}
