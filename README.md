# PotatoPeeler
A simple tool to remove **unused chunks** from my Minecraft server, freeing disk space.   

Maybe a Java implementation of [Thanos](https://github.com/aternosorg/thanos).  

## Features  

...

## Outline

1. 工具工作原理，写清楚哪些区块会被移除，哪些不会（强制加载，InhabitedTime，数据过大）
2. 应该也能在本地存档上使用。
3. 支持的存档格式（仅 Java，Anvil 格式）
4. 使用方式（作为独立工具使用 / 作为服务器 Wrapper 使用）
5. unsuitable-scenarios
6. 对于原版存档格式可以像这样指定不同维度的数据目录：`world`, `world/DIM-1`。
7. 不会移除被 `/forceload` 强制加载的区块，以及区块保护名单（这个名单像 `.gitignore` 那样支持 `#` 注释）的使用（实现是 R* Tree）（保证 to > from）（写的是区块坐标）（每个维度都有一个，放在维度根目录下）
8. 配置项中带数值的一定要写单位。
9. 一些常见的使用用例 Examples

## 使用的开源项目

感谢开源开发者们的辛苦工作！

* [lz4-java](https://github.com/lz4/lz4-java)  
* [rtree2](https://github.com/davidmoten/rtree2)  

## 参考文档

1. [区域文件格式 - Minecraft Wiki](https://zh.minecraft.wiki/w/%E5%8C%BA%E5%9F%9F%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F)  
2. [强制加载区块存储格式 - Minecraft Wiki](https://zh.minecraft.wiki/w/%E5%BC%BA%E5%88%B6%E5%8A%A0%E8%BD%BD%E5%8C%BA%E5%9D%97%E5%AD%98%E5%82%A8%E6%A0%BC%E5%BC%8F)  
3. [Java版存档格式 - Minecraft Wiki](https://zh.minecraft.wiki/w/Java%E7%89%88%E5%AD%98%E6%A1%A3%E6%A0%BC%E5%BC%8F)  
4. [区块存储格式 - Minecraft Wiki](https://zh.minecraft.wiki/w/%E5%8C%BA%E5%9D%97%E5%AD%98%E5%82%A8%E6%A0%BC%E5%BC%8F)  
5. [NBT 二进制格式 - Minecraft Wiki](https://zh.minecraft.wiki/w/NBT%E6%A0%BC%E5%BC%8F?variant=zh-cn#%E4%BA%8C%E8%BF%9B%E5%88%B6%E6%A0%BC%E5%BC%8F)  