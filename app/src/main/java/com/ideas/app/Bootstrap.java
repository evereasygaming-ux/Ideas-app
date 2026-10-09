package com.ideas.app;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Owns the on-device runtime: extracts the bundled Alpine rootfs, installs
 * Node.js, and verifies the environment. All work happens off the main
 * thread and is safe to re-run (idempotent).
 */
public class Bootstrap {

    private static final String TAG = "IDEAS-Bootstrap";
    private static final String MARKER = ".ready";
    private static final long PROOT_TIMEOUT_MIN = 25;

    public interface Listener {
        void onProgress(int percent, String message);

        void onFinished(boolean success, String message);
    }

    private final Context app;

    public Bootstrap(Context context) {
        this.app = context.getApplicationContext();
    }

    public File getRuntimeDir() {
        return new File(app.getFilesDir(), "runtime");
    }

    public File getRootfsDir() {
        return new File(getRuntimeDir(), "rootfs");
    }

    public File getLogsDir() {
        return new File(getRuntimeDir(), "logs");
    }

    public File getMarkerFile() {
        return new File(getRuntimeDir(), MARKER);
    }

    public boolean isReady() {
        return getMarkerFile().isFile() && getNodeFile().exists();
    }

    public File getNodeFile() {
        return new File(getRootfsDir(), "usr/bin/node");
    }

    public void run(Listener listener) {
        new Thread(() -> {
            try {
                doRun(listener);
                listener.onFinished(true, "Runtime ready");
            } catch (Throwable t) {
                Log.e(TAG, "Bootstrap failed", t);
                String msg = t.getMessage() == null ? t.toString() : t.getMessage();
                listener.onFinished(false, msg);
            }
        }, "ideas-bootstrap").start();
    }

    private void doRun(Listener l) throws Exception {
        getLogsDir().mkdirs();
        getRootfsDir().mkdirs();

        l.onProgress(5, "Preparing PRoot...");
        File proot = ensureProot();
        proot.setExecutable(true, false);

        l.onProgress(15, "Extracting Alpine rootfs...");
        if (!new File(getRootfsDir(), "bin/busybox").exists()) {
            File tmp = new File(getRuntimeDir(), "rootfs.tar");
            copyAsset("alpine-rootfs.tar", tmp);
            TarExtractor.extract(tmp, getRootfsDir());
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }

        File resolv = new File(getRootfsDir(), "etc/resolv.conf");
        writeText(resolv, "nameserver 8.8.8.8\nnameserver 1.1.1.1\n");

        l.onProgress(30, "Verifying shell...");
        String shell = runProot(proot, new String[]{"/bin/sh", "-c", "echo SHELL_OK"});
        if (!shell.contains("SHELL_OK")) {
            throw new IOException("Shell smoke test failed: " + shell);
        }

        if (!getNodeFile().exists()) {
            l.onProgress(45, "Installing Node.js (may take a few minutes)...");
            File script = new File(getRootfsDir(), "root/bootstrap.sh");
            copyAsset("bootstrap.sh", script);
            String out = runProot(proot, new String[]{"/bin/sh", "/root/bootstrap.sh"});
            if (!getNodeFile().exists()) {
                throw new IOException("Node.js install failed: " + out);
            }
        }

        l.onProgress(85, "Verifying Node.js...");
        String node = runProot(proot, new String[]{"/usr/bin/node", "-e", "console.log('NODE_OK')"});
        if (!node.contains("NODE_OK")) {
            throw new IOException("Node smoke test failed: " + node);
        }

        l.onProgress(95, "Finalizing...");
        writeText(getMarkerFile(), "ready\n");
        l.onProgress(100, "Ready");
    }

    /**
     * Resolves the PRoot binary. On Android 10+ apps cannot execute files
     * they own and can write (W^X), so the preferred location is the native
     * library directory, where the binary is extracted by the installer.
     * Older devices fall back to a copy in app storage.
     */
    public File ensureProot() throws IOException {
        String nativeDir = app.getApplicationInfo().nativeLibraryDir;
        File nativeProot = new File(nativeDir, "libproot.so");
        if (nativeProot.exists() && nativeProot.length() > 0) {
            return nativeProot;
        }
        File dst = new File(getRuntimeDir(), "proot");
        if (!dst.exists() || dst.length() == 0) {
            copyAsset("proot-arm64", dst);
        }
        return dst;
    }

    public String runProot(File proot, String[] innerArgs) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(proot.getAbsolutePath());
        cmd.add("-r");
        cmd.add(getRootfsDir().getAbsolutePath());
        cmd.add("-0");
        cmd.add("-w");
        cmd.add("/root");
        cmd.add("-b");
        cmd.add("/dev");
        cmd.add("-b");
        cmd.add("/proc");
        cmd.add("-v");
        cmd.add("3");
        for (String a : innerArgs) {
            cmd.add(a);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        pb.environment().put("PROOT_TMP_DIR", getRuntimeDir().getAbsolutePath());
        pb.environment().put("PROOT_NO_SECCOMP", "1");
        File loader = new File(app.getApplicationInfo().nativeLibraryDir, "libloader.so");
        File loaderRuntime = new File(getRuntimeDir(), "loader");
        try {
            if (loader.exists() && !loaderRuntime.exists()) {
                java.nio.file.Files.copy(loader.toPath(), loaderRuntime.toPath());
                loaderRuntime.setExecutable(true, false);
            }
        } catch (Exception ignored) {}
        if (loaderRuntime.exists() && loaderRuntime.canExecute()) {
            pb.environment().put("PROOT_LOADER", loaderRuntime.getAbsolutePath());
        } else {
            pb.environment().put("PROOT_LOADER", loader.getAbsolutePath());
        }


        Process p = pb.start();

        byte[] buf = new byte[8192];
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        Thread pump = new Thread(() -> {
            try (InputStream in = p.getInputStream()) {
                int n;
                while ((n = in.read(buf)) > 0) {
                    captured.write(buf, 0, n);
                }
            } catch (IOException ignored) {
                // Pump thread must never crash the bootstrap.
            }
        }, "ideas-proot-pump");
        pump.start();

        boolean finished = p.waitFor(PROOT_TIMEOUT_MIN, TimeUnit.MINUTES);
        if (!finished) {
            p.destroyForcibly();
            pump.join(2000);
            throw new IOException("proot timed out running: " + String.join(" ", innerArgs));
        }
        pump.join(2000);
        String output = captured.toString(StandardCharsets.UTF_8.name());
        Log.i(TAG, "proot " + String.join(" ", innerArgs) + " -> exit " + p.exitValue()
                + ", output " + captured.size() + " bytes");
        try {
            if (p.exitValue() != 0 || output.length() > 0) {
                File dbg = new File(getRuntimeDir(), "proot-debug.log");
                java.io.FileWriter fw = new java.io.FileWriter(dbg, false);
                fw.write("cmd: " + String.join(" ", cmd) + "\n");
                fw.write("exit: " + p.exitValue() + "\n");
                fw.write("output:\n" + output);
                fw.close();
            }
        } catch (Exception ignored) {}
        return output;
    }

    void copyAsset(String assetName, File dest) throws IOException {
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (InputStream in = app.getAssets().open(assetName);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
    }

    static void writeText(File file, String text) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
