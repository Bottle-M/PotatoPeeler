package indi.somebottle.utils;

import indi.somebottle.exceptions.PeelerArgIncompleteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArgsHandler {
    // 定义 PotatoPeeler 能用到的参数 <参数名, 是否要指定值>
    public final static HashMap<String, Boolean> PEELER_ARGS = new HashMap<String, Boolean>();

    // 初始化参数
    static {
        PEELER_ARGS.put("--server-jar", true);
        PEELER_ARGS.put("--min-inhabited", true);
        PEELER_ARGS.put("--mca-deletable-delay", true);
    }

    /**
     * 提取出命令行参数中和 PotatoPeeler 相关的参数，返回除掉该参数的剩余参数
     *
     * @param args       命令行参数
     * @param peelerArgs PotatoPeeler 相关参数 Map，此 map 会被该函数修改。
     * @return 除掉 Peeler 相关参数的剩余参数
     * @throws PeelerArgIncompleteException 命令行参数不完整
     */
    public static List<String> stripPeelerArgs(String[] args, HashMap<String, String> peelerArgs) throws PeelerArgIncompleteException {
        List<String> remainingArgs = new ArrayList<>();
        // 清空残余参数
        peelerArgs.clear();
        int i = 0;
        while (i < args.length) {
            if (args[i].startsWith("-") && PEELER_ARGS.containsKey(args[i])) {
                // 首先要是参数或者选项
                if (!PEELER_ARGS.get(args[i])) {
                    // 如果这个是不需要指定值的，是选项
                    peelerArgs.put(args[i], "");
                    i++;
                } else {
                    // 如果需要指定值，则是参数
                    if (i + 1 >= args.length || args[i + 1].startsWith("-")) {
                        // 下一个元素不是值，说明参数不完整
                        throw new PeelerArgIncompleteException("PotatoPeeler parameter " + args[i] + " is incomplete.");
                    }
                    // 如果下一个元素是值，说明参数完整
                    peelerArgs.put(args[i], args[i + 1]);
                    i += 2;
                }
            } else {
                // 否则是和 PotatoPeeler 无关的参数，暂且保留
                remainingArgs.add(args[i]);
                i++;
            }
        }
        return remainingArgs;
    }
}
