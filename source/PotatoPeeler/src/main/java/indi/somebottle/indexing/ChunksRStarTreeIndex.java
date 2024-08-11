package indi.somebottle.indexing;

import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Geometry;

public class ChunksRStarTreeIndex implements ChunksSpatialIndex {
    private final RTree<Boolean, Geometry> rStarTree;

    /**
     * 构造基于 R* 树的空间索引对象（Immutable）<br>
     * R 树非常适合为空间中的范围查询建立索引（尤其是空间中存在形状不规则的矩形）
     */
    public ChunksRStarTreeIndex() {// R* 树
        // 默认配置下 maxChildren=4, minChildren=round(4*0.4)=2
        // 详见：https://github.com/davidmoten/rtree2/blob/45d209dff7d407632abfe7c67e2ff90f6ff24f03/src/main/java/com/github/davidmoten/rtree2/RTree.java#L348
        this.rStarTree = RTree.star().create();
    }

    /**
     * 基于现有树重新构造对象 <br>
     * rtree2 采用了共享结构（Structural Sharing）的思路，因此每次构造新对象时开销不算大
     *
     * @param tree R* 树
     */
    private ChunksRStarTreeIndex(RTree<Boolean, Geometry> tree) {
        this.rStarTree = tree;
    }

    @Override
    public ChunksSpatialIndex add(int chunkX, int chunkZ) {
        return new ChunksRStarTreeIndex(rStarTree.add(true, Geometries.point((double) chunkX, chunkZ)));
    }

    @Override
    public ChunksSpatialIndex add(int chunkX1, int chunkZ1, int chunkX2, int chunkZ2) {
        // R 树构建要求 chunkX2 > chunkX1 且 chunkZ2 > chunkZ1
        return new ChunksRStarTreeIndex(rStarTree.add(true, Geometries.rectangle((double) chunkX1, chunkZ1, chunkX2, chunkZ2)));
    }

    @Override
    public boolean contains(int chunkX, int chunkZ) {
        Iterable<Entry<Boolean, Geometry>> results = rStarTree.search(Geometries.point((double) chunkX, chunkZ));
        // 搜索到结果则说明 (x, z) 点和某些受保护区块有交集（本质上是一些矩形区域以及点）
        return results.iterator().hasNext();
    }
}
