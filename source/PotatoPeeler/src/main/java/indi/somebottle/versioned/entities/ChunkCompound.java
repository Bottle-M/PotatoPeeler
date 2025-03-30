package indi.somebottle.versioned.entities;

/**
 * chunks.dat 文件中 tickets 列表中的区块 Compound 标签
 */
public class ChunkCompound {
    /**
     * 此区块在整个世界中的 x 坐标
     */
    private final int globalX;

    /**
     * 此区块在整个世界中的 z 坐标
     */
    private final int globalZ;

    /**
     * 此区块标签的类型
     */
    private final String type;

    /**
     * 构造区块 Compound 标签对象
     *
     * @param globalX 区块在整个世界中的 x 坐标
     * @param globalZ 区块在整个世界中的 z 坐标
     * @param type    区块标签的类型
     */
    public ChunkCompound(int globalX, int globalZ, String type) {
        this.globalX = globalX;
        this.globalZ = globalZ;
        this.type = type;
    }

    /**
     * 获取区块在整个世界中的 x 坐标
     *
     * @return 区块在整个世界中的 x 坐标
     */
    public int getGlobalX() {
        return globalX;
    }

    /**
     * 获取区块在整个世界中的 z 坐标
     *
     * @return 区块在整个世界中的 z 坐标
     */
    public int getGlobalZ() {
        return globalZ;
    }

    /**
     * 获取区块标签的类型
     *
     * @return 区块标签的类型
     */
    public String getType() {
        return type;
    }


}
