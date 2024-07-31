package indi.somebottle.utils;

// 区块 Chunk 相关的工具方法

import indi.somebottle.entities.Chunk;
import indi.somebottle.exceptions.RegionFormatException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ChunkUtils {
    /**
     * 从 .mca 文件中根据偏移读出特定的区块
     *
     * @param regionFile            mca 文件 File 对象
     * @param offsetInFile          区块数据起始位置距离 Region 文件开头的偏移
     * @param sectorsOccupiedInFile 区块占用的扇区数
     * @return Chunk 对象
     */
    public static Chunk read(File regionFile, long offsetInFile, int sectorsOccupiedInFile) throws IOException, RegionFormatException {
        try (RandomAccessFile chunkReader = new RandomAccessFile(regionFile, "r")) {
            // 开一个 4 KiB × sectorsOccupiedInFile 的缓冲区
            // 最多会读取这么多数据
            byte[] buffer = new byte[4096 * sectorsOccupiedInFile];
            int byteRead = 0;
            // 先把指针移动到区块数据起始处
            chunkReader.seek(offsetInFile);
            // 获得区块数据真实长度
            // 区块数据前 4 个字节是大端存储的
            byteRead = chunkReader.read(buffer, 0, 4);
            if (byteRead < 4) {
                // 读取失败
                throw new RegionFormatException("Chunk data error in " + regionFile.getName() + ": unable to read chunk data length.");
            }
            // 按大端方式转换为整数，因为有 4 个字节，而 Java 没有无符号数，这里需要用 long 进行存储
            long chunkDataLen = (long) (buffer[0] & 0xFF) << 24 | (long) (buffer[1] & 0xFF) << 16 | (long) (buffer[2] & 0xFF) << 8 | (long) (buffer[3] & 0xFF);
            // 继续读取 1 个字节，这个字节是压缩格式
            int compressionType = chunkReader.read();
            if (compressionType < 0) {
                // 读取失败
                throw new RegionFormatException("Chunk data error in " + regionFile.getName() + ": unable to read compression type.");
            }
            // 接下来把区块数据读入缓冲区

        }
        return null;
    }
}
