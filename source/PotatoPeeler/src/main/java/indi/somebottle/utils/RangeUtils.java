package indi.somebottle.utils;

import indi.somebottle.entities.IntRange;

import java.io.IOException;

/**
 * 此类是用于处理 CoordsRange 等区间的工具类
 */
public class RangeUtils {
    /**
     * 内部方法，解析得到受保护区块清单每行中的单个 IntRange <br>
     *
     * @param rangeStr 字符串形式的范围，形如 "x1-x2"，其中 x2 可选，x1 和 x2 都可以是 '*'
     * @return IntRange 对象
     * @throws IOException           格式错误时抛出
     * @throws NumberFormatException 当配置的数字无法解析时抛出此异常
     * @apiNote 此方法保证返回的 to > from
     */
    public static IntRange parseSingleIntRange(String rangeStr) throws IOException {
        String[] parts = rangeStr.split("-");
        if (parts.length > 2) {
            // 格式错误
            throw new IOException("Invalid coordinate range: " + rangeStr);
        }
        // 去除多余空格
        parts[0] = parts[0].trim();
        if (parts[0].isEmpty()) {
            // range from 不可为空
            throw new IOException("Invalid coordinate range 'from': " + rangeStr);
        }
        IntRange intRange = new IntRange();
        if (parts[0].equals("*")) {
            // from 如果是 *，先指定全范围
            intRange.from = Integer.MIN_VALUE;
            intRange.to = Integer.MAX_VALUE;
        } else {
            // 否则先指定为 parts[0]
            intRange.from = Integer.parseInt(parts[0]);
            intRange.to = intRange.from;
        }
        if (parts.length == 2) {
            // 如果有 to，那么不应该为空
            parts[1] = parts[1].trim();
            if (parts[1].isEmpty()) {
                throw new IOException("Invalid coordinate range 'to': " + rangeStr);
            }
            // 如果有 parts[1]，那么指定为 to
            if (parts[1].equals("*")) {
                // 如果 to 是 * 则指定为最大值
                intRange.to = Integer.MAX_VALUE;
            } else {
                intRange.to = Integer.parseInt(parts[1]);
            }
        }
        // 需要保证 to > from
        if (intRange.to < intRange.from) {
            int temp = intRange.from;
            intRange.from = intRange.to;
            intRange.to = temp;
        }
        return intRange;
    }
}
