package indi.somebottle.streams;

// 用于解压区块数据的接口

import java.io.*;

/**
 * 区块数据读取类
 *
 * @apiNote 数据读取完毕后 RandomAccessFile 的指针会后移
 */
public class ChunkDataInputStream extends InputStream {


    /**
     * 指向区块数据起始字节（而不是压缩方式那一个字节）的 RandomAccessFile 对象
     */
    protected final RandomAccessFile chunkReader;

    /**
     * 压缩类型
     */
    protected final int compressionType;

    /**
     * 该区块数据剩余可读的字节数
     */
    protected int remainingDataLen;

    /**
     * 文件读取缓冲区
     */
    protected final byte[] readBuf = new byte[4096];

    /**
     * 文件读取缓冲区内现有的字节数
     */
    protected int bufLen = 0;

    /**
     * 解压缩流实例
     */
    protected InputStream inputStream = null;


    /**
     * 构造区块数据阅读器
     *
     * @param chunkReader     读取 Chunk 数据的 RAF 对象
     * @param dataLen         可读的区块数据总长度
     * @param compressionType 压缩类型
     */
    public ChunkDataInputStream(RandomAccessFile chunkReader, int dataLen, int compressionType) {
        this.chunkReader = chunkReader;
        this.remainingDataLen = dataLen;
        this.compressionType = compressionType;
    }

    /**
     * 内部方法，把 buffer 现有数据转换为 ByteArrayInputStream，并把 bufLen 置 0 （表示缓冲区数据全部读出）
     *
     * @return ByteArrayInputStream 实例
     */
    protected ByteArrayInputStream getBufferByteStream() {
        ByteArrayInputStream res = new ByteArrayInputStream(readBuf, 0, bufLen);
        bufLen = 0;
        return res;
    }

    /**
     * 内部方法，返回 buffer 是否为空（没有更多文件数据了）
     *
     * @return buffer 是否为空
     */
    protected boolean isBufferEmpty() {
        return bufLen == 0;
    }

    /**
     * 内部方法，继续读取区块数据，存入缓冲区
     *
     * @return 读取的字节数，如果是 -1，代表没有更多字节可读
     */
    protected int readRawDataToBuffer() throws IOException {
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
     * 通过解压流读取区块数据中的一个字节
     *
     * @return 字节 int，若无更多数据则返回 -1
     * @throws IOException IO 异常
     */
    @Override
    public int read() throws IOException {
        if (inputStream == null) {
            // 如果没有 lz4 流，先检查缓冲区有没有数据
            if (isBufferEmpty()) {
                // 缓冲区内没有更多数据可读了
                // 检查能不能从文件中读入更多数据
                if (readRawDataToBuffer() == -1) {
                    // 数据也读完了则返回 -1
                    return -1;
                }
            }
            // 把缓冲区现有数据读入
            inputStream = InputStreamFactory.getStream(compressionType, getBufferByteStream());
        }
        int readByte = inputStream.read();
        if (readByte == -1) {
            // 如果读完了，关闭 lz4 流，重试一次 read
            inputStream.close();
            inputStream = null;
            return read();
        }
        return readByte;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
    }

}
