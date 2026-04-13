# 1. Introduction

Parsers are fundamental components in compilers, interpreters, code analysis tools, and language-aware editors. Parser generators like ANTLR4 [1], Yacc/Bison [2], and others automate parser construction by generating code from declarative grammar specifications. These tools have been enormously successful and are used in thousands of projects.

However, parser generators impose a workflow overhead: they require code generation and recompilation for every grammar change. The typical workflow is:

1. Edit the grammar file (.g4 for ANTLR4)
2. Run the parser generator to produce source code (~1-2 seconds)
3. Compile the generated code (~2-3 seconds)
4. Run tests to verify the grammar
5. Repeat from step 1 for each iteration

For a simple JSON grammar, steps 2-3 take approximately 3 seconds. While this may seem minor, it accumulates quickly during grammar development. A developer making 30 grammar changes in a session spends 1.5 minutes waiting for builds—time that could be spent on productive work. More importantly, this delay disrupts flow and makes grammar debugging tedious. For 100 iterations typical in complex grammar development, the overhead reaches 5 minutes of pure waiting time.

## 1.1 Motivation

Consider a developer designing a grammar for a new programming language. They write an initial grammar, generate the parser, and test it on sample code. The parser fails on a particular construct. They modify the grammar, regenerate, recompile, and test again. This cycle repeats dozens or hundreds of times as they refine the grammar, handle edge cases, and fix ambiguities.

The code generation step is necessary because generated parsers are fast: ANTLR4's generated parsers can process millions of tokens per second. However, during grammar development, this performance is often unnecessary. The developer is testing on small sample files (typically <5,000 tokens), not parsing gigabytes of code. What matters more is iteration speed—the time from making a grammar change to seeing the result.

This creates a fundamental trade-off: **development velocity vs runtime performance**. Parser generators optimize for runtime performance at the cost of development velocity. But what if we could optimize for development velocity instead?

## 1.2 MSLL Approach

MSLL (Multi-Stack LL Parser) eliminates the code generation step by interpreting ANTLR4-compatible grammars directly at runtime. When a grammar changes, the developer simply reloads it—no generation, no compilation, instant feedback. Our measurements show MSLL achieves 6.11 seconds per iteration vs ANTLR4's 8.95 seconds, a 1.46× speedup. In ideal conditions without build system overhead, MSLL can achieve sub-second iteration time vs ANTLR4's fixed 3-second overhead.

The key challenge in runtime parsing is handling constructs that traditional LL parsers struggle with, particularly left recursion and ambiguity. MSLL addresses this using a multi-stack parsing approach inspired by Generalized LL (GLL) parsing [3]:

- When the parser encounters ambiguity (multiple possible parse paths), it **forks** multiple stacks and explores alternatives in parallel
- When a stack fails to match the input, it is **pruned** immediately
- The parser continues until all stacks either succeed or fail

This approach enables MSLL to handle direct left recursion, ambiguous grammars, and complex language constructs without requiring grammar transformation or manual disambiguation.

**Crucially, while GLL provides the theoretical foundation for multi-stack parsing, existing GLL implementations (GLL Combinators, GoGLL) follow traditional compiler toolchain workflows requiring compilation or code generation.** To our knowledge, MSLL is the first multi-stack parser that supports runtime grammar loading, enabling true instant iteration: developers can modify grammar files and immediately test changes without any build step.

MSLL maintains broad compatibility with ANTLR4's G4 grammar format (~85% of common features), supporting:
- Lexer features: modes, channels, fragments, character classes, regex patterns
- Parser features: alternatives, labels, left recursion
- Grammar options: tokenVocab for linking lexer and parser

**Trade-off**: MSLL achieves 5,000-69,000 tokens/sec throughput on small inputs, approximately 100× slower than ANTLR4's generated parsers. However, this performance is sufficient for grammar development scenarios where test inputs are small and iteration speed matters more than runtime throughput.

## 1.3 Contributions

This paper makes the following contributions:

1. **Runtime parser engine with G4 compatibility.** We present MSLL, a runtime parser that interprets ANTLR4-compatible grammars directly without code generation, achieving 1.46× faster iteration during grammar development (6.11s vs 8.95s per iteration).

2. **Practical multi-stack parsing implementation.** We describe a GLL-inspired multi-stack parsing approach with engineering optimizations: stack pooling for memory efficiency, flag-based pruning to avoid redundant work, and epsilon-alongside mechanism for FIRST/FOLLOW conflict resolution. Unlike theoretical GLL with its complex Graph-Structured Stack, MSLL provides a simpler implementation (~7,800 lines of Java) focused on practical workflow benefits.

3. **Comprehensive evaluation with workflow analysis.** We evaluate MSLL on three real-world grammars (JSON, JavaScript, Python) at multiple scales, demonstrating:
   - Correctness: All test cases pass across diverse grammars
   - Performance: 5K-69K tok/s on small inputs, degrading to 200-1,200 tok/s at scale
   - Workflow: 1.46× faster iteration, saving 1.4-4.7 minutes over 30-100 iterations
   - Compatibility: ~85% of common G4 features supported

4. **Hybrid workflow recommendation.** Based on our evaluation, we propose a practical hybrid approach: use MSLL for rapid grammar development (fast iteration), then deploy with ANTLR4 for production (high performance). This combines the best of both worlds.

5. **Open-source implementation.** MSLL is implemented in ~7,800 lines of Java and released as open source, providing a practical tool for grammar prototyping, debugging, and education.

## 1.4 Positioning

MSLL is not a replacement for ANTLR4 or other parser generators. Rather, it is a complementary tool optimized for a different phase of the development lifecycle:

- **MSLL**: Optimized for grammar development phase
  - Fast iteration (<1s ideal, 6s with Maven)
  - Immediate feedback on grammar changes
  - Suitable for small test inputs (<5K tokens)

- **ANTLR4**: Optimized for production deployment
  - High throughput (2M+ tok/s)
  - Optimized generated code
  - Suitable for large-scale parsing

Compared to GLL (Generalized LL parsing), MSLL shares the multi-stack approach but differs in focus:

- **GLL**: Theoretical parsing algorithm with completeness guarantees
  - Graph-Structured Stack (GSS) for memoization
  - Handles all context-free grammars
  - Focus on parsing theory

- **MSLL**: Practical implementation with workflow focus
  - Simpler stack management with pooling
  - G4 grammar compatibility for reusing existing grammars
  - Focus on development velocity

## 1.5 Paper Organization

The rest of this paper is organized as follows. Section 2 provides background on LL parsing and parser generators. Section 3 reviews related work in generalized parsing and runtime parsing systems. Section 4 describes MSLL's design, including the multi-stack parsing model and G4 compatibility. Section 5 details the implementation and optimizations. Section 6 presents experimental evaluation on real-world grammars with workflow comparison. Section 7 discusses use cases, hybrid workflow, and limitations. Section 8 concludes and outlines future work.