package indi.somebottle.versioned.handlers;

import indi.somebottle.exceptions.NBTFormatException;
import indi.somebottle.indexing.ChunksSpatialIndex;
import indi.somebottle.entities.ForcedChunksLoadResult;

import java.io.IOException;

/**
 * 读取 chunks.dat 的 handler 的接口
 */
public interface ChunksDatReadHandler {
    /**
     * 把 chunks.dat 文件中的强制加载区块存入受保护区块索引
     *
     * @param protectedChunksIndex 受保护区块索引
     * @return 返回更新后的索引以及新增的区块数量
     * @throws IOException        如果读取失败会抛出此异常
     * @throws NBTFormatException 当 NBT 标签数据有误，无法读取到一些字段时抛出
     */
    ForcedChunksLoadResult loadForcedIntoSpatialIndex(ChunksSpatialIndex protectedChunksIndex) throws IOException;
}
