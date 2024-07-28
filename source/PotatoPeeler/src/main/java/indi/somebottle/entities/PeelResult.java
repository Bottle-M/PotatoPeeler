package indi.somebottle.entities;

public class PeelResult {
    // 共腾出了多少字节的空间
    private long sizeReduced;
    // 清理掉多少个区块
    private long chunksRemoved;
    // 受影响的区域数
    private long regionsAffected;
    // 运行时间（毫秒）
    private long timeElapsed;

    public PeelResult() {
        sizeReduced = 0;
        chunksRemoved = 0;
        regionsAffected = 0;
    }

    /**
     * 清理区块后的结果
     *
     * @param sizeReduced     减小的尺寸（Bytes）
     * @param chunksRemoved   移除的区块数目
     * @param regionsAffected 受影响的区域数目
     */
    public PeelResult(long sizeReduced, long chunksRemoved, long regionsAffected, long timeElapsed) {
        this.sizeReduced = sizeReduced;
        this.chunksRemoved = chunksRemoved;
        this.regionsAffected = regionsAffected;
        this.timeElapsed = timeElapsed;
    }

    public long getTimeElapsed() {
        return timeElapsed;
    }

    public void setTimeElapsed(long timeElapsed) {
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
}
