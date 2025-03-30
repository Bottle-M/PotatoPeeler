package indi.somebottle.versioned.handlers;

import indi.somebottle.constants.NBTTagConstants;
import indi.somebottle.versioned.entities.ChunkCompound;
import indi.somebottle.exceptions.NBTFormatException;
import indi.somebottle.indexing.ChunksSpatialIndex;
import indi.somebottle.utils.IOUtils;
import indi.somebottle.utils.NumUtils;
import indi.somebottle.entities.ForcedChunksLoadResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ChunksDatReadHandlerImpl4325 implements ChunksDatReadHandler {
    byte[] chunksDatBytes;
    File chunksDatFile;

    /**
     * 构造函数（1.21.5 的实现）
     *
     * @param chunksDatBytes chunks.dat 文件的字节数据
     * @param chunksDatFile  chunks.dat 文件的 File 对象
     */
    public ChunksDatReadHandlerImpl4325(byte[] chunksDatBytes, File chunksDatFile) {
        // chunks.dat 文件的字节数据
        this.chunksDatBytes = chunksDatBytes;
        // chunks.dat 文件的 File 对象
        this.chunksDatFile = chunksDatFile;
    }

    /**
     * 根据 data 解析区块标签列表 NBT 数据（chunks.data 中的 tickets 列表）
     *
     * @param bais    tickets 列表部分的数据流，即从列表首个元素的第一个字节开始直至最后一个元素的后的 TagEnd 字节
     * @param listLen tickets 列表的长度
     * @return ChunkCompound[] 解析后的区块标签列表
     * @throws NBTFormatException NBT解析失败时抛出
     * @throws IOException        IO 失败时抛出
     */
    private ChunkCompound[] parseChunkTicketsNBT(InputStream bais, int listLen) throws IOException {
        List<ChunkCompound> chunkCompounds = new ArrayList<>();
        // 坐标缓冲区，X / Z 坐标各占 4 字节
        byte[] coordBuf = null;
        // 存储区块标签类型
        String chunkType = null;
        int byteRead;
        while (chunkCompounds.size() < listLen && (byteRead = bais.read()) != -1) {
            switch (byteRead) {
                case 0x0B:
                    // 只有 chunk_pos 用了 0x0B 标签，遇到 chunk_pos 标签了
                    // 跳过随后的 15 个字节
                    if (bais.skip(15) != 15) {
                        // 读取失败
                        throw new NBTFormatException("Unable to read 'chunk_pos' tag, failed to skip prefix. File:" + chunksDatFile.getAbsolutePath());
                    }
                    // 读取坐标
                    coordBuf = new byte[8];
                    if (bais.read(coordBuf) != 8) {
                        // 读取失败
                        throw new NBTFormatException("Unable to read 'chunk_pos' tag, incomplete coordinate data. File:" + chunksDatFile.getAbsolutePath());
                    }
                    break;
                case 0x03:
                    // level 标签是整型 0x03，跳过 2+5+4 = 11 字节
                    if (bais.skip(11) != 11) {
                        // 读取失败
                        throw new NBTFormatException("Unable to skip 'level' tag. File:" + chunksDatFile.getAbsolutePath());
                    }
                    break;
                case 0x04:
                    // ticks_left 标签是长整型 0x04，跳过 2+10+8 = 20 字节
                    if (bais.skip(20) != 20) {
                        // 读取失败
                        throw new NBTFormatException("Unable to skip 'ticks_left' tag. File:" + chunksDatFile.getAbsolutePath());
                    }
                    break;
                case 0x08:
                    // type 标签是字符串 0x08
                    // 先跳过前缀 2+4 = 6 字节
                    if (bais.skip(6) != 6) {
                        // 读取失败
                        throw new NBTFormatException("Unable to read 'type' tag, failed to skip prefix. File:" + chunksDatFile.getAbsolutePath());
                    }
                    // 随后两字节表示字串长度
                    byte[] readBuf = new byte[2];
                    if (bais.read(readBuf) != 2) {
                        // 读取失败
                        throw new NBTFormatException("Unable to read 'type' tag, incomplete string length. File:" + chunksDatFile.getAbsolutePath());
                    }
                    int strLen = (int) NumUtils.bigEndianToLong(readBuf, 2);
                    // 读取随后 strLen 个字节
                    byte[] strBuf = new byte[strLen];
                    if (bais.read(strBuf) != strLen) {
                        // 读取失败
                        throw new NBTFormatException("Unable to read 'type' tag, incomplete string data. File:" + chunksDatFile.getAbsolutePath());
                    }
                    chunkType = new String(strBuf);
                    break;
                case 0x00:
                    // 一个 Compound 读完了
                    ChunkCompound compound = getChunkCompound(coordBuf, chunkType);
                    chunkCompounds.add(compound);
                    coordBuf = null;
                    chunkType = null;
                    break;
                default:
                    // 其他标签出现，读取异常
                    throw new NBTFormatException("Unknown tag type: " + byteRead + ", file:" + chunksDatFile.getAbsolutePath());
            }
        }
        if (chunkCompounds.size() != listLen) {
            // 读取的标签数量和列表长度不一致
            throw new NBTFormatException("Chunk tickets list size mismatch, expected: " + listLen + ", actual: " + chunkCompounds.size() + ", file:" + chunksDatFile.getAbsolutePath());
        }
        return chunkCompounds.toArray(new ChunkCompound[0]);
    }

    /**
     * 根据坐标缓冲区和区块标签类型，生成 ChunkCompound 对象
     *
     * @param coordBuf  存有坐标的缓冲区
     * @param chunkType 区块标签类型
     * @return ChunkCompound 对象
     * @throws NBTFormatException NBT 解析失败时抛出
     */
    private ChunkCompound getChunkCompound(byte[] coordBuf, String chunkType) throws NBTFormatException {
        if (coordBuf == null || chunkType == null) {
            // 坐标和类型都不能为空
            throw new NBTFormatException("Unable to read chunk compound, incomplete data. File:" + chunksDatFile.getAbsolutePath());
        }
        // coordBuf 中高 4 字节是 x 坐标，低 4 字节是 z 坐标（和 1.21.5 之前不同）
        long chunkPos = NumUtils.bigEndianToLong(coordBuf, 8);
        int z = (int) (chunkPos & 0xFFFFFFFFL);
        int x = (int) (chunkPos >> 32 & 0xFFFFFFFFL);
        return new ChunkCompound(x, z, chunkType);
    }

    /**
     * 把 chunks.dat 文件中的强制加载区块存入受保护区块索引（1.21.5 的实现）
     *
     * @param protectedChunksIndex 受保护区块索引
     * @return 返回更新后的索引以及新增的区块数量
     * @throws IOException        如果读取失败会抛出此异常
     * @throws NBTFormatException 当 NBT 标签数据有误，无法读取到一些字段时抛出
     */
    @Override
    public ForcedChunksLoadResult loadForcedIntoSpatialIndex(ChunksSpatialIndex protectedChunksIndex) throws IOException {
        InputStream bais = new ByteArrayInputStream(chunksDatBytes);
        long chunksCount = 0;
        // 先找到 tickets 标签
        if (IOUtils.findAndSkipBytes(bais, NBTTagConstants.TICKETS_COMPOUND_TAG_BIN)) {
            // 后面 4 个字节表示 tickets 列表的长度
            byte[] numBuf = new byte[8];
            if (bais.read(numBuf, 0, 4) != 4) {
                // 读取失败
                throw new NBTFormatException("Tickets tag was found, but unable to read array size in file:" + chunksDatFile.getAbsolutePath());
            }
            long listSize = NumUtils.bigEndianToLong(numBuf, 4);
            // 读取 tickets 列表中所有 Compounds
            ChunkCompound[] chunkCompounds = parseChunkTicketsNBT(bais, (int) listSize);
            // 把所有 forced 区块坐标加入到索引
            for (ChunkCompound chunkCompound : chunkCompounds) {
                // 仅把 type 为 forced 的区块加入到索引
                // 实际上类型可能是 minecraft:forced，这里用子串匹配
                if (chunkCompound.getType().contains("forced")) {
                    protectedChunksIndex = protectedChunksIndex.add(chunkCompound.getGlobalX(), chunkCompound.getGlobalZ());
                    chunksCount++;
                }
            }
        }
        return new ForcedChunksLoadResult(protectedChunksIndex, chunksCount);
    }
}
