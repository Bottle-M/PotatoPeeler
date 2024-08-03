package indi.somebottle.utils;

import indi.somebottle.entities.Chunk;
import indi.somebottle.exceptions.RegionFormatException;
import indi.somebottle.streams.ChunkDataInputStream;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 区块 Chunk 相关的工具方法
 */
public class ChunkUtils {
    /**
     * 区块数据部分的最大长度
     * <p> </p>
     * 这其实是受限于区块整体占用的扇区数，最多只能有 255 个扇区，去掉头部的 5 字节，所以最多有 255*4096-5 字节。
     */
    public static final int MAX_CHUNK_DATA_LEN = 255 * 4096 - 5;

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
     * 从 .mca 文件中根据偏移读出特定的区块
     *
     * @param reader                mca 文件 RandomAccessFile 对象
     * @param offsetInFile          区块数据起始位置距离 Region 文件开头的偏移
     * @param sectorsOccupiedInFile 区块占用的扇区数
     * @param x                     区块在区域内的局部坐标 x
     * @param z                     区块在区域内的局部坐标 z
     * @return Chunk 对象
     * @throws RegionFormatException 当区块数据有误时抛出
     * @apiNote 注意：这个方法会改变 RandomAccessFile 的指针位置
     */
    public static Chunk readChunk(RandomAccessFile reader, long offsetInFile, int sectorsOccupiedInFile, int x, int z) throws IOException {
        // 先把先前的指针位置备个份
        // 开一个 4 KiB × sectorsOccupiedInFile 的缓冲区
        // 最多会读取这么多数据
        byte[] buffer = new byte[4096 * sectorsOccupiedInFile];
        int byteRead = 0;
        // 先把指针移动到区块数据起始处
        reader.seek(offsetInFile);
        // 获得区块数据真实长度
        // 区块数据前 4 个字节是大端存储的
        byteRead = reader.read(buffer, 0, 4);
        if (byteRead < 4) {
            // 读取失败
            throw new RegionFormatException("Chunk data error: unable to read chunk data length.");
        }
        // 按大端方式转换为整数，因为有 4 个字节，而 Java 没有无符号数，这里需要用 long 进行存储
        // long chunkDataLen = (long) (buffer[0] & 0xFF) << 24 | (long) (buffer[1] & 0xFF) << 16 | (long) (buffer[2] & 0xFF) << 8 | (long) (buffer[3] & 0xFF);
        long chunkDataLen = NumUtils.bigEndianToLong(buffer, 4);
        // 因为这个是从压缩方式这一个字节开始算的，因此还要减去 1 字节
        chunkDataLen--;
        if (chunkDataLen > MAX_CHUNK_DATA_LEN) {
            // 对于超出 MAX_CHUNK_DATA_LEN 的区块应当设定为 overSized，且不读取其 inhabitedTime，直接返回
            return new Chunk(x, z, offsetInFile, sectorsOccupiedInFile, -1, true);
        }
        // 继续读取 1 个字节，这个字节是压缩格式
        int compressionType = reader.read();
        if (compressionType < 0) {
            // 读取失败
            throw new RegionFormatException("Chunk data error: unable to read compression type.");
        }
        // 接下来把区块数据读入缓冲区
        // 此处 reader 指针已经指向区块数据起始字节
        long inhabitedTime = findInhabitedTime(reader, (int) chunkDataLen, compressionType);
        // 构建新的区块对象
        return new Chunk(x, z, offsetInFile, sectorsOccupiedInFile, inhabitedTime, false);
    }

    /**
     * 解压并读取区块的 InhabitedTime 数据
     *
     * @param chunkReader     区块文件读取 RandomAccessFile
     * @param dataLen         最多读取多少数据长度（记得去掉“压缩方式”占用的一个字节）
     * @param compressionType 压缩类型
     * @return 读取出的 InhabitedTime 数据（Long）
     * @throws IOException           如果读取失败会抛出此异常
     * @throws RegionFormatException 当区块数据有误，读取不到 InhabitedTime 时抛出
     * @apiNote 调用前，请把 chunkReader 指针移动到开始读取的位置。注意，读取完毕后 chunkReader 指针应该在读取的最后一个字节处。
     */
    public static long findInhabitedTime(RandomAccessFile chunkReader, int dataLen, int compressionType) throws IOException {
        // 其实可以读取 nbt 文件的二进制流，找到指定的字节，虽然标签没有明显的头部和尾部标记，但是要找到 InhabitedTime 这个 Long 标签还是不难的
        // 用于进行字节匹配的指针
        int searchPtr = 0;
        try (ChunkDataInputStream cdis = new ChunkDataInputStream(chunkReader, dataLen, compressionType)) {
            int byteRead;
            while ((byteRead = cdis.read()) != -1) {
                // 与上 0xFF 提升至 int，方便比较。
                if (byteRead == (INHABITED_TIME_TAG_BIN[searchPtr] & 0xFF)) {
                    // 字节匹配则指针后移
                    searchPtr++;
                } else if (byteRead == (INHABITED_TIME_TAG_BIN[0] & 0xFF)) {
                    // 如果不匹配，检查开头字节是否和当前字节一致
                    // 如果是的话下一次从子串第二个字节开始匹配
                    searchPtr = 1;
                } else {
                    // 否则下一次从头开始
                    searchPtr = 0;
                }
                // 如果整个 INHABITED_TIME_TAG_BIN 串匹配上了
                // 说明找到了 InhabitedTime 标签
                if (searchPtr == INHABITED_TIME_TAG_BIN.length) {
                    // 接着读取后 8 个字节
                    // 注意 nbt Long 标签值是有符号数，因此用 long 存储
                    byte[] numBuf = new byte[8];
                    if (cdis.read(numBuf) == 8) {
                        // 正好读入了 8 字节，按大端序进行转换
                        return NumUtils.bigEndianToLong(numBuf, 8);
                    } else {
                        // 否则读取失败
                        throw new RegionFormatException("InhabitedTime was found, but no enough bytes to read.");
                    }
                }
            }
        }
        // 没有找到
        throw new RegionFormatException("InhabitedTime not found in chunk data.");
    }
}
