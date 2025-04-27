package indi.somebottle.streams;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 加上了一个计数器的 OutputStream 包装流
 */
public class ByteCountingOutputStream extends OutputStream {
    private long bytesCount = 0;
    private final OutputStream out;

    public ByteCountingOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        bytesCount++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        bytesCount += len;
    }

    /**
     * 返回当前已经写入的字节数
     *
     * @return 当前已经写入的字节数
     */
    public long getBytesCount() {
        return bytesCount;
    }
}
