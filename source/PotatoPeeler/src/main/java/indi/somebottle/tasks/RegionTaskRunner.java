package indi.somebottle.tasks;

import java.io.File;
import java.util.Queue;

// 实现 Runnable，在一个线程内从队列中取出 .mca 文件进行处理
public class RegionTaskRunner implements Runnable {
    private final long minInhabited;
    private final long mcaDeletableDelay;
    private final boolean verboseOutput;
    private final Queue<File> queue; // 此线程独有的任务队列

    public RegionTaskRunner(Queue<File> queue, long minInhabited, long mcaDeletableDelay, boolean verboseOutput) {
        this.queue = queue;
        this.minInhabited = minInhabited;
        this.mcaDeletableDelay = mcaDeletableDelay;
        this.verboseOutput = verboseOutput;
    }

    @Override
    public void run() {
        // 队列非空时不断取出进行处理
        while (!Thread.currentThread().isInterrupted() && !queue.isEmpty()) {
            // 队列有任务时就取出进行处理
            File mcaFile = queue.poll();

        }
    }
}
