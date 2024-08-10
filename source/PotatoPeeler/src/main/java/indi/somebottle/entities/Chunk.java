package indi.somebottle.entities;

public class Chunk {
    /**
     * 此区块数据在原文件中距离起始的字节数
     */
    private final long offsetInFile;
    /**
     * 这个区块在原文件中占用了多少扇区（4 KiB）
     */
    private final int sectorsOccupiedInFile;
    /**
     * 此区块在整个世界中的 x 坐标
     */
    private final int globalX;
    /**
     * 此区块在整个世界中的 z 坐标
     */
    private final int globalZ;
    /**
     * 此区块的 InhabitedTime （Tick）
     */
    private final long inhabitedTime;
    /**
     * 此区块是否超出了 255 个扇区（1020 KiB）的大小
     * <p>
     * 这种情况下区块会在另外的 c.<区块X坐标>.<区块Z坐标>.mcc 进行存储，本程序会忽略处理这么大的区块
     */
    private final boolean overSized;

    /**
     * 标记是否删除这个区块
     */
    private boolean deleteFlag = false;

    /**
     * 构造区块对象
     *
     * @param globalX               区块在整个世界中的 x 坐标
     * @param globalZ               区块在整个世界中的 z 坐标
     * @param offsetInFile          区块数据在原文件中距离起始的字节数
     * @param sectorsOccupiedInFile 这个区块在原文件中占用了多少扇区（4 KiB）
     * @param inhabitedTime         InhabitedTime （Tick）
     * @param overSized             是否超出了 255 个扇区（1020 KiB）的大小
     */
    public Chunk(int globalX, int globalZ, long offsetInFile, int sectorsOccupiedInFile, long inhabitedTime, boolean overSized) {
        this.globalX = globalX;
        this.globalZ = globalZ;
        this.offsetInFile = offsetInFile;
        this.sectorsOccupiedInFile = sectorsOccupiedInFile;
        this.inhabitedTime = inhabitedTime;
        this.overSized = overSized;
    }

    /**
     * 获得删除标记
     *
     * @return 是否删除这个区块
     */
    public boolean isDeleteFlag() {
        return deleteFlag;
    }

    /**
     * 设置删除标记
     *
     * @param deleteFlag 是否删除这个区块
     */
    public void setDeleteFlag(boolean deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    /**
     * 是否超出了 255 个扇区（1020 KiB）的大小
     *
     * @return 是否超出了 255 个扇区（1020 KiB）的大小
     */
    public boolean isOverSized() {
        return overSized;
    }

    /**
     * 获得此区块在原文件中距离起始的字节数
     *
     * @return 此区块在原文件中距离起始的字节数
     */
    public long getOffsetInFile() {
        return offsetInFile;
    }

    /**
     * 获得此区块在原文件中占用了多少扇区（4 KiB）
     *
     * @return 此区块在原文件中占用了多少扇区（4 KiB）
     */
    public int getSectorsOccupiedInFile() {
        return sectorsOccupiedInFile;
    }

    /**
     * 获得区块在整个世界中的全局坐标
     *
     * @return 全局 x 坐标（32 bit 有符号整数）
     */
    public int getGlobalX() {
        return globalX;
    }

    /**
     * 获得区块在整个世界中的全局坐标
     *
     * @return 全局 z 坐标（32 bit 有符号整数）
     */
    public int getGlobalZ() {
        return globalZ;
    }

    /**
     * 获得 InhabitedTime
     *
     * @return InhabitedTime
     */
    public long getInhabitedTime() {
        return inhabitedTime;
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "offsetInFile=" + offsetInFile +
                ", sectorsOccupiedInFile=" + sectorsOccupiedInFile +
                ", x=" + globalX +
                ", z=" + globalZ +
                ", inhabitedTime=" + inhabitedTime +
                ", overSized=" + overSized +
                '}';
    }
}
