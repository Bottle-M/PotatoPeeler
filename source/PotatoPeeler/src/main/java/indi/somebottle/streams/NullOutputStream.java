package indi.somebottle.streams;

import java.io.OutputStream;

/**
 * 空输出流，用于模拟操作，所有写入这个流的数据都会被丢弃。
 */
public class NullOutputStream extends OutputStream {
    @Override
    public void write(int b) {
        // 啥都不做，俺就是个空输出流
    }

    @Override
    public void write(byte[] b, int off, int len) {
        // 啥都不做
    }
}
