package indi.somebottle;

import indi.somebottle.exceptions.PeelerArgIncompleteException;
import indi.somebottle.utils.ArgsUtils;
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
        // 开启启动 Minecraft 服务器
        System.out.println("Passing parameters to launch Minecraft server...");
    }
}