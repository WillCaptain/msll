# MSLL 评测统一总结

本文件是 MSLL 论文评测结果的统一入口，用于替代多份阶段性测试记录。

## 评测目标

- Correctness：不同 grammar 下是否可正确解析
- Performance：不同规模下吞吐表现与衰减特征
- Workflow：grammar 迭代效率与 ANTLR4 的对比
- Compatibility：G4 常用能力覆盖情况

## Correctness（当前结论）

- JSON：核心用例通过（对象、数组、混合类型、嵌套结构）
- JavaScript 子集：核心用例通过（变量、函数、表达式、控制流）
- Python 子集：已有语法与初步测试记录，建议继续补充更系统样本

## Performance（当前口径）

- 小规模输入：可满足 grammar 研发阶段验证需求
- 大规模输入：吞吐下降明显，存在规模衰减特征
- 结论：MSLL 适合“高迭代开发阶段”，不以生产吞吐为目标

## Workflow（核心价值）

- MSLL：编辑 grammar -> 直接运行
- ANTLR4：编辑 grammar -> 生成代码 -> 编译 -> 运行
- 在高频迭代场景下，MSLL 的反馈链路更短

## Compatibility（当前口径）

- 词法能力：mode/channel/fragment 等常用能力可用
- 语法能力：alternatives/quantifier/left recursion 等常见结构可用
- 复杂语义动作与更高阶场景仍需明确边界与补测

## 结果使用建议

1. 对外统一引用本文件，不再引用阶段性“进度日志”作为最终口径。
2. 图表和细粒度数据放在 `EVALUATION-GRAPHS.md`。
3. 论文正文数值更新时，以本文件为先，再同步主稿。
