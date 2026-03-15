# AIGC 创作与搜推服务

本仓库提供素材生成与创作管理、搜索窗口、推荐召回与消重能力，核心场景是“素材与创作的创建 + 搜推系统”。服务以 gRPC 为对外接口，底层整合 MySQL、Redis、Milvus、Nebula Graph、Kafka、OSS 等组件。

## 业务架构

- 素材生产与管理
  - 文生图/文生视频：调用 Minimax 接口生成素材，异步回调处理视频结果。
  - 素材入库、状态流转、封面生成（视频首帧预留）。
  - 素材上传到 OSS 并推送结果到下游。
- 创作管理
  - 创作生成、删除、按 ID/用户/好友关系获取创作列表。
  - 创作与素材的绑定、封面和正文存储。
- 搜索窗口
  - 关键词检索：使用向量检索 + 文本向量检索的混合搜索。
  - 搜索结果缓存：Redis 缓存关键词检索结果列表。
- 推荐系统
  - 四路召回 + 兜底召回 + 消重 + 简单排序，输出推荐列表。
  - 曝光记录写入 Redis，作为后续过滤依据。
- 智能体管理

## 技术架构

### 核心组件与数据流

- 接口层：Spring Boot + gRPC（`AigcService`）。
- 素材生成：WebClient 调用 Minimax；结果上传到 OSS；回调写回数据库并触发推送。
- 创作入库与索引：Kafka 消费创作变更，调用 DashScope(Qwen) 生成文本向量，并写入 Milvus；同时写入图关系存储。
- 搜索：Milvus 混合检索（rec_embeddings + title_embeddings），使用 RRF 进行融合排序，结果缓存到 Redis。
- 推荐召回：
  1. **Embedding 召回**：Milvus 向量近邻检索（基于 trigger 的 rec_embeddings）。
  2. **热度召回**：Redis ZSet 读取热度榜（Flink 统计产出写入 `trend:*`）。
  3. **Swing i2i 召回**：Nebula Graph 读取相似图（Hive/Spark 离线计算写入 `sim` 边）。
  4. **兜底召回**：随机向量在 Milvus 里兜底召回。
- 消重与过滤：多路召回结果取并集去重；按曝光历史进行过滤与补位。
