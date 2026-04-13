<div align="center">

# MSLL — Multi-Stack LL Parser

**Parse any language. Ship no generated code. Stay in Java.**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/license/mit)
[![Java](https://img.shields.io/badge/Java-11%2B-orange.svg)](https://openjdk.org/)
[![G4 Compatible](https://img.shields.io/badge/G4-Compatible-green.svg)](#g4-compatibility)

</div>

---

## What is MSLL?

MSLL is a **runtime parsing engine** that reads your grammar file and parses source code — no code generation, no tool chain, no recompilation.  Write a `.gm` file (ANTLR4/G4 syntax), point MSLL at it, and you have a fully functional parser in three lines of Java.

```java
// That's it. Seriously.
MyParserBuilder builder = new MyParserBuilder("myLang.gm", "myLangLexer.gm");
MyParser        parser  = builder.createParser("let x = 1 + 2;");
ParseTree       tree    = parser.parse();
```

---

## Why MSLL?

### No code generation

ANTLR4 is fantastic for production — but it forces a **write grammar → run tool → compile generated code → test** cycle.  Every grammar change means regenerating and recompiling.

MSLL eliminates that cycle entirely.  Grammar changes are **instant**.

```
ANTLR workflow:           MSLL workflow:
  edit .g4                  edit .gm
    ↓                          ↓
  run antlr4 tool           (nothing)
    ↓                          ↓
  compile generated .java   run your tests ← instant feedback
    ↓
  run your tests
```

### Tiny footprint

The entire MSLL engine — parser, lexer, grammar loader, parse tree — is
**under 4 000 lines of Java** with zero heavyweight dependencies.  Add one JAR and you're done.

### Elegant internals

The core idea is beautiful in its simplicity:

> When the parser reaches an ambiguous choice, **clone the stack** for each
> possibility.  Continue parsing all branches in parallel.  
> Discard any branch that fails to match.  
> The branch that survives is the answer.

No DFA construction.  No look-ahead tables.  No grammar transformations.  
Left recursion works naturally.  Ambiguous grammars work naturally.

```
input: "a + b * c"

Stack 0: expr → expr + expr          Stack 1: expr → expr * expr
         ↓ matches "a + …"                    ↓ does not match "a + …"
         survives ✓                            pruned ✗
```

### G4 compatible

You already know G4?  You already know MSLL.  
Reuse your ANTLR4 grammar files with minimal changes.

---

## Quick Start

### 1. Add the dependency

```xml
<!-- Maven -->
<dependency>
    <groupId>org.twelve</groupId>
    <artifactId>msll</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Write a lexer grammar  — `myLang.lexer.gm`

```antlr
lexer grammar MyLangLexer;

channels { HIDDEN }

WS      : [ \t\r\n]+ -> channel(HIDDEN);
Let     : 'let';
Return  : 'return';
NUMBER  : [0-9]+ ('.' [0-9]+)? ;
ID      : [a-zA-Z_][a-zA-Z0-9_]* ;
Plus    : '+';
Star    : '*';
Eq      : '=';
Semi    : ';';
LParen  : '(';
RParen  : ')';
```

### 3. Write a parser grammar — `myLang.parser.gm`

```antlr
parser grammar MyLangParser;
options { tokenVocab = MyLangLexer; }

program    : statement+ EOF ;
statement  : letDecl | returnStmt ;
letDecl    : 'let' ID '=' expr ';' ;
returnStmt : 'return' expr ';' ;
expr       : expr ('+' | '*') expr
           | '(' expr ')'
           | NUMBER
           | ID
           ;
```

### 4. Parse

```java
MyParserBuilder builder = new MyParserBuilder("myLang.parser.gm", "myLang.lexer.gm");
MyParser        parser  = builder.createParser("let x = 1 + 2 * 3;");
ParseTree       tree    = parser.parse();

// Walk the tree
tree.root().nodes().forEach(node -> System.out.println(node.name()));
```

---

## G4 Compatibility

MSLL uses the same `.gm` format as ANTLR4's `.g4`.  The features below
are all supported out of the box.

### Lexer

| Feature | Example | Supported |
|---------|---------|:---------:|
| String literals | `'let'` | ✅ |
| Regex patterns | `[a-zA-Z_]\w*` | ✅ |
| `/"…"/` Java regex | `/"(\+\|-)?[0-9]+"/ ` | ✅ |
| Quantifiers `* + ? *? +?` | `'/*' .*? '*/'` | ✅ |
| Character classes `[…]` | `[0-9a-fA-F]` | ✅ |
| Negated classes `~[…]` | `~[\r\n]` | ✅ |
| Grouping `(…)` | `('x'\|'X')` | ✅ |
| `fragment` rules | `fragment Digit : [0-9];` | ✅ |
| `-> channel(NAME)` | `-> channel(HIDDEN)` | ✅ |
| `-> skip` | `WS : ' '+ -> skip;` | ✅ |
| `-> type(TOKEN)` | `-> type(BackTick), popMode` | ✅ |
| `-> pushMode(M)` / `-> popMode` | template strings | ✅ |
| `mode NAME;` declarations | `mode TEMPLATE;` | ✅ |
| Channels block | `channels { HIDDEN, ERROR }` | ✅ |
| `EOF` terminal | `program : stmt* EOF ;` | ✅ |

### Parser

| Feature | Example | Supported |
|---------|---------|:---------:|
| Alternatives `\|` | `a \| b \| c` | ✅ |
| Sequences | `'(' expr ')'` | ✅ |
| Quantifiers `? * +` | `stmt*` | ✅ |
| Inline grouping | `('a' \| 'b')+` | ✅ |
| String literals | `'return'` | ✅ |
| Semantic predicates | `{notLineTerminator()}?` | ✅ |
| Alternative labels `#` | `expr '+' expr # addExpr` | ✅ |
| Associativity | `<assoc=right> a '**' a` | ✅ |
| Left recursion | `expr : expr '+' expr` | ✅ |
| `options { tokenVocab }` | links parser to lexer | ✅ |

---

## How Multi-Stack LL Works

Traditional LL(k) parsers fail on ambiguous grammars and left recursion
because they pre-compute a single deterministic choice table.

MSLL takes a different approach: **defer the decision, explore in parallel**.

```
Parsing:  a * b + c

         ┌─ Stack A: expr → expr * expr ─────────────────────── ✓
         │
start ───┤
         │
         └─ Stack B: expr → expr + expr  ← fails on '*' at pos 1 ✗
```

At any point of ambiguity, the current parse state is **cloned**.  Each
clone pursues a different production.  Stacks that cannot consume the next
token are pruned immediately.  At the end, exactly one stack (or zero,
indicating a parse error) survives.

This naturally handles:
- **Left recursion** — the left-recursive branch simply gets a chance to consume
- **Operator precedence** — naturally emerges from which branch survives
- **Ambiguous grammars** — first surviving branch wins (configurable)

The algorithm is O(n × k) where k is the maximum simultaneous live stacks —
typically a very small constant for well-written grammars.

---

## Performance

MSLL is designed for **developer productivity**, not raw throughput.
That said, it is not slow:

| Grammar | Tokens | Time | Throughput |
|---------|--------|------|-----------|
| JavaScript (full ES2020) | 447 000 | ~20 s | ~22 K tok/s |
| Outline DSL | ~5 000 | < 1 s | fast enough |

For production parsing of large files, generate an ANTLR parser from your
stable `.gm` file.  For prototyping, debugging, DSL interpretation, and
IDE tooling, MSLL is the right tool.

---

## Lexer Mode Example — Template Strings

MSLL's mode-aware lexer correctly handles JavaScript template literals,
where different sets of tokens are active depending on context:

```antlr
lexer grammar TemplateLexer;

// DEFAULT_MODE
BackTick    : '`'  -> pushMode(TEMPLATE);
ID          : [a-zA-Z_][a-zA-Z0-9_]* ;
WS          : [ \t\n]+ -> skip ;

mode TEMPLATE;
BackTickEnd : '`'  -> type(BackTick), popMode;
ExprStart   : '${'  -> pushMode(DEFAULT_MODE);
StrContent  : ~[`\\$]+;
```

```
Input: `hello ${name}!`

DEFAULT_MODE  → BackTick "`"
TEMPLATE mode → StrContent "hello "
TEMPLATE mode → ExprStart "${"
DEFAULT_MODE  → ID "name"
TEMPLATE mode → StrContent "!"
DEFAULT_MODE  → BackTick "`" (relabelled from BackTickEnd)
```

---

## Architecture

```
                     ┌─────────────────────────────────────────┐
  Grammar files      │              MSLL Engine                 │
  ┌──────────┐       │  ┌─────────┐  ┌────────┐  ┌─────────┐  │
  │ *.lexer  │──────▶│  │ Lexer   │─▶│ Parser │─▶│  Tree   │  │
  │   .gm    │       │  │ Builder │  │ Builder│  │ Builder │  │
  └──────────┘       │  └─────────┘  └────────┘  └─────────┘  │
  ┌──────────┐       │       │            │                     │
  │ *.parser │──────▶│  ┌────▼────┐  ┌───▼──────────────────┐  │
  │   .gm    │       │  │RegexLexer│  │  MsllParser           │  │
  └──────────┘       │  │(mode-   │  │  (multi-stack LL)     │  │
                     │  │ aware)  │  └──────────────────────┘  │
  Source code        │  └─────────┘                             │
  ┌──────────┐       │       │                                  │
  │  input   │──────▶│  Token stream                            │
  └──────────┘       └─────────────────────────────────────────┘
                                          │
                                    ┌─────▼──────┐
                                    │ Parse Tree  │
                                    └────────────┘
```

---

## Documentation

| Document | Description |
|----------|-------------|
| [Docs Index](docs/index.md) | 用户 / SDK / 二次开发文档入口 |
| [Design Index](spec/index.md) | 架构 / 解析模型 / 性能权衡入口 |
| [Lexer Grammar Reference](docs/lexer-grammar.md) | Token rules, fragments, modes, commands |
| [Parser Grammar Reference](docs/parser-grammar.md) | Rules, quantifiers, predicates, left recursion |
| [Paper Index](docs/paper/index.md) | 论文主稿、评测与投稿材料 |

---

## Comparison with ANTLR4

| | MSLL | ANTLR4 |
|--|------|--------|
| **Code generation** | ❌ None — runtime only | ✅ Generates Java/Python/C#/… |
| **Grammar syntax** | G4-compatible `.gm` | `.g4` |
| **Left recursion** | ✅ Native (multi-stack) | ✅ Rewritten internally |
| **Grammar changes** | Instant (reload file) | Regenerate + recompile |
| **Footprint** | < 4 000 lines, 1 JAR | Large generated code |
| **Performance** | Dev-speed (~22K tok/s) | Production-speed (millions tok/s) |
| **Best for** | Prototyping, DSLs, tooling | Production compilers |

---

## Use Cases

- **Language prototyping** — iterate on grammar design without a build step
- **Domain-specific languages** — ship a grammar file, not a generated parser
- **IDE tooling** — parse code on-the-fly for syntax highlighting and analysis
- **Grammar debugging** — inspect which stack survives at each step
- **Education** — study LL parsing without the complexity of DFA generation
- **Test harness** — validate grammar changes in milliseconds

---

## License

MIT License — free for academic and commercial use.  
See [LICENSE](https://opensource.org/license/mit) for details.
