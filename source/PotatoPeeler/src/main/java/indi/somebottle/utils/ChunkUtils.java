package indi.somebottle.utils;

import indi.somebottle.entities.Chunk;
import indi.somebottle.entities.IntRange;
import indi.somebottle.exceptions.NBTFormatException;
import indi.somebottle.exceptions.RegionFormatException;
import indi.somebottle.exceptions.CompressionTypeUnsupportedException;
import indi.somebottle.indexing.ChunksSpatialIndex;
import indi.somebottle.streams.ChunkDataInputStream;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * 区块 Chunk 相关的工具方法
 */
public class ChunkUtils {

    /**
     * nbt 二进制数据中的 InhabitedTime 标签部分的十六进制表示 <br>
     * 0x04 表示这是一个 Long 类型标签 <br>
     * 0x00 0x0D 表示这个标签的名字长度为 13 字节 <br>
     * 0x49 0x6E 0x68 0x61 0x62 0x69 0x74 0x65 0x64 0x54 0x69 0x6d 0x65 表示标签名 "InhabitedTime" <br>
     * 在此之后的 8 个字节即为 InhabitedTime 的值（大端）
     * <br><br>
     * 参考: <a href="https://zh.minecraft.wiki/w/NBT%E6%A0%BC%E5%BC%8F?variant=zh-cn#%E4%BA%8C%E8%BF%9B%E5%88%B6%E6%A0%BC%E5%BC%8F">NBT 二进制格式</a>, <a href="https://zh.minecraft.wiki/w/%E5%8C%BA%E5%9D%97%E6%A0%BC%E5%BC%8F">区块格式</a>
     */
    private static final byte[] INHABITED_TIME_TAG_BIN = {0x04, 0x00, 0x0D, 0x49, 0x6E, 0x68, 0x61, 0x62, 0x69, 0x74, 0x65, 0x64, 0x54, 0x69, 0x6d, 0x65};

    /*
        可以发现，INHABITED_TIME_TAG_BIN 序列无论以哪个下标为止，都没有任何的“相等前后缀”。

        用 KMP 算法来考虑的话，我在 nbt 文件的字节序列中寻找 INHABITED_TIME_TAG_BIN 时，只要遇到和主字节串失配的地方，就可以直接把 INHABITED_TIME_TAG_BIN 的搜索指针回退到头部，而主字节串指针不用回退。

        SomeBottle 2024.8.1
     */

    /**
     * 强制加载区块存储文件中的 Forced 标签部分的十六进制表示 <br>
     * 0x0C 表示这是一个 Long 类型数组 <br>
     * 0x00 0x06 表示这个标签名字长 6 字节 <br>
     * 后门这一段表示标签名 Forced。<br>
     * 标签名后面 4 个字节是数组长度，随后则是 Long 类型的数组元素。
     */
    public static final byte[] FORCED_TAG_BIN = {0x0C, 0x00, 0x06, 0x46, 0x6F, 0x72, 0x63, 0x65, 0x64};

