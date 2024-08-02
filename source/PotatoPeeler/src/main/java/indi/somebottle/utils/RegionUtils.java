package indi.somebottle.utils;

import indi.somebottle.entities.Region;
import indi.somebottle.exceptions.RegionFormatException;
import indi.somebottle.exceptions.RegionPosNotFoundException;
import indi.somebottle.exceptions.CompressionTypeUnsupportedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public static Region read(File regionFile) throws RegionPosNotFoundException, IOException, RegionFormatException {
        Region region = new Region(regionFile);
        try (RandomAccessFile metaReader = new RandomAccessFile(regionFile, "r")) {
            // 参考文档：https://zh.minecraft.wiki/w/%E5%8C%BA%E5%9F%9F%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F
            // 一次读取到的最大单位是一个扇区，定为 4 KiB
            // 因此开一个 4 KiB 的缓冲区
            byte[] buffer = new byte[4096];
            int byteRead = 0;
            // 一共有 1024 个区块的偏移和长度数据，逐个读取
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    // 先读取距离文件起点的偏移扇区数目
                    // 前 3 B 是大端存储的偏移扇区数目
                    byteRead = metaReader.read(buffer, 0, 3);
                    if (byteRead < 3) {
                        // 读取失败，文件格式错误
                        throw new RegionFormatException("MCA File format error in " + regionFile + ", unable to find sector offset of chunk " + x + ", " + z);
                    }
                    // 偏移扇区数目按大端方式转换为整数
                    // 通过与 0xFF，先提升为 int（因为 Java 的 byte 是 8 bit 有符号数），因为是大端。首个 8 位左移 16 位组成数值的最高字节， 以此类推把各个字节移动到对应位置上，通过或运算组成最终数值
                    // 因为单位是扇区，再乘上 4 KiB 得到偏移字节数
                    // long sectorOffset = ((long) (buffer[0] & 0xFF) << 16 | (long) (buffer[1] & 0xFF) << 8 | (long) (buffer[2] & 0xFF)) * 4096;
                    long sectorOffset = NumUtils.bigEndianToLong(buffer, 3) * 4096;
                    // 接下来一个字节是此区块占用的扇区数
                    int sectorsOccupied = metaReader.read();
                    if (sectorsOccupied < 0) {
                        // 读取失败，文件格式错误
                        throw new RegionFormatException("MCA File format error in " + regionFile + ", unable to find sectors occupied number of chunk " + x + ", " + z);
                    }
                    // 如果以上两个字段均为 0，说明此区块不存在
                    if (sectorOffset == 0 && sectorsOccupied == 0) {
                        continue;
                    }
                    // 继续读取区块
                    // TODO：单独给 Chunk 的读取开一个 RAF
                    // TODO: 注意处理 RegionFormatException，信息中加上区块局部坐标

                }
            }
            // TODO: 读取时间戳表到 Region 中

            // TODO: 清理工作
        }
        return region;
    }
}
