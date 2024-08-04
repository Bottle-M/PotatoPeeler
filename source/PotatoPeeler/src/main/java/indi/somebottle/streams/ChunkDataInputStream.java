package indi.somebottle.streams;

// 用于解压区块数据的接口

import java.io.*;

import indi.somebottle.exceptions.CompressionTypeUnsupportedException;

/**
 * 区块数据读取类
 *
 * @apiNote 数据读取完毕后 RandomAccessFile 的指针会后移
 */
public class ChunkDataInputStream extends InputStream {

    /**
     * 解压缩流实例
     */
    private InputStream inflatedStream;

    /**
     * 流是否已经关闭
     */
    private boolean isClosed = false;


    /**
     * 构造区块数据阅读器
     *
     * @param chunkReader     读取 Chunk 数据的 RAF 对象
     * @param dataLen         可读的区块数据总长度
     * @param compressionType 压缩类型
     * @throws IOException                         IO 异常
     * @throws CompressionTypeUnsupportedException 压缩类型不支持
     */
    public ChunkDataInputStream(RandomAccessFile chunkReader, int dataLen, int compressionType) throws IOException {
        // 先把 RAF 包装成流，然后再用 InputStreamFactory 根据压缩类型获取解压流
        this.inflatedStream = DecompressedInputStreamFactory.getStream(compressionType, new RandomAccessInputStream(chunkReader, dataLen));
    }

    /**
     * 通过解压流读取区块数据中的一个字节
     *
     * @return 字节 int，若无更多数据则返回 -1
     * @throws IOException IO 异常
     */
    @Override
    public int read() throws IOException {
        if (isClosed)
            return -1;
        return inflatedStream.read();
    }

    @Override
    public void close() throws IOException {
        if (!isClosed) {
            isClosed = true;
            // 关闭流
            inflatedStream.close();
            inflatedStream = null;
        }
    }

}