    /**
     * 从 .mca 文件中根据偏移读出特定的区块
     *
     * @param reader                mca 文件 RandomAccessFile 对象
     * @param offsetInFile          区块数据起始位置距离 Region 文件开头的偏移
     * @param sectorsOccupiedInFile 区块占用的扇区数
     * @param x                     区块在区域内的局部坐标 x（0-31）
     * @param z                     区块在区域内的局部坐标 z（0-31）
     * @param regionX               区块所在区域的 X 坐标
     * @param regionZ               区块所在区域的 Z 坐标
     * @return Chunk 对象
     * @throws RegionFormatException 当区块数据有误时抛出
     * @apiNote 注意：这个方法会改变 RandomAccessFile 的指针位置
     */
    public static Chunk readChunk(RandomAccessFile reader, long offsetInFile, int sectorsOccupiedInFile, int x, int z, int regionX, int regionZ) throws IOException {
        // 把区块局部坐标转换为全局坐标
        // 低 5 位为区块局部坐标，高 27 位为区域坐标
        // 参考: https://zh.minecraft.wiki/w/%E5%8C%BA%E5%9F%9F%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F#%E5%8C%BA%E5%9F%9F
        int globalX = regionX << 5 | x;
        int globalZ = regionZ << 5 | z;
        byte[] buffer = new byte[4];
        // 先把指针移动到区块数据起始处
        reader.seek(offsetInFile);
        // 获得区块数据真实长度
        // 区块数据前 4 个字节是大端存储的
        if (reader.read(buffer, 0, 4) < 4) {
            // 读取失败
            throw new RegionFormatException("Chunk data error: unable to read data length of chunk (" + x + ", " + z + ")");
        }
        // 按大端方式转换为整数，因为有 4 个字节，而 Java 没有无符号数，这里需要用 long 进行存储
        // long chunkDataLen = (long) (buffer[0] & 0xFF) << 24 | (long) (buffer[1] & 0xFF) << 16 | (long) (buffer[2] & 0xFF) << 8 | (long) (buffer[3] & 0xFF);
        long chunkDataLen = NumUtils.bigEndianToLong(buffer, 4);
        // 因为这个是从压缩方式这一个字节开始算的，因此实际数据长度还要减去 1 字节
        chunkDataLen--;
        // 继续读取 1 个字节，这个字节是压缩格式
        int compressionType = reader.read();
        if (compressionType < 0) {
            // 读取失败
            throw new RegionFormatException("Chunk data error: unable to read compression type of chunk (" + x + ", " + z + ")");
        }
        /*
            数据长度超出 255 个扇区的区块，其压缩类型的最高字节会被标记为 1，此时其值会 > 128
            SomeBottle 2024.8.10
            参考:
                - https://minecraft.wiki/w/Region_file_format#Payload
                - https://zh.minecraft.wiki/w/%E5%8C%BA%E5%9F%9F%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F
         */
        if (compressionType > 128) {
            // 对于超出 255 个扇区的区块应当设定为 overSized，且不读取其 inhabitedTime，直接返回
            return new Chunk(globalX, globalZ, offsetInFile, sectorsOccupiedInFile, -1, true);
        }
        try {
            // 此处 reader 指针已经指向区块数据起始字节
            long inhabitedTime = findInhabitedTime(reader, (int) chunkDataLen, compressionType);
            // 构建新的区块对象
            return new Chunk(globalX, globalZ, offsetInFile, sectorsOccupiedInFile, inhabitedTime, false);
        } catch (NBTFormatException | CompressionTypeUnsupportedException e) {
            // 发生 NBTFormatException 后加上区块坐标信息
            throw new RegionFormatException(e.getMessage() + " in chunk (" + x + ", " + z + ")");
        }
    }

    /**
     * 解压并读取区块的 InhabitedTime 数据
     *
     * @param chunkReader     区块文件读取 RandomAccessFile
     * @param dataLen         最多读取多少数据长度（记得去掉“压缩方式”占用的一个字节）
     * @param compressionType 压缩类型
     * @return 读取出的 InhabitedTime 数据（Long）
     * @throws IOException                         如果读取失败会抛出此异常
     * @throws NBTFormatException                  当区块数据有误，读取不到 InhabitedTime 时抛出
     * @throws CompressionTypeUnsupportedException 如果压缩类型不支持会抛出此异常
     * @apiNote 调用前，请把 chunkReader 指针移动到开始读取的位置。注意，读取完毕后 chunkReader 指针应该在读取的最后一个字节处。
     */
    public static long findInhabitedTime(RandomAccessFile chunkReader, int dataLen, int compressionType) throws IOException, CompressionTypeUnsupportedException {
        // 其实可以读取 nbt 文件的二进制流，找到指定的字节，虽然标签没有明显的头部和尾部标记，但是要找到 InhabitedTime 这个 Long 标签还是不难的
        try (ChunkDataInputStream cdis = new ChunkDataInputStream(chunkReader, dataLen, compressionType)) {
            if (IOUtils.findAndSkipBytes(cdis, INHABITED_TIME_TAG_BIN)) {
                // 如果找到了标签名，便接着读取后 8 个字节
                // 注意 nbt Long 标签值是有符号数，因此用 long 存储
                byte[] numBuf = new byte[8];
                if (cdis.read(numBuf) == 8) {
                    // 正好读入了 8 字节，按大端序进行转换
                    // 虽然只有 8 个字节，但不用担心会变成负数
                    // InhabitedTime 怎么也不可能这么大
                    return NumUtils.bigEndianToLong(numBuf, 8);
                } else {
                    // 否则读取失败
                    throw new NBTFormatException("InhabitedTime was found, but no enough bytes to read");
                }
            }
        }
        // 没有找到
        throw new NBTFormatException("InhabitedTime not found in chunk data");
    }


