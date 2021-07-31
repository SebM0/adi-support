package com.axway.adi.tools.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// Copy of Guava's Murmur3_32HashFunction
public class RedoLogHasher {
    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;
    private int h1;
    private long buffer;
    private int shift;
    private int length;
    private boolean isDone;

    public RedoLogHasher(int seed) {
        this.h1 = seed;
        this.length = 0;
        isDone = false;
    }

    private void update(int nBytes, long update) {
        // 1 <= nBytes <= 4
        buffer |= (update & 0xFFFFFFFFL) << shift;
        shift += nBytes * 8;
        length += nBytes;

        if (shift >= 32) {
            h1 = mixH1(h1, mixK1((int) buffer));
            buffer >>>= 32;
            shift -= 32;
        }
    }

    private static int mixK1(int k1) {
        k1 *= C1;
        k1 = Integer.rotateLeft(k1, 15);
        k1 *= C2;
        return k1;
    }

    private static int mixH1(int h1, int k1) {
        h1 ^= k1;
        h1 = Integer.rotateLeft(h1, 13);
        h1 = h1 * 5 + 0xe6546b64;
        return h1;
    }

    public void putInt(int i) {
        update(4, i);
    }

    public void putByte(byte b) {
        update(1, b & 0xFF);
    }

    public void putBytes(ByteBuffer buffer) {
        ByteOrder bo = buffer.order();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() >= 4) {
            putInt(buffer.getInt());
        }
        while (buffer.hasRemaining()) {
            putByte(buffer.get());
        }
        buffer.order(bo);
    }

    public int hash() {
        if (isDone) {
            throw new IllegalStateException("Hash already computed");
        }
        isDone = true;
        h1 ^= mixK1((int) buffer);
        return fmix(h1, length);
    }

    // Finalization mix - force all bits of a hash block to avalanche
    private static int fmix(int h1, int length) {
        h1 ^= length;
        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;
        return h1;
    }
}
