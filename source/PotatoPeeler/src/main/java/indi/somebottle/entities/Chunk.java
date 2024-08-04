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
     * 此区块在相应区域内的局部坐标 x，从 0 至 31
     */
    private final int x;
    /**
     * 此区块在相应区域内的局部坐标 z，从 0 至 31
     */
    private final int z;
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

    public Chunk(int x, int z, long offsetInFile, int sectorsOccupiedInFile, long inhabitedTime, boolean overSized) {
        this.x = x;
        this.z = z;
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
     * 获得局部 x 坐标
     *
     * @return 局部 x 坐标
     */
    public int getX() {
        return x;
    }

    /**
     * 获得局部 z 坐标
     *
     * @return 局部 z 坐标
     */
    public int getZ() {
        return z;
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
                ", x=" + x +
                ", z=" + z +
                ", inhabitedTime=" + inhabitedTime +
                ", overSized=" + overSized +
                '}';
    }
}
