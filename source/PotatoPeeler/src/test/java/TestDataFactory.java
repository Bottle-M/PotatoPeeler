import indi.somebottle.constants.NBTTagConstants;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Builds small binary fixtures for unit tests so they do not depend on external Minecraft worlds.
 * 为单元测试构建小型二进制夹具，避免依赖外部 Minecraft 世界存档。
 */
final class TestDataFactory {
    /**
     * The fixed Anvil sector size used by region files.
     * Region 文件使用的固定 Anvil 扇区大小。
     */
    private static final int SECTOR_SIZE = 4096;

    /**
     * Utility class.
     * 工具类，不允许实例化。
     */
    private TestDataFactory() {
    }

    /**
     * Describes a single chunk entry to be embedded into a generated region file.
     * 描述一个要写入生成 Region 文件中的单个 Chunk 条目。
     */
    static final class RegionChunkSpec {
        /**
         * Chunk local x coordinate inside the region.
         * Chunk 在 Region 内的局部 X 坐标。
         */
        final int localX;
        /**
         * Chunk local z coordinate inside the region.
         * Chunk 在 Region 内的局部 Z 坐标。
         */
        final int localZ;
        /**
         * Chunk payload compression type as stored in the Anvil file.
         * Anvil 文件中存储的 Chunk 负载压缩类型。
         */
        final int compressionType;
        /**
         * InhabitedTime value to encode in the chunk NBT.
         * 要写入 Chunk NBT 的 InhabitedTime 数值。
         */
        final long inhabitedTime;
        /**
         * Timestamp value to encode in the region timestamp table.
         * 要写入 Region 时间戳表的时间戳数值。
         */
        final long timestamp;

        /**
         * Creates a chunk specification for a generated region fixture.
         * 创建一个用于生成 Region 测试夹具的 Chunk 规格。
         *
         * @param localX chunk local x coordinate
         *               Chunk 局部 X 坐标
         * @param localZ chunk local z coordinate
         *               Chunk 局部 Z 坐标
         * @param compressionType Anvil compression type
         *                        Anvil 压缩类型
         * @param inhabitedTime chunk inhabited time
         *                      Chunk 的 InhabitedTime
         * @param timestamp chunk timestamp table value
         *                  时间戳表中的时间戳值
         */
        RegionChunkSpec(int localX, int localZ, int compressionType, long inhabitedTime, long timestamp) {
            this.localX = localX;
            this.localZ = localZ;
            this.compressionType = compressionType;
            this.inhabitedTime = inhabitedTime;
            this.timestamp = timestamp;
        }
    }

    /**
     * Describes a single ticket compound for a generated modern chunk ticket file.
     * 描述一个要写入新版 chunk ticket 文件的单个 ticket compound。
     */
    static final class ChunkTicketSpec {
        /**
         * Global chunk x coordinate.
         * 全局 Chunk X 坐标。
         */
        final int chunkX;
        /**
         * Global chunk z coordinate.
         * 全局 Chunk Z 坐标。
         */
        final int chunkZ;
        /**
         * Ticket type string stored in the NBT compound.
         * 写入 NBT compound 的 ticket 类型字符串。
         */
        final String type;

