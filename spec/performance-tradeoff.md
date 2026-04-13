# MSLL 性能权衡

## 核心定位

MSLL 优先优化 grammar 开发迭代速度，而不是极限吞吐。

## 典型权衡

- 优势：运行时直接加载 grammar，减少开发等待时间
- 代价：解析吞吐通常低于代码生成型 parser（如 ANTLR4）

## 推荐策略

- 开发期：使用 MSLL 快速迭代 grammar
- 生产期：将稳定 grammar 迁移到 ANTLR4 以获得更高吞吐

## 结论

MSLL 不是替代 ANTLR4，而是补齐“语法研发阶段”的效率短板。
