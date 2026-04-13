# MSLL SDK 集成说明

## 模块定位

MSLL 提供运行时解析能力：

- 输入：lexer/parser grammar 文件（`.gm`）
- 输出：`ParserTree`

不生成目标语言代码，不替代生产级编译器前端构建链路。

## 核心类

- `MyParserBuilder`：加载 grammar，创建 parser 实例
- `MyParser`：执行解析
- `ParserTree`：解析树输出
- `RegexLexer`：词法引擎
- `MsllParser`：多栈 LL 解析核心

## 典型调用流程

```java
MyParserBuilder builder = new MyParserBuilder("x.parser.gm", "x.lexer.gm");
MyParser parser = builder.createParser(sourceCode);
ParserTree tree = parser.parse();
```

## 推荐使用策略

- 开发 grammar：优先 MSLL（改 grammar 即测）
- 生产大规模吞吐：优先 ANTLR4 生成器
- 推荐混合流程：MSLL 做语法迭代，ANTLR4 做线上部署
