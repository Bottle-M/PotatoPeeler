package indi.somebottle.entities;

import indi.somebottle.indexing.ChunksSpatialIndex;

import java.nio.file.Path;

/**
 * 任务参数 <br>
 * - minInhabited InhabitedTime 阈值 <br>
 * - protectedChunksTree 所有受保护区块的区块空间索引 <br>
 * - dryRun 试运行选项
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
     * 世界目录的绝对路径
     */
    public Path absWorldDirPath;

    /**
     * 输出的世界目录的绝对路径，如果为 null 则未指定
     */
    public Path absOutputDirPath;

    /**
     * 试运行选项
     */
    public boolean dryRun;

    /**
     * 构造任务参数
     *
     * @param minInhabited         InhabitedTime 阈值
     * @param protectedChunksIndex 区块空间索引对象
     * @param dryRun               试运行选项
     * @param worldDirPath         世界目录的路径
     * @param outputDirPath        输出的世界目录的路径, 为 null 则不指定
     */
    public TaskParams(long minInhabited, ChunksSpatialIndex protectedChunksIndex, boolean dryRun, Path worldDirPath, Path outputDirPath) {
        this.minInhabited = minInhabited;
        this.protectedChunksIndex = protectedChunksIndex;
        this.dryRun = dryRun;
        // 路径全部转换为绝对路径方便处理
        this.absWorldDirPath = worldDirPath.toAbsolutePath();
        this.absOutputDirPath = outputDirPath != null ? outputDirPath.toAbsolutePath() : null;
    }
}
