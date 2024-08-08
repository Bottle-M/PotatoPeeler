# 不适合使用此工具的场景  

## Paper 端的配置

采用 Paper 端或者是以 Paper 为上游的服务端，在**世界配置文件**中一定不要把 `fixed-chunk-inhabited-time` 这一项设为 $\ge 0$ 的值，不然会固定区块的 `InhabitedTime`，导致本插件失效。  

* 文档：[World Configuration - PaperMC](https://docs.papermc.io/paper/reference/world-configuration#chunks_fixed_chunk_inhabited_time)    

## 世界本身

### 1. 依靠外部软件生成的世界

如果是利用**外部软件 / 模组**编辑得到的地图，最好不要使用此插件的功能。  
* 一方面，移除的部分区块重新生成时，会按照种子进行生成，造成割裂；
* 另一方面，这些软件可能没有正确设置区块的 `InhabitedTime` 数据，导致一些区块被错误地移除。

> 当然，可以使用本工具的**受保护区块清单**来指定不被移除的部分。
