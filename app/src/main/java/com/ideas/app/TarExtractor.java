package com.ideas.app;

import android.system.Os;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.zip.GZIPInputStream;

/**
 * Minimal, dependency-free tar (gz) extractor.
 *
 * Supports exactly the entry types used by the bundled Alpine rootfs:
 * regular files ('0'/'\0'), directories ('5') and symlinks ('2').
 * No long-name, pax, or hardlink handling is required by the current
 * asset (verified: 87 files, 97 dirs, 334 symlinks, no pax/longname).
 */
final class TarExtractor {

    private static final String TAG = "IDEAS-Tar";
    private static final int BLOCK = 512;

    private TarExtractor() {
    }

    static void extract(File tarFile, File destDir) throws IOException {
        try (FileInputStream fin = new FileInputStream(tarFile)) {
            byte[] magic = new byte[2];
            int n = fin.read(magic);
            InputStream in;
            if (n == 2 && (magic[0] & 0xFF) == 0x1F && (magic[1] & 0xFF) == 0x8B) {
                in = new GZIPInputStream(new SequenceInputStream(new ByteArrayInputStream(magic), fin), 1 << 16);
            } else {
                in = new SequenceInputStream(new ByteArrayInputStream(magic, 0, n), fin);
            }
            extract(in, destDir);
        }
    }

    static void extractGz(File tarGz, File destDir) throws IOException {
        extract(tarGz, destDir);
    }

    private static void extract(InputStream in, File destDir) throws IOException {
        byte[] header = new byte[BLOCK];
        byte[] buf = new byte[8192];

        while (true) {
            int read = readFully(in, header, 0, BLOCK);
            if (read == 0) {
                break;
            }
            if (read < BLOCK) {
                throw new IOException("Truncated tar header");
            }
            if (isZeroBlock(header)) {
                break;
            }

            String name = getString(header, 0, 100);
            String prefix = getString(header, 345, 155);
            if (!prefix.isEmpty()) {
                name = prefix + "/" + name;
            }
            long size = parseNumber(header, 124, 12);
            int mode = (int) parseNumber(header, 100, 8);
            char type = (char) (header[156] & 0xFF);
            String link = getString(header, 157, 100);

            name = sanitize(name);
            if (name == null || name.isEmpty()) {
                skipData(in, size);
                continue;
            }

            File out = new File(destDir, name);
            File parent = out.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            if (type == '5') {
                if (!out.exists()) {
                    out.mkdirs();
                }
                skipData(in, size);
            } else if (type == '2') {
                if (out.exists()) {
                    out.delete();
                }
                try {
                    Os.symlink(link, out.getAbsolutePath());
                } catch (Exception e) {
                    Log.w(TAG, "symlink failed: " + name + " -> " + link);
                }
                skipData(in, size);
            } else if (type == '0' || type == 0 || type == ' ') {
                if (size < 0) {
                    throw new IOException("Invalid size for " + name);
                }
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    long remaining = size;
                    while (remaining > 0) {
                        int toRead = (int) Math.min(buf.length, remaining);
                        int n = in.read(buf, 0, toRead);
                        if (n < 0) {
                            throw new IOException("Truncated data for " + name);
                        }
                        fos.write(buf, 0, n);
                        remaining -= n;
                    }
                }
                if ((mode & 0111) != 0) {
                    out.setExecutable(true, false);
                }
                skipPadding(in, size);
            } else {
                skipData(in, size);
            }
        }
    }

    private static void skipData(InputStream in, long size) throws IOException {
        if (size > 0) {
            skipFully(in, size);
            skipPadding(in, size);
        }
    }

    private static void skipPadding(InputStream in, long size) throws IOException {
        long padding = (BLOCK - (size % BLOCK)) % BLOCK;
        if (padding > 0) {
            skipFully(in, padding);
        }
    }

    private static void skipFully(InputStream in, long count) throws IOException {
        long remaining = count;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped <= 0) {
                if (in.read() < 0) {
                    throw new IOException("Unexpected end of tar stream");
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }

    private static int readFully(InputStream in, byte[] b, int off, int len) throws IOException {
        int total = 0;
        while (total < len) {
            int n = in.read(b, off + total, len - total);
            if (n < 0) {
                break;
            }
            total += n;
        }
        return total;
    }

    private static boolean isZeroBlock(byte[] b) {
        for (byte value : b) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    private static String getString(byte[] b, int off, int len) {
        int end = off;
        int limit = Math.min(off + len, b.length);
        while (end < limit && b[end] != 0) {
            end++;
        }
        return new String(b, off, end - off).trim();
    }

    private static long parseNumber(byte[] b, int off, int len) {
        if ((b[off] & 0x80) != 0) {
            long value = b[off] & 0x7F;
            for (int i = off + 1; i < off + len; i++) {
                value = (value << 8) | (b[i] & 0xFF);
            }
            return value;
        }
        String s = getString(b, off, len).trim();
        if (s.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(s, 8);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String sanitize(String name) {
        String n = name.replace('\\', '/');
        while (n.startsWith("/")) {
            n = n.substring(1);
        }
        if (n.startsWith("./")) {
            n = n.substring(2);
        }
        if (n.contains("../")) {
            throw new SecurityException("Illegal tar path: " + name);
        }
        while (n.endsWith("/")) {
            n = n.substring(0, n.length() - 1);
        }
        return n;
    }
}
