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

2. 在服务器中，可能每个玩家**身边载入内存（Loaded）的区块**的 `InhabitedTime` 都会增加。不过增加上并不一定遵循第 1 点的“有几个玩家在附近就增加多少”，详见这个[讨论贴](https://www.spigotmc.org/threads/chunk-inhabited-time-increase-unexpectedly.580847/)。  

... 待进一步研究  

## 必须要注意的地方（待写入 README）

采用 Paper 端或者是以 Paper 为上游的服务端，在**世界配置文件**中一定不要修改 `fixed-chunk-inhabited-time` 这一项，不然会固定区块的 `InhabitedTime`，导致本插件失效。  

* 文档：[World Configuration - PaperMC](https://docs.papermc.io/paper/reference/world-configuration#chunks_fixed_chunk_inhabited_time)  

## 参考文档

* [区块格式 - Minecraft Wiki](https://wiki.biligame.com/mc/%E5%8C%BA%E5%9D%97%E6%A0%BC%E5%BC%8F)  
* [Chunk - Bukkit API](https://bukkit.windit.net/javadoc/org/bukkit/Chunk.html)  
* [Minecraft-Optimization](https://github.com/YouHaveTrouble/minecraft-optimization?tab=readme-ov-file)  