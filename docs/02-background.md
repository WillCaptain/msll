# 2. Background

This section provides background on LL parsing, parser generators, and the challenges that motivate MSLL.

## 2.1 LL Parsing

LL parsing is a top-down parsing technique where the parser constructs a parse tree from the root down to the leaves, reading input from left to right (the first L) and producing a leftmost derivation (the second L). LL parsers are intuitive and closely match the structure of recursive descent parsers written by hand.

An LL(k) parser uses k tokens of lookahead to decide which production rule to apply. For example, given the grammar:

```
expr : expr '+' term
     | term
     ;
term : NUMBER
     | '(' expr ')'
     ;
```

An LL(1) parser would look at the next token to decide whether to parse `expr '+' term` or just `term`. However, this grammar has a problem: it contains **left recursion** (`expr` appears as the first symbol in its own production). LL parsers cannot handle left recursion directly because they would enter infinite recursion.

Traditional LL parser generators require the grammar to be transformed to eliminate left recursion:

```
expr : term expr'
     ;
expr' : '+' term expr'
      | ε
      ;
```

This transformation makes the grammar less readable and harder to maintain.

## 2.2 ANTLR4 and ALL(*)

ANTLR4 [1] is a widely-used parser generator that addresses many limitations of traditional LL parsing. It introduced the ALL(*) algorithm [2], which uses augmented transition networks (ATNs) and performs dynamic lookahead at runtime. ALL(*) can:

- Handle direct left recursion automatically (no manual transformation needed)
- Use arbitrary lookahead when needed (not limited to fixed k)
- Resolve many ambiguities through precedence and associativity rules

ANTLR4 generates parsers in multiple target languages (Java, C++, Python, JavaScript, etc.). The generated parsers are highly optimized and can process millions of tokens per second.

However, ANTLR4 requires code generation. The workflow is:

1. Write grammar in .g4 file
2. Run `antlr4` tool to generate lexer and parser classes
3. Compile generated code (e.g., `javac` for Java target)
4. Use generated parser in application

For a large grammar like Java, step 2 can take 10-20 seconds, and step 3 another 5-10 seconds. During grammar development, this 15-30 second cycle repeats for every change.

## 2.3 The Code Generation Trade-off

Code generation provides performance: generated parsers are specialized for the grammar and heavily optimized. ANTLR4's generated parsers achieve throughput of 1-3 million tokens per second on typical grammars.

However, code generation has costs:

**Build time overhead.** Every grammar change requires regeneration and recompilation. For large grammars, this can take 15-30 seconds per iteration.

**Workflow disruption.** The delay breaks developer flow. Instead of immediate feedback, developers must wait for the build to complete.

**Tooling complexity.** Build systems must be configured to run the parser generator and compile the output. This adds complexity to project setup.

**Debugging difficulty.** When a grammar fails, developers must reason about the generated code, which can be thousands of lines and difficult to understand.

For production systems processing large volumes of code, the performance benefit of generated parsers justifies these costs. However, during grammar development—when developers are iterating rapidly on small test cases—the build overhead becomes the bottleneck.

## 2.4 Left Recursion and Ambiguity

Two challenges complicate parser design:

**Left recursion** occurs when a non-terminal can derive a string starting with itself (e.g., `expr : expr '+' term`). Traditional LL parsers cannot handle left recursion and require grammar transformation. ANTLR4's ALL(*) handles direct left recursion but not all forms of indirect left recursion.

**Ambiguity** occurs when a grammar allows multiple parse trees for the same input. For example:

```
expr : expr '+' expr
     | expr '*' expr
     | NUMBER
     ;
```

The input `1 + 2 * 3` can be parsed as `(1 + 2) * 3` or `1 + (2 * 3)`. LL and LR parsers require unambiguous grammars or explicit disambiguation rules. Generalized parsing algorithms like GLR and GLL can handle ambiguous grammars by exploring multiple parse paths.

MSLL uses a multi-stack approach to handle both left recursion and ambiguity, as described in Section 4.