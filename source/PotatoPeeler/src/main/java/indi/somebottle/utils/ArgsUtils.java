package indi.somebottle.utils;

import indi.somebottle.exceptions.PeelerArgIncompleteException;
import indi.somebottle.logger.GlobalLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArgsUtils {
    /**
     * 定义 PotatoPeeler 能用到的参数 <参数名, 是否要指定值>
     */
    public final static HashMap<String, Boolean> PEELER_ARGS = new HashMap<>();

    // 初始化参数
    static {
        // Minecraft 服务器世界目录路径，可以有多个（逗号分隔）
        PEELER_ARGS.put("--world-dirs", true);
        // Minecraft 服务端 jar 包路径
        PEELER_ARGS.put("--server-jar", true);
        // InhabitedTime 阈值，低于此值的区块会被移除，单位：tick
        PEELER_ARGS.put("--min-inhabited", true);
        // 处理间隔时间，单位：分钟
        PEELER_ARGS.put("--cool-down", true);
        // 处理时采用的线程数
        PEELER_ARGS.put("--threads-num", true);
        // 每个日志文件的最大大小(字节)
        PEELER_ARGS.put("--max-log-size", true);
        // 日志文件的最大数量
        PEELER_ARGS.put("--retain-log-files", true);
        // 是否详细输出区块处理情况
        PEELER_ARGS.put("--verbose", false);
        // 是否跳过本次处理
        PEELER_ARGS.put("--skip-peeler", false);
        // 让程序打印使用信息
        PEELER_ARGS.put("--help", false);
        // 试运行选项
        PEELER_ARGS.put("--dry-run", false);
    }

    /**
     * 从文件中读取命令行参数
     *
     * @param filePath 文件路径
     * @return 参数数组
     * @throws IOException 文件读取失败时抛出；文件没有提供任何参数时也会抛出
     */
    public static String[] readArgsFromFile(String filePath) throws IOException {
        Path argFilePath = Paths.get(filePath);
        byte[] allBytes = Files.readAllBytes(argFilePath);
        String[] argList = new String(allBytes).split("\\s+");
        // 去除头部的空字串
        List<String> trimmedArgList = new ArrayList<>();
        for (String arg : argList) {
            if (arg.isEmpty())
                continue;
            trimmedArgList.add(arg);
        }
        // 文件中没有提供任何有效参数
        if (trimmedArgList.isEmpty())
            throw new IOException("No arguments provided in " + filePath);
        return trimmedArgList.toArray(new String[0]);
    }

    /**
     * 按逗号分隔 worldDirs 参数，得到多个路径
     *
     * @param worldDirs 逗号分隔的世界目录路径
     * @return 多个世界目录路径 List<String>
     */
    public static List<String> parseWorldDirs(String worldDirs) {
        List<String> worldDirList = new ArrayList<>();
        String[] dirs = worldDirs.split(",");
        for (String dir : dirs) {
            if (dir.trim().isEmpty()) {
                // 空字符串不计入
                continue;
            }
            worldDirList.add(dir);
        }
        return worldDirList;
    }

    /**
     * 检查传递给 PotatoPeeler 的参数是否合法
     *
     * @param peelerArgs 传递给 PotatoPeeler 的参数
     * @return 是否合法
     */
    public static boolean checkPeelerArgs(HashMap<String, String> peelerArgs) {
        // 需要接收数字的参数无法被解析成数字则参数无效
        if (!CheckUtils.isInt(peelerArgs.get("--min-inhabited"))) {
            GlobalLogger.warning("PotatoPeeler parameter --min-inhabited must be an integer.");
            return false;
        }
        if (Long.parseLong(peelerArgs.get("--min-inhabited")) < 0) {
            // 不能小于 0
            GlobalLogger.warning("PotatoPeeler parameter --min-inhabited must be >= 0.");
            return false;
        }
        if (!CheckUtils.isInt(peelerArgs.get("--cool-down"))) {
            GlobalLogger.warning("PotatoPeeler parameter --cool-down must be an integer.");
            return false;
        }
        if (Long.parseLong(peelerArgs.get("--cool-down")) < 0) {
            // 不能小于 0
            GlobalLogger.warning("PotatoPeeler parameter --cool-down must be >= 0.");
            return false;
        }
        if (!CheckUtils.isInt(peelerArgs.get("--threads-num"))) {
            GlobalLogger.warning("PotatoPeeler parameter --threads-num must be an integer.");
            return false;
        }
        if (Long.parseLong(peelerArgs.get("--threads-num")) < 1) {
            // 不能小于 1
            GlobalLogger.warning("PotatoPeeler parameter --threads-num must be >= 1.");
            return false;
        }
        if (!CheckUtils.isInt(peelerArgs.get("--max-log-size"))) {
            GlobalLogger.warning("PotatoPeeler parameter --max-log-size must be an integer.");
            return false;
        }
        if (Long.parseLong(peelerArgs.get("--max-log-size")) < 0) {
            // 不能小于 0
            GlobalLogger.warning("PotatoPeeler parameter --max-log-size must be >= 0.");
            return false;
        }
        if (!CheckUtils.isInt(peelerArgs.get("--retain-log-files"))) {
            GlobalLogger.warning("PotatoPeeler parameter --retain-log-files must be an integer.");
            return false;
        }
        if (Long.parseLong(peelerArgs.get("--retain-log-files")) < 1) {
            // 不能小于 1
            GlobalLogger.warning("PotatoPeeler parameter --retain-log-files must be >= 1.");
            return false;
        }
        return true;
    }

    /**
     * 把没有指定值的参数设置成默认的
     *
     * @param peelerArgs PotatoPeeler 参数 Map
     */
    public static void setDefaultPeelerArgs(HashMap<String, String> peelerArgs) {
        // 如果没有设定 minInhabited，默认为 0
        if (!peelerArgs.containsKey("--min-inhabited")) {
            peelerArgs.put("--min-inhabited", "0");
        }
        // 如果没有设定 cool-down，则默认为 0
        if (!peelerArgs.containsKey("--cool-down")) {
            peelerArgs.put("--cool-down", "0");
        }
        // 如果没有指定线程数，默认为 10
        if (!peelerArgs.containsKey("--threads-num")) {
            peelerArgs.put("--threads-num", "10");
        }
        // 如果没有指定世界路径，默认为空
        if (!peelerArgs.containsKey("--world-dirs")) {
            peelerArgs.put("--world-dirs", "");
        }
        // 如果没有指定保存日志文件的大小，默认为 2 MiB
        if (!peelerArgs.containsKey("--max-log-size")) {
            peelerArgs.put("--max-log-size", "2097152");
        }
        // 如果没有指定日志文件的最大数量，默认为 10
        if (!peelerArgs.containsKey("--retain-log-files")) {
            peelerArgs.put("--retain-log-files", "10");
        }
    }

    /**
     * 提取出命令行参数中和 PotatoPeeler 相关的参数，返回除掉该参数的剩余参数
     *
     * @param args       命令行参数
     * @param peelerArgs PotatoPeeler 相关参数 Map，此 map 会被该函数修改。
     * @return 除掉 Peeler 相关参数后的剩余参数
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
