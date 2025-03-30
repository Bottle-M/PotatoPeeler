package indi.somebottle.utils;

import indi.somebottle.constants.NBTTagConstants;
import indi.somebottle.entities.Chunk;
import indi.somebottle.entities.IntRange;
import indi.somebottle.exceptions.NBTFormatException;
import indi.somebottle.exceptions.RegionFormatException;
import indi.somebottle.exceptions.CompressionTypeUnsupportedException;
import indi.somebottle.indexing.ChunksSpatialIndex;
import indi.somebottle.streams.ChunkDataInputStream;
import indi.somebottle.constants.DataVersionConstants;
import indi.somebottle.versioned.ChunksDatReadHandlerFactory;
import indi.somebottle.entities.ForcedChunksLoadResult;
import indi.somebottle.versioned.handlers.ChunksDatReadHandler;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * 区块 Chunk 相关的工具方法
 */
public class ChunkUtils {

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
            if (IOUtils.findAndSkipBytes(cdis, NBTTagConstants.INHABITED_TIME_TAG_BIN)) {
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
        // 每行一个配置，支持区块坐标 x,z、区块坐标范围 x1~x2,z1~z2 或 *,z1~z2 乃至 *~x2, z1~* 这样的配置
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
     * 把 chunks.dat 文件中的强制加载区块存入受保护区块索引<br>
     * - 这个文件是用 GZip 压缩的。
     *
     * @param protectedChunksIndex 受保护区块索引
     * @param chunksDatFile        chunks.dat 文件的 File 对象
     * @return 返回更新后的索引以及新增的区块数量
     * @throws IOException        如果读取失败会抛出此异常
     * @throws NBTFormatException 当 NBT 标签数据有误，无法读取到一些字段时抛出
     */
    public static ForcedChunksLoadResult protectForceLoadedChunks(ChunksSpatialIndex protectedChunksIndex, File chunksDatFile) throws IOException {
        // 和 findInhabitedTime 思路类似
        // 强制加载区块存储文件是以 GZip 算法压缩的
        byte[] decompressedData;
        try (
                FileInputStream fis = new FileInputStream(chunksDatFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                GZIPInputStream gzis = new GZIPInputStream(bis);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ) {
            byte[] decompressBuf = new byte[1024];
            int bytesRead;
            while ((bytesRead = gzis.read(decompressBuf)) != -1) {
                baos.write(decompressBuf, 0, bytesRead);
            }
            decompressedData = baos.toByteArray();
        }
        InputStream bais = new ByteArrayInputStream(decompressedData);
        // 根据 wiki，如果没有 DataVersion 字段，默认为 1343（JE 1.12.2）
        int dataVersion = DataVersionConstants.DATA_VERSION_1_12_2;
        if (IOUtils.findAndSkipBytes(bais, NBTTagConstants.DATA_VERSION_TAG_BIN)) {
            byte[] numBuf = new byte[4];
            if (bais.read(numBuf, 0, 4) == 4) {
                dataVersion = (int) NumUtils.bigEndianToLong(numBuf, 4);
            } else {
                throw new NBTFormatException("DataVersion tag was found, but unable to read data version in file:" + chunksDatFile.getAbsolutePath());
            }
        }
        // 读取 chunks.dat 文件中的强制加载区块到索引中
        ChunksDatReadHandler chunksDatReadHandler = ChunksDatReadHandlerFactory.getChunksDatReadHandler(dataVersion, decompressedData, chunksDatFile);
        return chunksDatReadHandler.loadForcedIntoSpatialIndex(protectedChunksIndex);
    }

}
