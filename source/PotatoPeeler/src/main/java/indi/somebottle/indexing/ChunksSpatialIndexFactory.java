package indi.somebottle.indexing;

/**
 * 用于获取区块空间索引对象的静态工厂类
 */
public class ChunksSpatialIndexFactory {
    /**
     * 获取一个基于 R* 树的区块空间索引对象
     *
     * @return 基于 R* 树的区块空间索引对象
     */
    public static ChunksSpatialIndex createRStarTreeIndex() {
        return new ChunksRStarTreeIndex();
    }
}
