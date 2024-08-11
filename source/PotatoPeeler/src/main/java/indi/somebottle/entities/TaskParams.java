package indi.somebottle.entities;

import indi.somebottle.indexing.ChunksSpatialIndex;

/**
 * 任务参数 <br>
 * - minInhabited InhabitedTime 阈值 <br>
 * - mcaModifiableDelay mca 文件创建后多久能删除（分钟） <br>
 * - protectedChunksTree 所有受保护区块的区块空间索引
 */
public class TaskParams {
    /**
     * InhabitedTime 阈值
     */
    public long minInhabited;
    /**
     * mca 文件创建后多久能删除（分钟）
     */
    public long mcaModifiableDelay;
    /**
     * 所有受保护区块区域的区块空间索引
     */
    public ChunksSpatialIndex protectedChunksIndex;

    /**
     * 构造任务参数
     *
     * @param minInhabited         InhabitedTime 阈值
     * @param mcaModifiableDelay   mca 文件创建后多久能删除（分钟）
     * @param protectedChunksIndex 区块空间索引对象
     */
    public TaskParams(long minInhabited, long mcaModifiableDelay, ChunksSpatialIndex protectedChunksIndex) {
        this.minInhabited = minInhabited;
        this.mcaModifiableDelay = mcaModifiableDelay;
        this.protectedChunksIndex = protectedChunksIndex;
    }
}
