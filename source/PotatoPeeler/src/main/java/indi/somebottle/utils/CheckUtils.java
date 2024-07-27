package indi.somebottle.utils;

// 此类用于承载一些用于检查的方法
public class CheckUtils {
    /**
     * 检查字串是否能解析成整数
     *
     * @param str 字串
     * @return 是否是整数
     */
    public static boolean isInt(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
