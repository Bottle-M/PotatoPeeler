# 测试笔记

## 测试不同压缩算法下是否能正常工作

关键在于怎么让服务端按特定压缩算法存储 Region。  

### 1. GZip  

Paper 端在 `paper-global.yml` 中可配置：  

```yaml
compression-format: GZIP
```

* 文档：https://docs.papermc.io/paper/reference/global-configuration#unsupported_settings_compression_format  

### 2. ZLib, LZ4, 无压缩  

采用 Mojang 的原版端比较好测试，直接修改 `server.properties` 中的：  

```yaml
# ZLib
region-file-compression=deflate

# LZ4 
region-file-compression=lz4

# Uncompressed
region-file-compression=none
```

* 文档：https://zh.minecraft.wiki/w/%E5%8C%BA%E5%9F%9F%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F