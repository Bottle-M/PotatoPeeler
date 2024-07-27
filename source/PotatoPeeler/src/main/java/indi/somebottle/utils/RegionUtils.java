package indi.somebottle.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;

// 区块相关的工具方法
public class RegionUtils {
    /**
     * 扫描世界目录，找到其中的 region 目录（.mca 文件所在目录）
     *
     * @param worldPath 世界目录
     * @return 找到的 .mca 文件所在目录。如果没有找到会返回 null
     */
    public static Path findRegionDirPath(String worldPath) {
        // BFS 寻找 region 目录
        Queue<Path> pathQueue = new LinkedList<>();
        pathQueue.add(Paths.get(worldPath));
        while (!pathQueue.isEmpty()) {
            Path currPath = pathQueue.poll();
            if (currPath.toFile().isDirectory()) {
                File[] files = currPath.toFile().listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            pathQueue.add(Paths.get(file.getAbsolutePath()));
                        } else if (file.getName().endsWith(".mca")) {
                            // region 目录装有 .mca 文件则找到了
                            return currPath;
                        }
                    }
                }
            }
        }
        return null;
    }
}
