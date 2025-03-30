package indi.somebottle.entities;

import indi.somebottle.indexing.ChunksSpatialIndex;

/**
 * 受保护区块加载结果
 */
public class ForcedChunksLoadResult {
    private final ChunksSpatialIndex chunksSpatialIndex;
    private final long chunksCount;

    /**
     * 构造受保护区块加载结果对象
     *
     * @param chunksSpatialIndex 受保护区块空间索引
     * @param chunksCount        新增的区块数量
     */
    public ForcedChunksLoadResult(ChunksSpatialIndex chunksSpatialIndex, long chunksCount) {
        this.chunksSpatialIndex = chunksSpatialIndex;
        this.chunksCount = chunksCount;
    }

    /**
     * 获取受保护区块空间索引
     *
     * @return 受保护区块空间索引
     */
    public ChunksSpatialIndex getChunksSpatialIndex() {
        return chunksSpatialIndex;
    }

    /**
     * 获取索引中新增的区块数量
     *
     * @return 区块数量
     */
    public long getChunksCount() {
        return chunksCount;
    }
}