    /**
     * 从名单文件中读取出受保护的区块，建立区块索引
     *
     * @param protectedChunksIndex 区块空间索引对象
     * @param listFile             名单文件 File 对象，其中的受保护区块将被存入 protectedChunksIndex 中
     * @return 新的区块空间索引对象
     * @throws IOException 文件读取出错时抛出
     */
    public static ChunksSpatialIndex readProtectedChunks(ChunksSpatialIndex protectedChunksIndex, File listFile) throws IOException {
        // 支持类似 .gitignore 的 # 注释
        // 每行一个配置，支持区块坐标 x,z、区块坐标范围 x1-x2,z1-z2 或 *,z1-z2 乃至 *-x2, z1-* 这样的配置
        long lineCnt = 0; // 记录行号，方便定位错误
        String line;
        try (
                FileReader fr = new FileReader(listFile);
                BufferedReader br = new BufferedReader(fr)
        ) {
            while ((line = br.readLine()) != null) {
                lineCnt++;
                // 可能有行内注释，用 # 开头，忽略 # 之后的部分即可
                line = line.split("#")[0];
                // 去除配置内容首尾的空白字符
                line = line.trim();
                // 跳过空行
                if (line.isEmpty())
                    continue;
                String[] parts = line.split(",");
                // 缺少了对 x 或者 z 坐标范围的指定
                if (parts.length != 2)
                    throw new IOException("Line " + lineCnt + ": Invalid line here: '" + line + "'");
                try {
                    IntRange xRange = ParseUtils.parseSingleIntRange(parts[0]);
                    IntRange zRange = ParseUtils.parseSingleIntRange(parts[1]);
                    // protectedChunksIndex immutable
                    // 注意 RangeUtils.parseSingleIntRange 方法保证 to > from，这也是 rtree 构建时的要求
                    // 采用双精度浮点数，不然 32 位有符号整数转换时可能损失精度
                    if (xRange.from == xRange.to && zRange.from == zRange.to) {
                        // 是一个点
                        protectedChunksIndex = protectedChunksIndex.add(xRange.from, zRange.from);
                    } else {
                        // 是一个矩形区域
                        protectedChunksIndex = protectedChunksIndex.add(xRange.from, zRange.from, xRange.to, zRange.to);
                    }
                } catch (Exception e) {
                    // 在头部加上行号再抛出
                    throw new IOException("Line " + lineCnt + ": " + e.getMessage() + ": Invalid line here: '" + line + "'");
                }
            }
        }
        return protectedChunksIndex;
    }


    /**
     * 把强制加载区块文件中的区块存入受保护区块索引<br>
     * - 这个文件是用 GZip 压缩的。
     *
     * @param protectedChunksIndex  受保护区块索引
     * @param forceLoadedChunksFile 强制加载区块存储文件的 File 对象
     * @return 新的区块空间索引对象
     * @throws IOException        如果读取失败会抛出此异常
     * @throws NBTFormatException 当 NBT 标签数据有误，无法读取到一些字段时抛出
     */
    public static ChunksSpatialIndex protectForceLoadedChunks(ChunksSpatialIndex protectedChunksIndex, File forceLoadedChunksFile) throws IOException {
        // 和 findInhabitedTime 思路类似
        // 强制加载区块存储文件是以 GZip 算法压缩的
        try (
                FileInputStream fis = new FileInputStream(forceLoadedChunksFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                GZIPInputStream gzis = new GZIPInputStream(bis)
        ) {
            if (IOUtils.findAndSkipBytes(gzis, FORCED_TAG_BIN)) {
                byte[] numBuf = new byte[8];
                // 找到了 Forced 标签，先读取其后 4 个字节，这是 Long 数组长度
                if (gzis.read(numBuf, 0, 4) != 4) {
                    // 读取失败
                    throw new NBTFormatException("Forced tag was found, but unable to read array size in file:" + forceLoadedChunksFile.getAbsolutePath());
                }
                long arrSize = NumUtils.bigEndianToLong(numBuf, 4);
                for (int i = 0; i < arrSize; i++) {
                    // 读取数组中的每个元素
                    if (gzis.read(numBuf) != 8) {
                        // 读取失败
                        throw new NBTFormatException("Forced tag was found, but unable to read array element [" + i + "] in file:" + forceLoadedChunksFile.getAbsolutePath());
                    }
                    long chunkPos = NumUtils.bigEndianToLong(numBuf, 8);
                    // 强制加载区块文件中，每个 Long 元素的低 4 字节是 x 坐标，高 4 字节是 z 坐标
                    int x = (int) (chunkPos & 0xFFFFFFFFL);
                    int z = (int) (chunkPos >> 32 & 0xFFFFFFFFL);
                    // 把区块坐标点加入到索引中
                    protectedChunksIndex = protectedChunksIndex.add(x, z);
                }
            }
        }
        return protectedChunksIndex;
    }

}
