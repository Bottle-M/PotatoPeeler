package indi.somebottle.utils;

// 区块 Chunk 相关的工具方法

import indi.somebottle.entities.Chunk;

import java.io.File;
import java.io.FileNotFoundException;
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
    public static Chunk read(File regionFile, long offsetInFile, int sectorsOccupiedInFile) throws IOException {
        try (RandomAccessFile chunkReader = new RandomAccessFile(regionFile, "r")) {
            // 先把指针移动到区块数据起始处
            chunkReader.seek(offsetInFile);
            // 获得区块数据真实长度
        }
        return null;
    }
}
