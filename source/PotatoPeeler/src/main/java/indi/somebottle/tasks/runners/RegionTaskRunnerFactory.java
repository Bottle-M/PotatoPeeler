package indi.somebottle.tasks.runners;

import indi.somebottle.entities.TaskParams;

import java.io.File;
import java.util.Queue;

/**
 * 区域任务执行器静态工厂类 <br>
 */
public class RegionTaskRunnerFactory {
    /**
     * 根据任务参数创建指定的区域任务执行器 <br>
     *
     * @param queue  区域文件队列
     * @param params 任务参数
     * @return 指定的区域任务执行器
     */
    public static RegionTaskRunner getTaskRunner(Queue<File> queue, TaskParams params) {
        if (params.absOutputDirPath == null) {
            // 如果输出路径未指定，使用原地处理
            return new InPlaceRegionTaskRunner(queue, params);
        } else {
            // 如果输出路径已指定，使用非原地处理
            return new CopyBasedRegionTaskRunner(queue, params);
        }
    }
}