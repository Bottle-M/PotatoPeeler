package indi.somebottle.entities;

import indi.somebottle.indexing.ChunksSpatialIndex;

/**
 * 任务参数 <br>
 * - minInhabited InhabitedTime 阈值 <br>
 * - protectedChunksTree 所有受保护区块的区块空间索引
 */
public class TaskParams {
    /**
     * InhabitedTime 阈值
     */
    public long minInhabited;
    /**
     * 所有受保护区块区域的区块空间索引
     */
    public ChunksSpatialIndex protectedChunksIndex;

    /**
     * 构造任务参数
     *
     * @param minInhabited         InhabitedTime 阈值
     * @param protectedChunksIndex 区块空间索引对象
     */
    public TaskParams(long minInhabited, ChunksSpatialIndex protectedChunksIndex) {
        this.minInhabited = minInhabited;
        this.protectedChunksIndex = protectedChunksIndex;
    }
}
