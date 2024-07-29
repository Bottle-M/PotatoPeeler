package indi.somebottle;

import indi.somebottle.tasks.RegionTaskDispatcher;
import indi.somebottle.entities.PeelResult;
import indi.somebottle.exceptions.RegionFileNotFoundException;
import indi.somebottle.exceptions.RegionTaskAlreadyStartedException;
import indi.somebottle.exceptions.RegionTaskInterruptedException;
import indi.somebottle.exceptions.RegionTaskNotAcceptedException;
import indi.somebottle.utils.RegionUtils;

import java.io.File;
import java.nio.file.Path;

public class Potato {
    /**
     * 对某个世界的 .mca 区域文件进行处理
     *
     * @param worldPath         世界目录路径
     * @param minInhabited      InhabitedTime 阈值
     * @param mcaDeletableDelay 自 .mca 创建多久后能删除（分钟）
     * @param threadsNum        线程数
     * @param verboseOutput     是否输出详细信息
     * @return 处理后的结果 PeelResult
     */
    public static PeelResult peel(String worldPath, long minInhabited, long mcaDeletableDelay, int threadsNum, boolean verboseOutput) throws RegionFileNotFoundException, RegionTaskInterruptedException, RegionTaskNotAcceptedException, RegionTaskAlreadyStartedException {
        long sizeReduced = 0;
        long chunksRemoved = 0;
        long regionsAffected = 0;
        long startTime = System.currentTimeMillis();
        // 先检查世界目录下的区域文件目录是否存在
        Path regionDirPath = RegionUtils.findRegionDirPath(worldPath);
        if (regionDirPath == null) {
            // 没有找到区域文件所在目录
            throw new RegionFileNotFoundException("Can not find region directory in " + worldPath + " or there's no mca file in it.");
        }
        // 扫描目录下的 .mca 文件
        File[] mcaFiles = regionDirPath.toFile().listFiles(file -> file.getName().endsWith(".mca"));
        if (mcaFiles == null || mcaFiles.length == 0) {
            // 没有找到 .mca 文件
            throw new RegionFileNotFoundException("Can not find .mca files in " + regionDirPath);
        }
        // 找到 .mca 文件了则开始处理
        // 创建任务调度器
        RegionTaskDispatcher dispatcher = new RegionTaskDispatcher(minInhabited, mcaDeletableDelay, threadsNum, verboseOutput);
        // 把文件提交给任务调度器
        for (File mcaFile : mcaFiles) {
            dispatcher.addTask(mcaFile);
        }
        // 启动任务调度器
        dispatcher.start();
        // 等待任务完成
        if (!dispatcher.waitForCompletion()) {
            // 如果被打断了，抛出异常
            throw new RegionTaskInterruptedException("Interrupted while waiting for .mca files to be processed.");
        }
        long timeElapsed = System.currentTimeMillis() - startTime;
        return new PeelResult(sizeReduced, chunksRemoved, regionsAffected, timeElapsed);
    }
}
