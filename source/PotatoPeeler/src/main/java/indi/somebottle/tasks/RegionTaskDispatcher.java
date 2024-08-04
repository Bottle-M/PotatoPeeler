package indi.somebottle.tasks;

import indi.somebottle.exceptions.RegionTaskAlreadyStartedException;
import indi.somebottle.exceptions.RegionTaskNotAcceptedException;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

// 此类用于把 .mca 文件分配给多个线程进行处理
public class RegionTaskDispatcher {
    private final long minInhabited;
    private final long mcaModifiableDelay;
    private final int threadsNum;
    private final boolean verboseOutput;
    private final ExecutorService executor;
    // 存放 .mca 文件对象的队列
    // 每个线程都有一个
    private final List<Queue<File>> queues = new ArrayList<>();
    // 记录上次把任务加入到哪个下标的队列中了
    private int enqueueIndex = 0;
    // 标记是否已经开始运行任务
    private boolean started = false;

    public RegionTaskDispatcher(long minInhabited, long mcaModifiableDelay, int threadsNum, boolean verboseOutput) {
        this.minInhabited = minInhabited;
        this.mcaModifiableDelay = mcaModifiableDelay;
        this.verboseOutput = verboseOutput;
        this.threadsNum = threadsNum;
        // 指定线程数初始化线程池
        this.executor = Executors.newFixedThreadPool(threadsNum);
        // 为每个线程都初始化一个队列
        for (int i = 0; i < threadsNum; i++) {
            queues.add(new LinkedList<>());
        }
    }

    /**
     * 提交一个新的 .mca 文件处理任务
     *
     * @param mcaFile mca 文件 File 对象
     * @throws RegionTaskNotAcceptedException 如果在启动执行后尝试添加任务则会抛出
     * @apiNote 启动任务执行后不可添加新任务（为了线程安全而设计）
     */
    public void addTask(File mcaFile) throws RegionTaskNotAcceptedException {
        if (started)
            throw new RegionTaskNotAcceptedException("Can not add task after start.");
        // 把任务均匀地分散到各个队列中
        queues.get(enqueueIndex).add(mcaFile);
        enqueueIndex = (enqueueIndex + 1) % threadsNum;
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
                // 等待所有线程执行完成
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
     * @throws RegionTaskAlreadyStartedException 如果在启动执行后尝试再次启动则会抛出
     * @apiNote 此方法只能执行一次，重复调用会抛出异常
     */
    public void start() throws RegionTaskAlreadyStartedException {
        // 重复启动会抛出异常
        if (started)
            throw new RegionTaskAlreadyStartedException("The task has been started.");
        started = true;
        // 启动 threadsNum 个线程
        for (int i = 0; i < threadsNum; i++) {
            executor.submit(new RegionTaskRunner(queues.get(i), minInhabited, mcaModifiableDelay, verboseOutput));
        }
        // 停止建立新的线程
        executor.shutdown();
    }
}
