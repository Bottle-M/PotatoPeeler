package indi.somebottle.entities;

/**
 * 存储一个整数的取值范围
 */
public class IntRange {
    public int from;
    public int to;

    /**
     * 构建一个整数的离散取值闭区间 [from, to]
     *
     * @param from 起始值
     * @param to   终止值
     */
    public IntRange(int from, int to) {
        this.from = from;
        this.to = to;
    }

    /**
     * 初始化一个空的 IntRange [0, 0]
     */
    public IntRange() {
        this.from = 0;
        this.to = 0;
    }
}
