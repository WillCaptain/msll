# MSLL 5 分钟快速开始

MSLL 是运行时解析引擎：直接读取 `.gm` grammar 文件并解析输入，无需代码生成。

## 1. 引入依赖

```xml
<dependency>
  <groupId>org.twelve</groupId>
  <artifactId>msll</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

## 2. 准备 grammar

- 词法：`myLang.lexer.gm`
- 语法：`myLang.parser.gm`

示例可参考：

- `lexer-grammar.md`
- `parser-grammar.md`

## 3. 运行解析

```java
MyParserBuilder builder = new MyParserBuilder("myLang.parser.gm", "myLang.lexer.gm");
MyParser parser = builder.createParser("let x = 1 + 2;");
ParserTree tree = parser.parse();
```

## 4. 下一步

- 了解 API 和集成方式：`sdk-integration.md`
- 对比开发流程：`development-workflow.md`
