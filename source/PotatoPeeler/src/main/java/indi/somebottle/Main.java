package indi.somebottle;

import indi.somebottle.entities.PeelResult;
import indi.somebottle.exceptions.PeelerArgIncompleteException;
import indi.somebottle.exceptions.RegionFileNotFoundException;
import indi.somebottle.exceptions.RegionTaskInterruptedException;
import indi.somebottle.logger.GlobalLogger;
import indi.somebottle.utils.*;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // 如果一个参数都没有
        if (args.length == 0) {
            // 尝试从工作目录下的 potatopeeler.args 文件中读取参数
            try {
                args = ArgsUtils.readArgsFromFile("potatopeeler.args");
            } catch (IOException e) {
                // 没有这个文件，或者文件中没有指定有效参数
                GlobalLogger.info("No args provided. Use 'java -jar PotatoPeeler.jar --help' to get help on usage.");
                System.exit(0);
            }
        }
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
            GlobalLogger.severe("Use 'java -jar PotatoPeeler.jar --help' to get help on usage.");
            System.exit(1);
        }
        // 可能只需要打印帮助信息
        boolean helpNeeded = peelerArgs.containsKey("--help");
        if (helpNeeded) {
            printHelp();
            System.exit(0);
        }
        // 因为要设置日志记录级别，这里要先拿到 --verbose 选项
        boolean verboseOutput = peelerArgs.containsKey("--verbose");
        GlobalLogger.setVerbose(verboseOutput);
        GlobalLogger.info("Potato Peeler starting...");
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
        // 给没有指定的参数标上默认值
        ArgsUtils.setDefaultPeelerArgs(peelerArgs);
        if (!ArgsUtils.checkPeelerArgs(peelerArgs)) {
            // 有参数不合法则退出
            System.exit(1);
        }
        // 解析参数值
        List<String> worldDirPaths = ArgsUtils.parseWorldDirs(peelerArgs.get("--world-dirs"));
        long minInhabited = Long.parseLong(peelerArgs.get("--min-inhabited"));
        long coolDown = Long.parseLong(peelerArgs.get("--cool-down"));
        int threadsNum = Integer.parseInt(peelerArgs.get("--threads-num"));
        boolean skipPeeler = peelerArgs.containsKey("--skip-peeler");
        // 列出 PotatoPeeler 相关的参数
        GlobalLogger.info("====== POTATO-PEELER PARAMS ======");
        GlobalLogger.info("Min inhabited time (tick): " + minInhabited);
        GlobalLogger.info("Cool down (min): " + coolDown);
        GlobalLogger.info("Worker threads num: " + threadsNum);
        GlobalLogger.info("Verbose output: " + verboseOutput);
        GlobalLogger.info("Skip peeler: " + skipPeeler);
        GlobalLogger.info("World dir paths: ");
        for (String worldDirPath : worldDirPaths) {
            GlobalLogger.info("\t" + worldDirPath);
        }
        GlobalLogger.info("==================================");
        // 在 minInhabited > 200 时发出警告
        if (minInhabited > 200) {
            GlobalLogger.warning("****** WARNING ******");
            GlobalLogger.warning("You are setting 'minInhabited' to a value greater than 200 ticks (10 seconds).");
            GlobalLogger.warning("This may cause some chunks to be removed even if they are currently in use.");
            GlobalLogger.warning("Please make sure you know what you are doing.");
            GlobalLogger.warning("*********************");
            // 20 秒冷静期
            GlobalLogger.warning("The program will continue in 20 seconds.");
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                // 如果线程被中断，则退出程序
                System.exit(0);
            }
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
            GlobalLogger.info("********* DO NOT INTERRUPT ********");
            if (!verboseOutput) {
                // 提示用户可以打开细节输出
                GlobalLogger.info("You could use '--verbose' option for more detailed information.");
            }
            // 标记是否进行了处理
            boolean peeled = false;
            for (String worldDirPath : worldDirPaths) {
                try {
                    // 对于每个世界都进行处理
                    GlobalLogger.info(">>> Processing '" + worldDirPath + "' ...");
                    // 任务参数
                    PeelResult peelResult = Potato.peel(worldDirPath, threadsNum, minInhabited);
                    GlobalLogger.info("=========== WORLD RESULT ============");
                    GlobalLogger.info("World: " + worldDirPath);
                    GlobalLogger.info("Time elapsed: " + (double) peelResult.getTimeElapsed() / 1000D + "s");
                    GlobalLogger.info("Regions affected: " + peelResult.getRegionsAffected());
                    GlobalLogger.info("Chunks removed: " + peelResult.getChunksRemoved());
                    GlobalLogger.info("Size reduced: " + NumUtils.bytesToHumanReadable(peelResult.getSizeReduced()));
                    GlobalLogger.info("=====================================");
                    // 标记进行了处理
                    peeled = true;
                } catch (RegionFileNotFoundException e) {
                    // 发生了区域文件没找到的异常，跳过
                    GlobalLogger.warning("Regions of world: '" + worldDirPath + "' not found, skipped.");
                } catch (IOException e) {
                    // IO 异常
                    GlobalLogger.severe("I/O Exception occurred while processing world: '" + worldDirPath + "', skipped the world.", e);
                } catch (RegionTaskInterruptedException e) {
                    // 发生了区域处理被中断的异常
                    GlobalLogger.severe("Failed to process regions of world: '" + worldDirPath + "', interrupted.", e);
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

    /**
     * 打印帮助信息，当命令行选项有 --help 时执行
     */
    public static void printHelp() {
        System.out.println();
        System.out.println("Potato Peeler - A simple tool to remove unused chunks from Minecraft worlds.");
        System.out.println();
        System.out.println("Author: github.com/SomeBottle");
        System.out.println();
        System.out.println("Usage: ");
        System.out.println("\tjava [jvm-options] -jar PotatoPeeler.jar [options] [--world-dirs <worldPath1>,<worldPath2>,...]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("\t--help                           Show this help message and exit.");
        System.out.println("\t--min-inhabited <ticks>          Minimum inhabited time (in ticks) for a chunk to be considered unused. (default: 0)");
        System.out.println("\t--cool-down <minutes>            Cooldown period (in minutes) after the last run before Potato Peeler can run again. (default: 0)");
        System.out.println("\t--threads-num <number>           Number of worker threads to use. (default: 10)");
        System.out.println("\t--verbose                        Enable verbose output.");
        System.out.println("\t--skip-peeler                    Skip the Potato Peeler process.");
        System.out.println("\t--server-jar <server.jar>        Path to the Minecraft server JAR file to launch after processing regions.");
        System.out.println();
        System.out.println("List of protected chunks:");
        System.out.println("\t- In order to protect certain chunks from being removed, you can create a file named 'chunks.protected' in the world(dimension) directory, as a sibling of the directory 'region'.");
        System.out.println("\t- The file should contain the coordinates (ranges are supported) of the chunks you want to protect, one per line, in the format 'x,z' or 'x1~x2,z1~z2'.");
        System.out.println("\t- In addition, wildcard asterisk is supported. For instance:");
        System.out.println("\t   > By adding a line '*,*', all of the chunks will be protected.");
        System.out.println("\t   > '0~10,*' will protect chunks from x=0 to x=10 in all z positions.");
        System.out.println("\t   > '1~*,2~9' will protect chunks from x=1 to the maximum coordinate and z positions from z=2 to z=9.");
        System.out.println("\t   > '114,514' will only protect the chunk at x=114, z=514.");
        System.out.println("\t- Please note that comments starting with the '#' are supported, including both single-line and inline comments.");
        System.out.println();
        System.out.println("Example:");
        System.out.println("\tjava -Xmx4G -jar PotatoPeeler.jar --min-inhabited 50 --cool-down 60 --threads-num 5 --world-dirs 'world,world_nether,/opt/server/world_the_end' --server-jar server.jar");
        System.out.println();
        System.out.println("Note:");
        System.out.println("\t- World paths passed to '--world-dirs' should be separated by commas without any spaces.");
        System.out.println("\t- After the Potato Peeler process completes, the server JAR file will be launched in the current JVM. Any remaining arguments, including JVM options, will be passed to the server jar.");
    }
}