package com.mediax.psstreaming.controller.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends FilterInputStream {
    private long remaining;

    public LimitedInputStream(InputStream in, long limit) {
        super(in);
        this.remaining = limit;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) return -1;
        int read = super.read(b, off, (int) Math.min(len, remaining));
        if (read > 0) remaining -= read;
        return read;
    }

    @Override
    public int read() throws IOException {
        if (remaining <= 0) return -1;
        int read = super.read();
        if (read != -1) remaining--;
        return read;
    }
}
