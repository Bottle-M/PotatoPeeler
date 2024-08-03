package indi.somebottle.streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * 把 RandomAccessFile 封装成 InputStream 的类
 */
public class RandomAccessInputStream extends InputStream {
    /**
     * 指向待读取数据起始字节的 RandomAccessFile 对象
     */
    private final RandomAccessFile chunkReader;

    /**
     * 数据剩余可读的字节数
     */
    private int remainingDataLen;

    /**
     * 文件读取缓冲区
     */
    private byte[] readBuf = new byte[2048];

    /**
     * 文件读取缓冲区内现有的字节数
     */
    private int bufLen = 0;

    /**
     * 读取字节的指针
     */
    private int readPtr = 0;

    /**
     * 流是否已经关闭
     */
    private boolean isClosed = false;

    /**
     * 将 RandomAccessFile 包装成流 <br>
     * 注意： 流关闭的时候不会关闭 RandomAccessFile
     *
     * @param chunkReader RandomAccessFile 对象
     * @param dataLen     可读的数据长度
     * @apiNote 将从 RandomAccessFile 当前指针位置开始读取数据，读完 dataLen 个字节为止。
     */
    public RandomAccessInputStream(RandomAccessFile chunkReader, int dataLen) {
        this.chunkReader = chunkReader;
        this.remainingDataLen = dataLen;
    }

    /**
     * 内部方法，继续读取区块数据，存入缓冲区
     *
     * @return 读取的字节数，如果是 -1，代表没有更多字节可读
     */
    private int readRawDataToBuffer() throws IOException {
        int bytesRead; // 读取的字节数
        if (remainingDataLen > 0 && (bytesRead = chunkReader.read(readBuf, 0, Math.min(readBuf.length, remainingDataLen))) != -1) {
            // 如果有则把区块数据读入缓冲区 readBuf
            remainingDataLen -= bytesRead;
            bufLen = bytesRead;
        } else {
            // 否则没有更多数据可以读了
            return -1;
        }
        return bytesRead;
    }

    /**
     * 从 RAF 流中读取一个字节
     *
     * @return 读取的字节，如果已读取完毕则返回 -1
     * @throws IOException 如果读取失败会抛出此异常
     */
    @Override
    public int read() throws IOException {
        if (isClosed)
            return -1;
        if (readPtr >= bufLen) {
            // 缓冲区内没有更多数据可读了
            // 先把 readPtr 和 bufLen 归零
            readPtr = 0;
            bufLen = 0;
            // 检查能不能读入更多数据
            if (readRawDataToBuffer() == -1) {
                // 数据也读完了则返回 -1
                return -1;
            }
        }
        // 从缓冲区读取一个字节返回
        /*
            我在这里掉在一个坑里了，原本写的是
            int readByte = readBuf[readPtr]
            隐式把 byte 提升为了 int，而 byte 可能是负数
            这就导致返回的 int 字节表示不正确。

            SomeBottle 2024.8.3
         */
        int readByte = readBuf[readPtr] & 0xFF;
        readPtr++;
        return readByte;
    }

    @Override
    public void close() {
        if (!isClosed) {
            // 解开缓冲区引用
            readBuf = null;
            isClosed = true;
        }
    }
}