        /**
         * Creates a chunk-ticket specification.
         * 创建一个 Chunk ticket 规格。
         *
         * @param chunkX global chunk x coordinate
         *               全局 Chunk X 坐标
         * @param chunkZ global chunk z coordinate
         *               全局 Chunk Z 坐标
         * @param type ticket type string
         *             ticket 类型字符串
         */
        ChunkTicketSpec(int chunkX, int chunkZ, String type) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.type = type;
        }
    }

    /**
     * Writes a minimal valid region file containing the supplied chunk definitions.
     * 写入一个包含给定 Chunk 定义的最小可用 Region 文件。
     *
     * @param path destination region file path
     *             目标 Region 文件路径
     * @param chunks chunk entries to include
     *               需要写入的 Chunk 条目
     * @return the written path
     *         已写入的路径
     * @throws IOException if fixture generation fails
     *                     当夹具生成失败时抛出
     */
    static Path writeRegionFile(Path path, RegionChunkSpec... chunks) throws IOException {
        Files.createDirectories(path.getParent());

        byte[] header = new byte[SECTOR_SIZE * 2];
        ByteArrayOutputStream chunkSectors = new ByteArrayOutputStream();
        int nextSectorOffset = 2;

        for (RegionChunkSpec chunk : chunks) {
            byte[] chunkNbt = createChunkNbt(chunk.inhabitedTime);
            byte[] payload = compress(chunkNbt, chunk.compressionType);
            int chunkDataLength = 1 + payload.length;
            int totalChunkBytes = 4 + chunkDataLength;
            int sectorsOccupied = Math.max(1, (int) Math.ceil(totalChunkBytes / (double) SECTOR_SIZE));

            int entryOffset = (chunk.localX + chunk.localZ * 32) * 4;
            writeMedium(header, entryOffset, nextSectorOffset);
            header[entryOffset + 3] = (byte) sectorsOccupied;
            writeInt(header, SECTOR_SIZE + entryOffset, chunk.timestamp);

            ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream(sectorsOccupied * SECTOR_SIZE);
            try (DataOutputStream out = new DataOutputStream(chunkBuffer)) {
                out.writeInt(chunkDataLength);
                out.writeByte(chunk.compressionType);
                out.write(payload);
            }

            byte[] chunkBytes = chunkBuffer.toByteArray();
            chunkSectors.write(chunkBytes);
            int padding = sectorsOccupied * SECTOR_SIZE - chunkBytes.length;
            if (padding > 0) {
                chunkSectors.write(new byte[padding]);
            }
            nextSectorOffset += sectorsOccupied;
        }

        try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            out.write(header);
            out.write(chunkSectors.toByteArray());
        }
        return path;
    }

    /**
     * Writes a legacy {@code chunks.dat} file containing forced chunks encoded as a long array.
     * 写入一个旧版 {@code chunks.dat} 文件，其中强制加载区块以 long 数组形式编码。
     *
     * @param path destination file path
     *             目标文件路径
     * @param dataVersion data version tag to encode
     *                    要编码的数据版本号
     * @param chunkCoords chunk coordinates expressed as {@code [x, z]} pairs
     *                    以 {@code [x, z]} 表示的 Chunk 坐标对
     * @return the written path
     *         已写入的路径
     * @throws IOException if fixture generation fails
     *                     当夹具生成失败时抛出
     */
    static Path writeLegacyChunksDat(Path path, int dataVersion, long[][] chunkCoords) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(raw)) {
            out.writeByte(0x0A);
            out.writeShort(0);
            out.write(NBTTagConstants.DATA_VERSION_TAG_BIN);
            out.writeInt(dataVersion);
            out.write(NBTTagConstants.FORCED_TAG_BIN);
            out.writeInt(chunkCoords.length);
            for (long[] coord : chunkCoords) {
                int chunkX = (int) coord[0];
                int chunkZ = (int) coord[1];
                long chunkPos = ((long) chunkZ << 32) | (chunkX & 0xFFFFFFFFL);
                out.writeLong(chunkPos);
            }
            out.writeByte(0x00);
        }
        return writeGzipFile(path, raw.toByteArray());
    }

    /**
     * Writes a modern {@code chunk_tickets.dat} file containing ticket compounds.
     * 写入一个包含 ticket compound 的新版 {@code chunk_tickets.dat} 文件。
     *
     * @param path destination file path
     *             目标文件路径
     * @param dataVersion data version tag to encode
     *                    要编码的数据版本号
     * @param tickets ticket compounds to encode
     *                需要编码的 ticket compound
     * @return the written path
     *         已写入的路径
     * @throws IOException if fixture generation fails
     *                     当夹具生成失败时抛出
     */
    static Path writeModernChunkTicketsDat(Path path, int dataVersion, ChunkTicketSpec... tickets) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(raw)) {
            out.writeByte(0x0A);
            out.writeShort(0);
            out.write(NBTTagConstants.DATA_VERSION_TAG_BIN);
            out.writeInt(dataVersion);
            out.write(NBTTagConstants.TICKETS_COMPOUND_TAG_BIN);
            out.writeInt(tickets.length);
            for (ChunkTicketSpec ticket : tickets) {
                writeChunkTicketCompound(out, ticket);
            }
            out.writeByte(0x00);
        }
        return writeGzipFile(path, raw.toByteArray());
    }

    /**
     * Writes a deliberately malformed legacy {@code chunks.dat} file whose declared long-array size
     * is larger than the actual payload.
     * 写入一个故意构造的损坏旧版 {@code chunks.dat} 文件，其声明的 long 数组长度大于真实负载长度。
     *
     * @param path destination file path
     *             目标文件路径
     * @param dataVersion data version tag to encode
     *                    要编码的数据版本号
     * @param declaredCount long-array size to declare without providing data
     *                      只声明但不提供实际数据的 long 数组长度
     * @return the written path
     *         已写入的路径
     * @throws IOException if fixture generation fails
     *                     当夹具生成失败时抛出
     */
    static Path writeTruncatedLegacyChunksDat(Path path, int dataVersion, int declaredCount) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(raw)) {
            out.writeByte(0x0A);
            out.writeShort(0);
            out.write(NBTTagConstants.DATA_VERSION_TAG_BIN);
            out.writeInt(dataVersion);
            out.write(NBTTagConstants.FORCED_TAG_BIN);
            out.writeInt(declaredCount);
            out.writeByte(0x00);
        }
        return writeGzipFile(path, raw.toByteArray());
    }

    /**
     * Builds the minimal chunk NBT payload required by the production parser to extract
     * {@code InhabitedTime}.
     * 构造生产代码提取 {@code InhabitedTime} 所需的最小 Chunk NBT 负载。
     *
     * @param inhabitedTime chunk inhabited time value to encode
     *                      需要编码的 InhabitedTime 数值
     * @return encoded NBT payload
     *         编码后的 NBT 负载
     * @throws IOException if encoding fails
     *                     当编码失败时抛出
     */
    private static byte[] createChunkNbt(long inhabitedTime) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(raw)) {
            out.writeByte(0x0A);
            out.writeShort(0);
            out.write(NBTTagConstants.INHABITED_TIME_TAG_BIN);
            out.writeLong(inhabitedTime);
            out.writeByte(0x00);
        }
        return raw.toByteArray();
    }

    /**
     * Writes one ticket compound in the simplified structure expected by the production parser.
     * 按生产代码解析器期望的简化结构写入单个 ticket compound。
     *
     * @param out destination NBT stream
     *            目标 NBT 输出流
     * @param ticket chunk-ticket specification to encode
     *               需要编码的 ticket 规格
     * @throws IOException if encoding fails
     *                     当编码失败时抛出
     */
    private static void writeChunkTicketCompound(DataOutputStream out, ChunkTicketSpec ticket) throws IOException {
        out.writeByte(0x0B);
        writeString(out, "chunk_pos");
        out.writeInt(2);
        out.writeInt(ticket.chunkX);
        out.writeInt(ticket.chunkZ);

        out.writeByte(0x03);
        writeString(out, "level");
        out.writeInt(33);

        out.writeByte(0x04);
        writeString(out, "ticks_left");
        out.writeLong(0L);

        out.writeByte(0x08);
        writeString(out, "type");
        writeString(out, ticket.type);

        out.writeByte(0x00);
    }

    /**
     * Writes a UTF-8 string using the NBT string wire format.
     * 使用 NBT 字符串线性格式写入一个 UTF-8 字符串。
     *
     * @param out destination NBT stream
     *            目标 NBT 输出流
     * @param value string value to encode
     *              需要编码的字符串值
     * @throws IOException if encoding fails
     *                     当编码失败时抛出
     */
    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    /**
     * Compresses a chunk payload with the same compression identifiers used by region files.
     * 使用与 Region 文件一致的压缩类型标识压缩 Chunk 负载。
     *
     * @param input uncompressed chunk NBT payload
     *              未压缩的 Chunk NBT 负载
     * @param compressionType region-file compression type
     *                        Region 文件压缩类型
     * @return compressed payload bytes
     *         压缩后的负载字节
     * @throws IOException if compression fails
     *                     当压缩失败时抛出
     */
    private static byte[] compress(byte[] input, int compressionType) throws IOException {
        if (compressionType == 3) {
            return Arrays.copyOf(input, input.length);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream compressor;
        switch (compressionType) {
            case 1:
                compressor = new GZIPOutputStream(baos);
                break;
            case 2:
                compressor = new DeflaterOutputStream(baos);
                break;
            case 4:
                compressor = new LZ4BlockOutputStream(baos);
                break;
            default:
                throw new IllegalArgumentException("Unsupported compression type in test data: " + compressionType);
        }
        try (OutputStream out = compressor) {
            out.write(input);
        }
        return baos.toByteArray();
    }

    /**
     * Writes a GZip-compressed file.
     * 写入一个 GZip 压缩文件。
     *
     * @param path destination file path
     *             目标文件路径
     * @param payload uncompressed payload
     *                未压缩负载
     * @return the written path
     *         已写入的路径
     * @throws IOException if writing fails
     *                     当写入失败时抛出
     */
    private static Path writeGzipFile(Path path, byte[] payload) throws IOException {
        Files.createDirectories(path.getParent());
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
            out.write(payload);
        }
        return path;
    }

    /**
     * Writes a 3-byte big-endian integer into a byte array.
     * 向字节数组写入一个 3 字节大端整数。
     *
     * @param target destination byte array
     *               目标字节数组
     * @param offset write offset
     *               写入偏移量
     * @param value integer value to encode
     *              需要编码的整数值
     */
    private static void writeMedium(byte[] target, int offset, int value) {
        target[offset] = (byte) (value >> 16);
        target[offset + 1] = (byte) (value >> 8);
        target[offset + 2] = (byte) value;
    }

    /**
     * Writes a 4-byte big-endian integer into a byte array.
     * 向字节数组写入一个 4 字节大端整数。
     *
     * @param target destination byte array
     *               目标字节数组
     * @param offset write offset
     *               写入偏移量
     * @param value integer value to encode
     *              需要编码的整数值
     */
    private static void writeInt(byte[] target, int offset, long value) {
        target[offset] = (byte) (value >> 24);
        target[offset + 1] = (byte) (value >> 16);
        target[offset + 2] = (byte) (value >> 8);
        target[offset + 3] = (byte) value;
    }
}
