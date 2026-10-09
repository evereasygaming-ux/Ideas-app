package com.ideas.app;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts, stops and monitors the local Node.js server that runs inside the
 * Alpine/PRoot rootfs. Only one instance is allowed at a time. Output is
 * captured to a bounded log file.
 */
public class ServerManager {

    private static final String TAG = "IDEAS-Server";
    private static final int PORT = 3000;
    private static final long MAX_LOG_BYTES = 512 * 1024;

    public interface Listener {
        void onState(String state, String message);
    }

    private final Context app;
    private final Bootstrap bootstrap;
    private final Object lock = new Object();

    private Process process;

    public ServerManager(Context context) {
        this.app = context.getApplicationContext();
        this.bootstrap = new Bootstrap(context);
    }

    public boolean isRunning() {
        synchronized (lock) {
            return process != null && process.isAlive();
        }
    }

    public void start(final Listener listener) {
        synchronized (lock) {
            if (process != null && process.isAlive()) {
                listener.onState("running", "Server already running");
                return;
            }
        }
        new Thread(() -> {
            try {
                if (!bootstrap.isReady()) {
                    listener.onState("error", "Runtime not ready");
                    return;
                }

                File proot = bootstrap.ensureProot();
                File rootfs = bootstrap.getRootfsDir();
                File serverDir = new File(rootfs, "root/ideas");
                if (!serverDir.exists() && !serverDir.mkdirs()) {
                    throw new IOException("Cannot create server dir: " + serverDir);
                }
                File serverJs = new File(serverDir, "server.js");
                bootstrap.copyAsset("server/server.js", serverJs);

                List<String> cmd = new ArrayList<>();
                cmd.add(proot.getAbsolutePath());
                cmd.add("-r");
                cmd.add(rootfs.getAbsolutePath());
                cmd.add("-0");
                cmd.add("-w");
                cmd.add("/root/ideas");
                cmd.add("-b");
                cmd.add("/dev");
                cmd.add("-b");
                cmd.add("/proc");
                cmd.add("-v");
                cmd.add("3");
                cmd.add("/usr/bin/node");
                cmd.add("server.js");

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                pb.environment().put("PROOT_TMP_DIR", bootstrap.getRuntimeDir().getAbsolutePath());
                pb.environment().put("PROOT_NO_SECCOMP", "1");
                pb.environment().put("PORT", String.valueOf(PORT));
                File loader = new File(app.getApplicationInfo().nativeLibraryDir, "libloader.so");
                pb.environment().put("PROOT_LOADER", loader.getAbsolutePath());

                Process started = pb.start();
                synchronized (lock) {
                    process = started;
                }
                listener.onState("running", "Server started on port " + PORT);

                pumpLogs(started);
                int code = started.waitFor();
                synchronized (lock) {
                    if (process == started) {
                        process = null;
                    }
                }
                listener.onState("stopped", "Server exited with code " + code);
            } catch (Exception e) {
                Log.e(TAG, "Server start failed", e);
                listener.onState("error", e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }, "ideas-server").start();
    }

    public void stop() {
        synchronized (lock) {
            if (process != null) {
                process.destroy();
                process = null;
            }
        }
    }

    private void pumpLogs(Process p) {
        File log = new File(bootstrap.getLogsDir(), "server.log");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            long written = 0;
            FileWriter writer = null;
            try {
                writer = new FileWriter(log, false);
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.i(TAG, line);
                    if (written < MAX_LOG_BYTES) {
                        String out = line + System.lineSeparator();
                        writer.write(out);
                        written += out.length();
                    }
                }
                writer.flush();
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Log pump ended: " + e.getMessage());
        }
    }
}
