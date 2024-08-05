package indi.somebottle.entities;

public class PeelResult {
    // 共腾出了多少字节的空间
    private long sizeReduced;
    // 清理掉多少个区块
    private long chunksRemoved;
    // 受影响的区域数
    private long regionsAffected;
    // 耗时（单位：ms）
    private long timeElapsed;

    public PeelResult() {
        sizeReduced = 0;
        chunksRemoved = 0;
        regionsAffected = 0;
        timeElapsed = 0;
    }

    /**
     * 清理区块后的结果
     *
     * @param sizeReduced     减小的尺寸（Bytes）
     * @param chunksRemoved   移除的区块数目
     * @param regionsAffected 受影响的区域数目
     * @param timeElapsed     耗时
     */
    public PeelResult(long sizeReduced, long chunksRemoved, long regionsAffected, long timeElapsed) {
        this.sizeReduced = sizeReduced;
        this.chunksRemoved = chunksRemoved;
        this.regionsAffected = regionsAffected;
        this.timeElapsed = timeElapsed;
    }

    public long getSizeReduced() {
        return sizeReduced;
    }

    public void setSizeReduced(long sizeReduced) {
        this.sizeReduced = sizeReduced;
    }

    public long getChunksRemoved() {
        return chunksRemoved;
    }

    public void setChunksRemoved(long chunksRemoved) {
        this.chunksRemoved = chunksRemoved;
    }

    public long getRegionsAffected() {
        return regionsAffected;
    }

    public void setRegionsAffected(long regionsAffected) {
        this.regionsAffected = regionsAffected;
    }

    /**
     * 获取耗时（单位：ms）
     *
     * @return 耗时
     */
    public long getTimeElapsed() {
        return timeElapsed;
    }

    /**
     * 设置耗时（单位：ms）
     *
     * @param timeElapsed 耗时
     */
    public void setTimeElapsed(long timeElapsed) {
        this.timeElapsed = timeElapsed;
    }

    /**
     * 把另一个 PeelResult 的结果加到本对象中
     *
     * @param another 另一个 PeelResult 对象
     */
    public void add(PeelResult another) {
        this.sizeReduced += another.sizeReduced;
        this.chunksRemoved += another.chunksRemoved;
        this.regionsAffected += another.regionsAffected;
        this.timeElapsed += another.timeElapsed;
    }
}
