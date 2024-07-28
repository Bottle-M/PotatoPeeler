package indi.somebottle.core;

import indi.somebottle.utils.ExceptionUtils;

import java.io.File;
import java.util.concurrent.*;

// 此类用于把 .mca 文件分配给多个线程进行处理
public class RegionTaskDispatcher {
    private final long minInhabited;
    private final long mcaDeletableDelay;
    private final int threadsNum;
    private final boolean verboseOutput;
    private final ExecutorService executor;
    // 存放 .mca 文件对象的阻塞队列
    private final BlockingQueue<File> queue = new LinkedBlockingQueue<>();
    // 标记是否已经开始运行任务
    private boolean started = false;

    public RegionTaskDispatcher(long minInhabited, long mcaDeletableDelay, int threadsNum, boolean verboseOutput) {
        this.minInhabited = minInhabited;
        this.mcaDeletableDelay = mcaDeletableDelay;
        this.verboseOutput = verboseOutput;
        this.threadsNum = threadsNum;
        // 指定线程数初始化线程池
        this.executor = Executors.newFixedThreadPool(threadsNum);
    }

    /**
     * 提交一个新的 .mca 文件处理任务
     *
     * @param mcaFile mca 文件 File 对象
     */
    public void addTask(File mcaFile) {
        queue.add(mcaFile);
    }

    /**
     * 等待队列中的所有任务处理完成（阻塞）
     *
     * @return 是否正常完成
     */
    public boolean waitForCompletion() {
        try {
            while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                // 空体, pass
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            executor.shutdownNow();
            return false;
        }
        return true;
    }

    /**
     * 启动所有的工作线程，开始处理 .mca 文件
     *
     * @note 此方法只能执行一次，后续调用的时候是空操作
     */
    public void start() {
        if (started)
            return;
        started = true;
        // 启动 threadsNum 个线程
        for (int i = 0; i < threadsNum; i++) {
            executor.submit(() -> {
                while (!Thread.currentThread().isInterrupted() && !queue.isEmpty()) {
                    // 队列有任务时就取出进行处理
                }
            });
        }
        // 停止建立新的线程
        executor.shutdown();
    }
}
