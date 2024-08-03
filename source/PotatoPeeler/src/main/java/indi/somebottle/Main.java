package indi.somebottle;

import indi.somebottle.exceptions.PeelerArgIncompleteException;
import indi.somebottle.exceptions.RegionFileNotFoundException;
import indi.somebottle.exceptions.RegionTaskInterruptedException;
import indi.somebottle.utils.ArgsUtils;
import indi.somebottle.utils.JarUtils;
import indi.somebottle.utils.TimeUtils;
import indi.somebottle.utils.ExceptionUtils;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Potato Peeler starting...");
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
            ExceptionUtils.print(e);
            System.exit(1);
        }
        // 先输出命令行参数
        System.out.println("====== JVM ARGS ======");
        for (String arg : jvmArgs) {
            System.out.print(arg + " ");
        }
        System.out.println();
        // 再输出 PotatoPeeler 相关的参数
        System.out.println("====== POTATO-PEELER ARGS ======");
        for (String arg : peelerArgs.keySet()) {
            System.out.println(arg + " " + peelerArgs.get(arg));
        }
        // 最后是其他参数
        System.out.println("====== OTHER ARGS ======");
        for (String arg : remainingArgs) {
            System.out.print(arg + " ");
        }
        System.out.println();
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
        long mcaDeletableDelay = Long.parseLong(peelerArgs.get("--mca-deletable-delay"));
        int threadsNum = Integer.parseInt(peelerArgs.get("--threads-num"));
        boolean verboseOutput = peelerArgs.containsKey("--verbose");
        boolean skipPeeler = peelerArgs.containsKey("--skip-peeler");
        // 列出 PotatoPeeler 相关的参数
        System.out.println("====== POTATO-PEELER PARAMS ======");
        System.out.println("Min inhabited time (tick): " + minInhabited +
                "\nCool down (min): " + coolDown +
                "\nMCA deletable delay after creation (min): " + mcaDeletableDelay +
                "\nWorker threads num: " + threadsNum +
                "\nVerbose output: " + verboseOutput +
                "\nSkip peeler: " + skipPeeler +
                "\nWorld dir paths: ");
        for (String worldDirPath : worldDirPaths) {
            System.out.println("\t" + worldDirPath);
        }
        // 计算自上次运行过去了多久
        long timeSinceLastRun = TimeUtils.timeNow() - TimeUtils.getLastRunTime();
        if (worldDirPaths.isEmpty()) {
            // 没有世界可处理，则跳过 Peeler
            System.out.println("====== POTATO-PEELER SKIPPED ======");
            System.out.println("No world to process.");
        } else if (skipPeeler) {
            // 指定了跳过
            System.out.println("====== POTATO-PEELER SKIPPED ======");
            System.out.println("Skipped.");
        } else if (timeSinceLastRun <= coolDown * 60) {
            // 注意 coolDown 单位是分钟
            // 自上次运行后还处于冷却期
            System.out.println("====== POTATO-PEELER SKIPPED ======");
            System.out.println("Currently in cool down period, skipped.");
        } else {
            // 开始处理区块
            System.out.println("====== POTATO-PEELER RUNNING ======");
            // 标记是否进行了处理
            boolean peeled = false;
            for (String worldDirPath : worldDirPaths) {
                try {
                    // 对于每个世界都进行处理
                    Potato.peel(worldDirPath, minInhabited, mcaDeletableDelay, threadsNum, verboseOutput);
                    peeled = true;
                } catch (RegionFileNotFoundException e) {
                    // 发生了区域文件没找到的异常，跳过
                    ExceptionUtils.print(e, "Failed to process regions of world: " + worldDirPath + ", skipped.");
                } catch (RegionTaskInterruptedException e) {
                    // 发生了区域处理被中断的异常
                    ExceptionUtils.print(e, "Failed to process regions of world: " + worldDirPath + ", interrupted.");
                    // 退出程序
                    System.exit(1);
                } catch (Exception e) {
                    // 到这里如果捕捉到了未知的异常，则退出程序
                    ExceptionUtils.print(e, "Unexpected exception!");
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
            System.out.println("No --server-jar specified, exiting normally.");
            System.exit(0);
        }
        // 启动服务器前先回收没有用的资源
        System.gc();
        // 如果指定了 --server-jar，就尝试启动 Minecraft 服务器
        String serverJarPath = peelerArgs.get("--server-jar");
        // 开启启动 Minecraft 服务器
        System.out.println("====== LAUNCHING MINECRAFT SERVER ======");
        try {
            // 在当前 JVM 中载入服务端 jar 并执行主类程序
            // 同时也传入了剩余参数 remainingArgs
            JarUtils.runJarInCurrentJVM(serverJarPath, remainingArgs.toArray(new String[0]));
        } catch (Exception e) {
            ExceptionUtils.print(e, "Exception occurred when launching Minecraft server: " + serverJarPath);
            e.printStackTrace();
            System.exit(1);
        }
    }
}