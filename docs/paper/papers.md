# MSLL 论文文档分工与去重矩阵

本文件定义 `docs/paper/` 内各文档的职责边界，避免同一信息在多份阶段性文档中漂移。

## 主稿分层

| 文件 | 角色定位 | 使用场景 |
|---|---|---|
| `msll-full-paper.md` | 长文主稿（完整叙事） | 期刊/技术报告底稿 |
| `MSLL-SLE-5PAGE.md` | 5 页工具论文稿 | 工具论文短稿投稿 |
| `msll-journal.md` | 期刊版提纲与组织稿 | 结构调整与章节规划 |
| `00-abstract.md` ~ `08-conclusion.md` | 分章节可独立编辑稿 | 协作拆分写作 |

## 评测文档分层

| 文件 | 角色定位 | 说明 |
|---|---|---|
| `evaluation-summary.md` | 统一评测口径（唯一入口） | 汇总 correctness / performance / workflow / compatibility |
| `EVALUATION-GRAPHS.md` | 图表原始数据与图表草稿 | 作为图表数据附录 |

## 投稿文档分层

| 文件 | 角色定位 | 说明 |
|---|---|---|
| `submission-notes.md` | 投稿策略与执行清单（唯一入口） | venue 选择、里程碑、风险与补齐项 |
| `DEMO-VIDEO-SCRIPT.md` | 演示脚本 | 仅保留视频方案 |

## 去重原则

1. 相同结论只在一个“主入口”文档维护：
   - 评测结论归 `evaluation-summary.md`
   - 投稿结论归 `submission-notes.md`
2. 进度/日报类文档不再并行维护，改为归并后删除。
3. 如需保留实验原始细节，优先放在数据附录而不是新增“总结类”文档。
