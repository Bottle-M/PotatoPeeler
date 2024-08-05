package indi.somebottle;

import indi.somebottle.entities.PeelResult;
import indi.somebottle.exceptions.PeelerArgIncompleteException;
import indi.somebottle.exceptions.RegionFileNotFoundException;
import indi.somebottle.exceptions.RegionTaskInterruptedException;
import indi.somebottle.logger.GlobalLogger;
import indi.somebottle.utils.*;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        GlobalLogger.info("Potato Peeler starting...");
        // 获得 JVM 参数
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        // 初始化 PotatoPeeler 参数
        HashMap<String, String> peelerArgs = new HashMap<>();
        // 除掉 PotatoPeeler 相关的参数，剩下的参数
        List<String> remainingArgs = null;
        // 提取出 PotatoPeeler 相关的参数
        try {
            remainingArgs = ArgsUtils.stripPeelerArgs(args, peelerArgs);
        } catch (PeelerArgIncompleteException e) {
            // 说明命令行参数不完整，打印错误信息
            GlobalLogger.severe(e.getMessage());
            System.exit(1);
        }
        // 因为要设置日志记录级别，这里要先拿到 --verbose 选项
        boolean verboseOutput = peelerArgs.containsKey("--verbose");
        GlobalLogger.setVerbose(verboseOutput);
        // 先输出命令行参数
        GlobalLogger.fine("====== JVM ARGS ======");
        for (String arg : jvmArgs) {
            GlobalLogger.fine(arg + " ");
        }
        // 再输出 PotatoPeeler 相关的参数
        GlobalLogger.fine("====== POTATO-PEELER ARGS ======");
        for (String arg : peelerArgs.keySet()) {
            GlobalLogger.fine(arg + " " + peelerArgs.get(arg));
        }
        // 最后是其他参数
        GlobalLogger.fine("====== OTHER ARGS ======");
        for (String arg : remainingArgs) {
            GlobalLogger.fine(arg + " ");
        }
        // ========== 开始进行参数检查 ==========
        if (!ArgsUtils.checkPeelerArgs(peelerArgs)) {
            // 有参数不合法则退出
            System.exit(1);
        }
        // 给没有指定的参数标上默认值
        ArgsUtils.setDefaultPeelerArgs(peelerArgs);
        // 解析参数值
        List<String> worldDirPaths = ArgsUtils.parseWorldDirs(peelerArgs.get("--world-dirs"));
        long minInhabited = Long.parseLong(peelerArgs.get("--min-inhabited"));
        long coolDown = Long.parseLong(peelerArgs.get("--cool-down"));
        long mcaModifiableDelay = Long.parseLong(peelerArgs.get("--mca-modifiable-delay"));
        int threadsNum = Integer.parseInt(peelerArgs.get("--threads-num"));
        boolean skipPeeler = peelerArgs.containsKey("--skip-peeler");
        // 列出 PotatoPeeler 相关的参数
        GlobalLogger.info("====== POTATO-PEELER PARAMS ======");
        GlobalLogger.info("Min inhabited time (tick): " + minInhabited);
        GlobalLogger.info("Cool down (min): " + coolDown);
        GlobalLogger.info("MCA modifiable delay after creation (min): " + mcaModifiableDelay);
        GlobalLogger.info("Worker threads num: " + threadsNum);
        GlobalLogger.info("Verbose output: " + verboseOutput);
        GlobalLogger.info("Skip peeler: " + skipPeeler);
        GlobalLogger.info("World dir paths: ");
        for (String worldDirPath : worldDirPaths) {
            GlobalLogger.info("\t" + worldDirPath);
        }
        // 在 minInhabited > 200 时发出警告
        if (minInhabited > 200) {
            GlobalLogger.warning("****** WARNING ******");
            GlobalLogger.warning("You are setting 'minInhabited' to a value greater than 200 ticks (10 seconds).");
            GlobalLogger.warning("This may cause some chunks to be removed even if they are currently in use.");
            GlobalLogger.warning("Please make sure you know what you are doing.");
            GlobalLogger.warning("*********************");
        }
        // 计算自上次运行过去了多久
        long timeSinceLastRun = TimeUtils.timeNow() - TimeUtils.getLastRunTime();
        if (worldDirPaths.isEmpty()) {
            // 没有世界可处理，则跳过 Peeler
            GlobalLogger.info("====== POTATO-PEELER SKIPPED ======");
            GlobalLogger.info("No world to process.");
        } else if (skipPeeler) {
            // 指定了跳过
            GlobalLogger.info("====== POTATO-PEELER SKIPPED ======");
            GlobalLogger.info("Skipped.");
        } else if (timeSinceLastRun <= coolDown * 60) {
            // 注意 coolDown 单位是分钟
            // 自上次运行后还处于冷却期
            GlobalLogger.info("====== POTATO-PEELER SKIPPED ======");
            GlobalLogger.info("Currently in cool down period, skipped.");
        } else {
            // 开始处理区块
            GlobalLogger.info("====== POTATO-PEELER RUNNING ======");
            // 标记是否进行了处理
            boolean peeled = false;
            for (String worldDirPath : worldDirPaths) {
                try {
                    // 对于每个世界都进行处理
                    PeelResult peelResult = Potato.peel(worldDirPath, minInhabited, mcaModifiableDelay, threadsNum);
                    GlobalLogger.info("====== POTATO-PEELER RESULT ======");
                    GlobalLogger.info("Time elapsed: " + (double) peelResult.getTimeElapsed() / 1000D + "s");
                    GlobalLogger.info("Regions affected: " + peelResult.getRegionsAffected());
                    GlobalLogger.info("Chunks removed: " + peelResult.getChunksRemoved());
                    GlobalLogger.info("Size reduced: " + NumUtils.bytesToHumanReadable(peelResult.getSizeReduced()));
                    GlobalLogger.info("=====================================");
                    // 标记进行了处理
                    peeled = true;
                } catch (RegionFileNotFoundException e) {
                    // 发生了区域文件没找到的异常，跳过
                    GlobalLogger.severe("Failed to process regions of world: " + worldDirPath + ", skipped.", e);
                } catch (RegionTaskInterruptedException e) {
                    // 发生了区域处理被中断的异常
                    GlobalLogger.severe("Failed to process regions of world: " + worldDirPath + ", interrupted.", e);
                    // 退出程序
                    System.exit(1);
                } catch (Exception e) {
                    // 到这里如果捕捉到了未知的异常，则退出程序
                    GlobalLogger.severe("Unexpected exception!", e);
                    // 退出程序
                    System.exit(1);
                }
            }
            // 如果有世界被处理，更新上次运行的时间
            if (peeled)
                TimeUtils.setLastRunTime(TimeUtils.timeNow());
        }
        // 处理完区块后若没有指定 server-jar 则退出
        if (!peelerArgs.containsKey("--server-jar")) {
            GlobalLogger.info("No --server-jar specified, exiting normally.");
            System.exit(0);
        }
        // 启动服务器前先回收没有用的资源
        System.gc();
        // 如果指定了 --server-jar，就尝试启动 Minecraft 服务器
        String serverJarPath = peelerArgs.get("--server-jar");
        // 开启启动 Minecraft 服务器
        GlobalLogger.info("====== LAUNCHING MINECRAFT SERVER ======");
        try {
            // 在当前 JVM 中载入服务端 jar 并执行主类程序
            // 同时也传入了剩余参数 remainingArgs
            JarUtils.runJarInCurrentJVM(serverJarPath, remainingArgs.toArray(new String[0]));
        } catch (Exception e) {
            GlobalLogger.severe("Exception occurred when launching Minecraft server: " + serverJarPath, e);
            System.exit(1);
        }
    }
}