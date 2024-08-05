package indi.somebottle.utils;

import indi.somebottle.entities.Chunk;
import indi.somebottle.entities.Region;
import indi.somebottle.exceptions.RegionChunkInitializedException;
import indi.somebottle.exceptions.RegionFormatException;
import indi.somebottle.exceptions.RegionPosNotFoundException;
import indi.somebottle.exceptions.CompressionTypeUnsupportedException;
import indi.somebottle.logger.GlobalLogger;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

// 区域 Region 文件相关的工具方法
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
                            if (file.getName().equals("region")) {
                                // 找到了 region 目录
                                return Paths.get(file.getAbsolutePath());
                            }
                            // 否则继续寻找
                            pathQueue.add(Paths.get(file.getAbsolutePath()));
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 从文件中读取 Region 数据
     *
     * @param regionFile 区域 .mca 文件对象
     * @return 读取到的 Region 对象
     * @throws RegionPosNotFoundException          如果文件名字格式不正确会抛出此异常
     * @throws IOException                         如果文件读取失败会抛出此异常
     * @throws RegionFormatException               如果 .mca 文件格式不正确会抛出此异常
     * @throws CompressionTypeUnsupportedException 如果压缩类型不支持，会抛出此异常
     */
    public static Region readRegion(File regionFile) throws RegionPosNotFoundException, IOException, RegionFormatException, RegionChunkInitializedException {
        // TODO：待测试 GZip, LZ4, 无压缩情况下的区块读取
        Region region = new Region(regionFile);
        GlobalLogger.fine("Reading region file: " + regionFile.getName());
        // regionStream 用于读取 .mca 文件头部元数据
        // chunkReader 用于读取区块数据
        try (
                BufferedInputStream regionStream = new BufferedInputStream(new FileInputStream(regionFile));
                RandomAccessFile chunkReader = new RandomAccessFile(regionFile, "r")
        ) {
            // 参考文档：https://zh.minecraft.wiki/w/%E5%8C%BA%E5%9F%9F%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F
            // 一次读取到的最大单位是一个扇区，定为 4 KiB
            // 因此开一个 4 KiB 的缓冲区
            byte[] buffer = new byte[4096];
            int byteRead = 0;
            // 一共有 1024 个区块的偏移和长度数据，逐个读取
            /*
             * 注意，如果你改变了这里的遍历顺序，那么底下 writeRegion 拷贝区块数据的逻辑就要重写。
             *
             * SomeBottle 2024.8.5
             */
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    GlobalLogger.fine("\tReading chunk at " + x + ", " + z);
                    // 先读取距离文件起点的偏移扇区数目
                    // 前 3 B 是大端存储的偏移扇区数目
                    byteRead = regionStream.read(buffer, 0, 3);
                    if (byteRead < 3) {
                        // 读取失败，文件格式错误
                        throw new RegionFormatException("MCA File format error in " + regionFile + ", unable to find sector offset of chunk " + x + ", " + z);
                    }
                    // 偏移扇区数目按大端方式转换为整数
                    // 通过与 0xFF，先提升为 int（因为 Java 的 byte 是 8 bit 有符号数），因为是大端。首个 8 位左移 16 位组成数值的最高字节， 以此类推把各个字节移动到对应位置上，通过或运算组成最终数值
                    // 因为单位是扇区，再乘上 4 KiB 得到偏移字节数
                    // long chunkOffset = ((long) (buffer[0] & 0xFF) << 16 | (long) (buffer[1] & 0xFF) << 8 | (long) (buffer[2] & 0xFF)) * 4096;
                    long chunkOffset = NumUtils.bigEndianToLong(buffer, 3) * 4096;
                    // 接下来一个字节是此区块占用的扇区数
                    int sectorsOccupied = regionStream.read();
                    if (sectorsOccupied < 0) {
                        // 读取失败，文件格式错误
                        throw new RegionFormatException("MCA File format error in " + regionFile + ", unable to find sectors occupied number of chunk " + x + ", " + z);
                    }
                    // 如果以上两个字段均为 0，说明此区块不存在
                    if (chunkOffset == 0 && sectorsOccupied == 0) {
                        continue;
                    }
                    // 继续读取区块
                    try {
                        Chunk chunk = ChunkUtils.readChunk(chunkReader, chunkOffset, sectorsOccupied, x, z);
                        // 初始化区域对象中的区块结构
                        region.initChunkAt(x, z, chunk);
                    } catch (RegionFormatException e) {
                        // 在 RegionFormatException 的信息中添加 Region 信息后重新抛出
                        throw new RegionFormatException(e.getMessage() + " in Region " + regionFile.getName());
                    }
                }
            }
            // 读取时间戳表到 Region 中
            // 紧接着的是 1024 个 4 字节大端时间戳（纪元秒）
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    byteRead = regionStream.read(buffer, 0, 4);
                    if (byteRead < 4) {
                        // 读取失败，文件格式错误
                        throw new RegionFormatException("MCA File format error in " + regionFile + ", unable to find timestamp of chunk " + x + ", " + z);
                    }
                    // 将读出来的时间戳存入时间戳表
                    long timestamp = NumUtils.bigEndianToLong(buffer, 4);
                    region.setChunkModifiedTimeAt(x, z, timestamp);
                }
            }
            // 清理工作
            // 取消缓冲区引用
            buffer = null;
        }
        return region;
    }

    // TODO：把区块写回时需要建立 checksum 文件存放 CRC32 摘要

    /**
     * 把 Region 对象重新写入到指定文件 outputFile 中 <br>
     * 被标记为 deleted 的区块不会再被写入。
     *
     * @param region     区域对象
     * @param outputFile 输出文件对象
     * @throws RegionFormatException 如果 .mca 文件格式不正确会抛出此异常
     * @throws IOException           IO 异常
     */
    public static void writeRegion(Region region, File outputFile) throws IOException, RegionFormatException {
        File regionFile = region.getRegionFile();
        GlobalLogger.fine("Writing region to file: " + outputFile.getAbsolutePath());
        try (
                FileOutputStream fos = new FileOutputStream(outputFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                RandomAccessFile chunkReader = new RandomAccessFile(regionFile, "r")
        ) {
            GlobalLogger.fine("\tWriting the first sector of the mca file.");
            // 缓冲区
            byte[] dataBuf = new byte[4096];
            Chunk chunkTmp;
            // 写入区块偏移和长度表
            // 记录当前区块起始位置在 mca 文件中的偏移扇区数
            int currChunkOffset = 2; // 初始为 2 个扇区
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    chunkTmp = region.getChunkAt(x, z);
                    if (chunkTmp == null || chunkTmp.isDeleteFlag()) {
                        // 如果区块不存在，或者被标记为移除，直接写入 4 个零字节
                        Arrays.fill(dataBuf, 0, 4, (byte) 0);
                    } else {
                        // 如果区块存在，写入偏移和长度
                        // 偏移扇区数（3 字节大端）
                        NumUtils.longToBigEndian(currChunkOffset, dataBuf, 3);
                        // 再加 1 字节的占用扇区数
                        int sectorsOccupied = chunkTmp.getSectorsOccupiedInFile();
                        dataBuf[3] = (byte) sectorsOccupied;
                        // 累加到扇区偏移上
                        currChunkOffset += sectorsOccupied;
                    }
                    // 把每个区块的扇区偏移和占用扇区数写入
                    bos.write(dataBuf, 0, 4);
                }
            }
            GlobalLogger.fine("\tWriting the second sector of the mca file.");
            // 接着写入时间戳表
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    chunkTmp = region.getChunkAt(x, z);
                    if (chunkTmp == null || chunkTmp.isDeleteFlag()) {
                        // 若区块不存在或被标记为已删除，时间戳 4 字节置 0
                        Arrays.fill(dataBuf, 0, 4, (byte) 0);
                    } else {
                        // 否则把区块时间戳转换为大端 4 字节
                        NumUtils.longToBigEndian(region.getChunkModifiedTimeAt(x, z), dataBuf, 4);
                    }
                    // 把每个区块的时间戳写入
                    bos.write(dataBuf, 0, 4);
                }
            }
            GlobalLogger.fine("\tCopying chunk data.");
            // 最后拷贝现存区块的数据
            // 因为区块占用的空间实际上以扇区为单位，因此这里其实就是把原文件中区块对应的扇区复制过来
            // 因为在记录现存区块时的扫描顺序也是 x:0-31, z:0-31，故 getExistingChunks 得到的列表
            // 可以直接拿来用，可能不用再执行循环体 1024 次了
            for (Chunk chunk : region.getExistingChunks()) {
                // 跳过被移除的区块
                if (chunk.isDeleteFlag())
                    continue;
                /*
                    因为区块占用的空间是扇区（4 KiB）的整数倍，因此拷贝的时候也可以直接以扇区为单位进行拷贝。

                    SomeBottle 2024.8.5
                 */
                // 未被移除的区块直接把占用的扇区数据拷贝过来即可
                // 需要读取 bytesRemaining 字节
                long bytesRemaining = 4096L * chunk.getSectorsOccupiedInFile();
                // 跳转到区块数据在原文件中的起始位置
                chunkReader.seek(chunk.getOffsetInFile());
                while (bytesRemaining > 0) {
                    // 读取的字节数
                    int bytesToRead = (int) Math.min(bytesRemaining, dataBuf.length);
                    // 读取区块数据
                    if (chunkReader.read(dataBuf, 0, bytesToRead) != bytesToRead) {
                        throw new RegionFormatException("MCA File format error in " + regionFile.getName() + ", unable to copy chunk data, no enough bytes.");
                    }
                    // 写入到输出文件
                    bos.write(dataBuf, 0, bytesToRead);
                    // 更新剩余字节数
                    bytesRemaining -= bytesToRead;
                }
            }
        }
    }
}

