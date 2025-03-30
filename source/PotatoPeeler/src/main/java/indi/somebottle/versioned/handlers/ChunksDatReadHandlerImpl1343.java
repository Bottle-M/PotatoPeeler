package indi.somebottle.versioned.handlers;

import indi.somebottle.constants.NBTTagConstants;
import indi.somebottle.exceptions.NBTFormatException;
import indi.somebottle.indexing.ChunksSpatialIndex;
import indi.somebottle.utils.IOUtils;
import indi.somebottle.utils.NumUtils;
import indi.somebottle.entities.ForcedChunksLoadResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ChunksDatReadHandlerImpl1343 implements ChunksDatReadHandler {
    byte[] chunksDatBytes;
    File chunksDatFile;

    /**
     * 构造函数（1.21.4 及之前的实现）
     *
     * @param chunksDatBytes chunks.dat 文件的字节数据
     * @param chunksDatFile  chunks.dat 文件的 File 对象
     */
    public ChunksDatReadHandlerImpl1343(byte[] chunksDatBytes, File chunksDatFile) {
        // chunks.dat 文件的字节数据
        this.chunksDatBytes = chunksDatBytes;
        // chunks.dat 文件的 File 对象
        this.chunksDatFile = chunksDatFile;
    }

    /**
     * 把 chunks.dat 文件中的强制加载区块存入受保护区块索引（1.21.4 及之前的实现）
     *
     * @param protectedChunksIndex 受保护区块索引
     * @return 返回更新后的索引以及新增的区块数量
     * @throws IOException        如果读取失败会抛出此异常
     * @throws NBTFormatException 当 NBT 标签数据有误，无法读取到一些字段时抛出
     */
    @Override
    public ForcedChunksLoadResult loadForcedIntoSpatialIndex(ChunksSpatialIndex protectedChunksIndex) throws IOException {
        long chunksCount = 0;
        InputStream bais = new ByteArrayInputStream(chunksDatBytes);
        if (IOUtils.findAndSkipBytes(bais, NBTTagConstants.FORCED_TAG_BIN)) {
            byte[] numBuf = new byte[8];
            // 找到了 Forced 标签，先读取其后 4 个字节，这是 Long 数组长度
            if (bais.read(numBuf, 0, 4) != 4) {
                // 读取失败
                throw new NBTFormatException("Forced tag was found, but unable to read array size in file:" + chunksDatFile.getAbsolutePath());
            }
            long arrSize = NumUtils.bigEndianToLong(numBuf, 4);
            for (int i = 0; i < arrSize; i++) {
                // 读取数组中的每个元素
                if (bais.read(numBuf) != 8) {
                    // 读取失败
                    throw new NBTFormatException("Forced tag was found, but unable to read array element [" + i + "] in file:" + chunksDatFile.getAbsolutePath());
                }
                long chunkPos = NumUtils.bigEndianToLong(numBuf, 8);
                // 强制加载区块文件中，每个 Long 元素的低 4 字节是 x 坐标，高 4 字节是 z 坐标
                int x = (int) (chunkPos & 0xFFFFFFFFL);
                int z = (int) (chunkPos >> 32 & 0xFFFFFFFFL);
                // 把区块坐标点加入到索引中
                protectedChunksIndex = protectedChunksIndex.add(x, z);
                chunksCount++;
            }
        }
        return new ForcedChunksLoadResult(protectedChunksIndex, chunksCount);
    }
}
