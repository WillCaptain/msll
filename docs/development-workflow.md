# MSLL 开发工作流

## 核心结论

在 grammar 高频迭代阶段，MSLL 的开发反馈链路更短：

- MSLL：编辑 `.gm` -> 直接运行测试
- ANTLR4：编辑 `.g4` -> 生成代码 -> 编译 -> 运行测试

## 适用阶段

- 语法探索/调试：MSLL
- 生产吞吐优化：ANTLR4

## 推荐实践

1. 在 MSLL 中快速收敛 grammar 设计
2. grammar 稳定后切换到 ANTLR4 生成生产 parser
3. 保持同一份 grammar 语义契约，避免双轨漂移
