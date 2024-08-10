package indi.somebottle.entities;

import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometry;

/**
 * 任务参数 <br>
 * - minInhabited InhabitedTime 阈值 <br>
 * - mcaModifiableDelay mca 文件创建后多久能删除（分钟） <br>
 * - protectedChunksTree 所有受保护区块区域的索引 R* 树
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
     * 所有受保护区块区域的索引 R* 树
     */
    public RTree<Boolean, Geometry> protectedChunksTree;

    /**
     * 构造任务参数
     *
     * @param minInhabited        InhabitedTime 阈值
     * @param mcaModifiableDelay  mca 文件创建后多久能删除（分钟）
     * @param protectedChunksTree 所有受保护区块区域的索引 R* 树
     */
    public TaskParams(long minInhabited, long mcaModifiableDelay, RTree<Boolean, Geometry> protectedChunksTree) {
        this.minInhabited = minInhabited;
        this.mcaModifiableDelay = mcaModifiableDelay;
        this.protectedChunksTree = protectedChunksTree;
    }
}
