# 开发备忘笔记 

## 插件大纲

1. 借助区块文件的 `InhabitedTime` 字段判断没有使用的区块。
2. 在**服务器运行过程**中**温柔地**、异步地逐渐**从硬盘上**移除未使用的区块，不显著影响游戏性能（**根据服务器压力自动调整删除频率**）。
3. 支持配置为哪些世界开启此功能。
4. 支持配置新建区块的赦免时间（新创建的区块在一段时间内不会被移除）。
5. 支持配置 `InhabitedTime` 阈值，阈值过高时在插件启动时**发出警告**。
6. 每隔一段时间在控制台吭个声，说明过去这段时间移除了多少无用区块。

## “未使用的区块”

“未使用的区块”指的是这样的区块：  

1. `InhabitedTime` 时间刻数**较低**。  
2. **不是刚刚被创建**的区块（通过文件系统中的 `createdTime` 判断）。  
3. **未被载入**内存（Unloaded）。  

## InhabitedTime 增长相关

1. 玩家在此区块停留时间，**每经过 1 tick**，附近有几个玩家，`InhabitedTime` 就增加多少。
   - 这里的“附近”似乎指的是在**刷怪半径范围**（单位是区块）内，见 [API 文档](https://bukkit.windit.net/javadoc/org/bukkit/Chunk.html#getInhabitedTime())以及 [minecraft-optimization 文档](https://github.com/YouHaveTrouble/minecraft-optimization?tab=readme-ov-file#mob-spawn-range)。   

2. 在服务器中，可能每个玩家**身边载入内存（Loaded）的区块**的 `InhabitedTime` 都会增加。不过增加上并不一定遵循第 1 点的“有几个玩家在附近就增加多少”，可能有遗留 BUG。详见这个[讨论贴](https://www.spigotmc.org/threads/chunk-inhabited-time-increase-unexpectedly.580847/)。  

... 待进一步研究  

## 技术难点以及可能的方案

1. 区块文件是以二进制数据密集存储在 Anvil 文件中的（`.mca`），在头部记录各个区块数据的起始偏移量。也就是说在删除必要的区块文件后，我还需要对文件进行紧凑（Compact）操作才能减少 `.mca` 文件的大小。  

2. 服务器可能会异步保存区块，也就是说不知道每个 `.mca` 文件在什么时候会被修改，为了防止区域损坏，在处理 `.mca` 的文件时应当**先创建一个拷贝**，在拷贝中删除无用的区块，然后再写回原文件。  
   为了防止这个期间 `.mca` 发生修改，需要对 `.mca` 文件计算多次 CRC32 摘要：  
   1. 创建拷贝前后各计算一次摘要 A，B，进行比对。
   2. 在修改拷贝后计算一次摘要 C，和 B 进行比对。
   3. 在把拷贝写入文件后再计算一次摘要 D，和 C 进行比对。
   4. 若以上比对结果均为相等，则正确修改了 `.mca` 文件。  

   

## 优化

1. 扫描 `.mca` 文件时避开玩家所在的区域（可以根据玩家坐标计算出来所在区域）。  
2. 一旦发现 `.mca` 文件中有**已经被载入内存**的区块，应当**立即跳过**对这个 `.mca` 文件的处理。  
   > 对于 `InhabitedTime` **低于阈值**的区块，可以用 [`isChunkLoaded(int x, int z)`](https://bukkit.windit.net/javadoc/org/bukkit/World.html#isChunkLoaded(int,int)) 来判断其是否被载入。    
   > 同时还要**监听** `ChunkLoadEvent` / `ChunkUnloadEvent` 事件，有区块载入 / 卸载时检查其是否位于当前在处理的区域内，如果是则停止处理本区域。


## 可能的问题  

1. 服务器运行过程中，若卸载了 `a.mca` 中的区块后，`a.mca` 被本插件修改了（移除了部分区块），那么服务器在不重启的情况下重新载入 `a.mca` 时能正常加载这个区域剩余的区块吗？  


## 必须要注意的地方（待写入 README）

1. 采用 Paper 端或者是以 Paper 为上游的服务端，在**世界配置文件**中一定不要修改 `fixed-chunk-inhabited-time` 这一项，不然会固定区块的 `InhabitedTime`，导致本插件失效。  

   * 文档：[World Configuration - PaperMC](https://docs.papermc.io/paper/reference/world-configuration#chunks_fixed_chunk_inhabited_time)    

2. 只能在**依靠种子自然生成的世界**中启动，如果是利用外部软件生成的地图，最好不要使用此插件的功能（否则移除的部分区块重新生成时，会按照种子进行生成，造成割裂）。

## 参考文档

* [区块格式 - Minecraft Wiki](https://wiki.biligame.com/mc/%E5%8C%BA%E5%9D%97%E6%A0%BC%E5%BC%8F)  
* [Chunk - Bukkit API](https://bukkit.windit.net/javadoc/org/bukkit/Chunk.html)  
* [Minecraft-Optimization](https://github.com/YouHaveTrouble/minecraft-optimization?tab=readme-ov-file)  
* [关于区块删除的讨论贴 - Bukkit Forum](https://bukkit.org/threads/delete-a-chunk.82993/)  