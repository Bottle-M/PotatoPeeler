package indi.somebottle.indexing;

/**
 * 为区块建立空间索引的接口 <br>
 * 实现时请确保：
 * - Immutable (对象不可变，线程安全)
 * - 链式调用
 */
public interface ChunksSpatialIndex {

    /**
     * 添加一个区块数据点到空间索引中
     *
     * @param chunkX 区块全局 X 坐标
     * @param chunkZ 区块全局 Z 坐标
     * @return 新的空间索引对象
     */
    ChunksSpatialIndex add(int chunkX, int chunkZ);

    /**
     * 添加一个矩形的区块区域到空间索引中 <br>
     * <b>需要保证 chunkX2 > chunkX1, chunkZ2 > chunkZ1</b>
     *
     * @param chunkX1 矩形区域的一个对角顶点 X 坐标
     * @param chunkZ1 矩形区域的一个对角顶点 Z 坐标
     * @param chunkX2 矩形区域的另一个对角顶点 X 坐标
     * @param chunkZ2 矩形区域的另一个对角顶点 Z 坐标
     * @return 新的空间索引对象
     */
    ChunksSpatialIndex add(int chunkX1, int chunkZ1, int chunkX2, int chunkZ2);

    /**
     * 空间索引中是否包含某个区块
     *
     * @param chunkX 区块全局 X 坐标
     * @param chunkZ 区块全局 Z 坐标
     * @return 是否包含
     */
    boolean contains(int chunkX, int chunkZ);
}
