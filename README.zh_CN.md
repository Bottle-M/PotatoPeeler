# PotatoPeeler

![Peeling Potato](./imgs/peeling.gif)  

这是一个简单的小工具，用于移除 Minecraft **Java 版**存档中的无用区块，以腾出部分硬盘空间。  

* 可以看作是 Java 语言实现的 [Thanos](https://github.com/aternosorg/thanos)。   

* 仅支持 [Anvil](https://minecraft.wiki/w/Anvil_file_format) 文件格式（自 Minecraft JE 1.2.1 起）。

* 较好的情况下可以把游戏存档的硬盘占用减少**超过 50%**。  

* 支持用区块坐标、区块坐标范围（支持通配符）配置每个世界的**受保护区块**，阻止某些区块被移除。

## 1. 工作原理

区块存储在 [Anvil](https://minecraft.wiki/w/Anvil_file_format) 文件内，Minecraft Java 版会为每个区块存储一个字段 `InhabitedTime`，以记录玩家在这个区块内停留的累计时间刻（tick）数。  

本工具通过 `InhabitedTime` 值筛选出玩家几乎没有停留的的区块，以进行移除。其既可以在本地游戏存档上使用，也可以在服务器存档上使用。  

> 玩家只要在某个区块的 生物生成距离<sup>[详见文档](https://minecraft.wiki/w/Spawn#Spawn_cycle)</sup> 内，这个区块的 `InhabitedTime` 就会增长。即玩家所在区块的周围区块的 `InhabitedTime` 往往也会增长。  

## 2. 使用前一定要注意

1. 如果你采用的是 Paper 端，或者是以 Paper 为上游的服务端（比如 Purpur），请**不要**在 Paper 的世界配置文件中把 `fixed-chunk-inhabited-time`<sup>[详见文档](https://docs.papermc.io/paper/reference/world-configuration#chunks_fixed_chunk_inhabited_time)</sup> 这一项设置为 $\ge 0$ 的值，否则区块的 `InhabitedTime` 会被固定，从而影响工具功能。

2. 如果你的存档是通过**模组 / 外部软件**编辑得到的，而不是人为建造的，其中的区块的 `InhabitedTime` 值难以预料，这种情况下不建议使用本工具。  

   > 当然，你也可以配置受保护的区块以防止某些区块被移除。

3. 本工具会对区域区块 Anvil 文件进行**原地处理**，发生的更改都是先写入一个临时文件再替换回去。尽管如此，还是建议时不时做一下备份。  

## 3. 安装

1. 确保你有相应版本的 JRE（Java 运行环境）。
2. 从 [Releases](https://github.com/Bottle-M/PotatoPeeler/releases/latest) 下载 `PotatoPeeler*.jar`，找个位置放着即可。

## 4. 使用

你可以在命令行运行此工具程序：  

```bash
java [jvmOptions] -jar PotatoPeeler*.jar 
    [--world-dirs <worldPath1>,<worldPath2>,...]
    [--server-jar <serverJarPath>]
    [--min-inhabited <ticks>]
    [--help]
    [--cool-down <minutes>]
    [--mca-modifiable-delay <minutes>]
    [--threads-num <number>]
    [--verbose]
    [--skip-peeler]
    [additionalOptions...]
```

| 标志项 | 说明 |
|---|---|
| `--help` | 显示帮助信息 |
| `--verbose` | 往日志中输出详细信息 |
| `--skip-peeler` | 直接跳过区块处理过程。若指定了 `--server-jar` 参数，会直接启动 Minecraft 服务端 |  


| 参数项 | 默认值 | 说明 |
|---|---|---|
| `--world-dirs` |  | 用逗号分隔的世界存档**路径**。<br><br> * 比如 `/opt/server/world,world_nether`，指定了两个世界目录，分别以绝对路径和相对路径的方式。程序会逐个处理这些世界。|
| `--min-inhabited` | `0` | 区块的 `InhabitedTime` 阈值（单位为 **tick**，20 ticks = 1 秒）。<br><br> * 某个区块的 `InhabitedTime` **低于或等于**这个值时，若其**未受保护**<sup>[见下方](#5-受保护的区块)</sup>，则**会被移除**。 |
| `--cool-down` | `0` | 距离上次区块处理**过去多久后**才能再次使用本工具（单位为**分钟**）。<br><br> * 注意是自上次所有指定世界的区块处理完成起计时。比如采用了 `--skip-peeler` 标志跳过了区块处理，就不计入在内。 |
| `--mca-modifiable-delay` | `0` | Anvil 文件（`.mca`）被创建多久后才能被修改（单位为**分钟**）。<br><br> * 此项用于防止 `.mca` 刚被创建不久，其中的区块就遭到移除。 |
| `--threads-num` | `10` | 采用多少线程并发（多核情况下可能能并行）处理一个世界中的 Anvil 文件。 |
| `--server-jar` |  | 指定 Minecraft 服务端 jar 包路径。<br><br> * 如果指定了可用的 jar 包，在本工具程序执行完后将会直接在当前 JVM 中运行此 jar 包。 |
| jvmOptions |  | JVM 参数。<br><br> * 如果指定了 `--server-jar`，会被服务端沿用。 |
| additionalOptions |  | 剩余参数。<br><br> * 如果指定了 `--server-jar`，这些参数会被传递给服务端。| 

* 注：对于原版存档格式，你可以这样指定各个世界维度： `--world-dirs world,world/DIM1,world/DIM-1`。  

  > 实际上本工具会采用广度优先方式搜索目录下的 `region` 子目录。  

## 5. 受保护的区块

受保护的区块**不会被移除**，主要包含以下两类：  

1. 世界中**强制加载**的区块（[/forceload](https://zh.minecraft.wiki/w/%E5%91%BD%E4%BB%A4/forceload)）。  
2. 自定义的受保护区块。  

### 5.1. 自定义受保护区块文件

你可以在世界[维度根目录](https://zh.minecraft.wiki/w/Java%E7%89%88%E5%AD%98%E6%A1%A3%E6%A0%BC%E5%BC%8F#%E5%AD%98%E6%A1%A3%E7%BB%93%E6%9E%84)（也就是和 `region` 在同一级目录中）下建立一个文本文件 `chunks.protected`，以指定要在这个世界中保护的区块。

<details>

<summary>点击查看这个文件所在的位置示例</summary>

```bash
world
├── DIM-1
│   ├── data
│   │   └── raids_end.dat
│   └── region
│       └── ...
├── DIM1
│   ├── data
│   │   └── raids_end.dat
│   ├── region
│   └── chunks.protected # 末地维度中要保护的区块
├── data
│   └── raids.dat
├── datapacks
├── entities
│   ├── r.-1.-1.mca
│   └── ...
│   
├── level.dat
├── level.dat_old
├── chunks.protected # 主世界维度中要保护的区块
├── playerdata
├── region
│   ├── r.-1.-1.mca
│   ├── r.-1.0.mca
│   ├── r.0.-1.mca
│   └── r.0.0.mca
└── session.lock
```

注：非原版服务端可能把 `DIM-1`, `DIM1` 这些维度单独存放在其他目录中，比较常见的则是 `world_nether/DIM-1`，`world_the_end/DIM1`。

</details>





## 6. 日志

## 7. 例子

### 7.1. 作为独立工具使用


### 7.2. 作为服务端前置程序使用

## Outline

1. 工具工作原理，写清楚哪些区块会被移除，哪些不会（强制加载，InhabitedTime，数据过大）
2. 使用方式（作为独立工具使用 / 作为服务器 Wrapper 使用）
3. 不会移除被 `/forceload` 强制加载的区块，以及区块保护名单（这个名单像 `.gitignore` 那样支持 `#` 注释）的使用（实现是 R* Tree）（保证 to > from）（写的是区块坐标）（每个维度都有一个，放在维度根目录下）
4. 配置项中带数值的在文档中一定要写单位。
5. 一些常见的使用用例 Examples（本地存档 / Vanilla）

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