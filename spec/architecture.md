# MSLL 架构设计

## 模块职责

- Grammar 解析：读取 `.gm`，构建内部 grammar 表示
- Lexer：根据 lexer grammar 进行 tokenization
- Parser：基于多栈 LL 模型进行并行分支解析
- ParseTree：输出语法树供上层消费

## 核心组件

- `MyParserBuilder`：编排 grammar 加载与 parser 创建
- `RegexLexer`：模式匹配、mode/channel 支持
- `MsllParser`：分支栈管理、失败剪枝、成功路径收敛
- `MsllStack` / `MsllStacks`：栈对象与栈集合管理

## 关键工程策略

- 栈池化：降低短生命周期栈对象分配开销
- 标记剪枝：快速标记失效路径，减少无效继续解析
- 运行时 grammar 加载：避免“生成 + 编译”步骤
