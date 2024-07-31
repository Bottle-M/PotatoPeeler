package indi.somebottle.decompressors;

// 用于解压区块数据的接口

import java.io.RandomAccessFile;

public interface ChunkDataDecompressor {
    /**
     * 解压区块数据
     *
     * @param chunkReader 区块文件读取 RandomAccessFile
     * @param dataLen     数据长度（记得去掉“压缩方式”占用的一个字节）
     * @return 解压后的数据 byte[]
     */
    byte[] decompress(RandomAccessFile chunkReader, int dataLen);
}
