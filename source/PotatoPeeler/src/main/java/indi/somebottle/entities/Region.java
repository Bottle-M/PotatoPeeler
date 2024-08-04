package indi.somebottle.entities;

import indi.somebottle.exceptions.RegionChunkInitializedException;
import indi.somebottle.exceptions.RegionPosNotFoundException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Region {
    // 此区域的文件对象
    private final File regionFile;
    // 此区域的 x 坐标
    private final long regionX;
    // 此区域的 z 坐标
    private final long regionZ;
    /**
     * 一个区域内有 32×32=1024 个区块
     */
    private final Chunk[][] chunks = new Chunk[32][32];
    /**
     * 存储每个区块最后一次被修改的秒级时间戳
     */
    private final long[][] chunkModifiedTimes = new long[32][32];
    /**
     * 存储非 null 的 Chunk 对象
     */
    private final List<Chunk> existingChunks = new ArrayList<>();


    public Region(File regionFile) throws RegionPosNotFoundException {
        this.regionFile = regionFile;
        // 从文件名中解析出 x, z
        // 文件名格式： r.<regionX>.<regionZ>.mca
        String[] mcaNameParts = regionFile.getName().split("\\.");
        try {
            this.regionX = Long.parseLong(mcaNameParts[1]);
            this.regionZ = Long.parseLong(mcaNameParts[2]);
        } catch (Exception e) {
            throw new RegionPosNotFoundException("Invalid region file name: " + regionFile.getName());
        }
    }

    /**
     * 获得本区域的 X 坐标
     *
     * @return X 坐标
     */
    public long getRegionX() {
        return regionX;
    }

    /**
     * 获得本区域的 Z 坐标
     *
     * @return Z 坐标
     */
    public long getRegionZ() {
        return regionZ;
    }

    /**
     * 获得本区域非 null 的 Chunk 对象列表
     *
     * @return 非 null 的 Chunk 对象 List
     */
    public List<Chunk> getExistingChunks() {
        return existingChunks;
    }

    /**
     * 获得指定坐标的 Chunk （局部坐标，从 (0,0) 到 (31,31)）
     *
     * @param x 局部坐标 x
     * @param z 局部坐标 z
     * @return Chunk 对象，可能为 null
     */
    public Chunk getChunkAt(int x, int z) {
        return chunks[x][z];
    }

    /**
     * 初始化指定坐标的 Chunk （局部坐标，从 (0,0) 到 (31,31)）
     *
     * @param x     局部坐标 x
     * @param z     局部坐标 z
     * @param chunk Chunk 对象
     * @throws RegionChunkInitializedException 如果该区块已经被初始化则会抛出异常
     */
    public void initChunkAt(int x, int z, Chunk chunk) throws RegionChunkInitializedException {
        if (chunk == null)
            return;
        if (chunks[x][z] != null)
            throw new RegionChunkInitializedException("Chunk at " + x + ", " + z + " has already been initialized.");
        chunks[x][z] = chunk;
        // 记录非 null 的 Chunk
        existingChunks.add(chunk);
    }

    /**
     * 获得此区域的文件对象
     *
     * @return 文件对象
     */
    public File getRegionFile() {
        return regionFile;
    }

    /**
     * 获得指定坐标区块最后被修改时间戳
     *
     * @param x 局部坐标 x
     * @param z 局部坐标 z
     * @return 最后修改时间戳
     */
    public long getChunkModifiedTimeAt(int x, int z) {
        return chunkModifiedTimes[x][z];
    }

    /**
     * 设定指定坐标区块最后被修改时间戳
     *
     * @param x                局部坐标 x
     * @param z                局部坐标 z
     * @param lastModifiedTime 最后修改时间戳
     */
    public void setChunkModifiedTimeAt(int x, int z, long lastModifiedTime) {
        chunkModifiedTimes[x][z] = lastModifiedTime;
    }
}
