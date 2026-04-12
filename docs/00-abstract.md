# Abstract

Parser generators like ANTLR4 are widely used for building language processors, but they impose a significant workflow overhead: every grammar change requires code generation and recompilation, taking 3-9 seconds per iteration. During grammar development, where developers may iterate 30-100 times to debug and refine rules, this overhead accumulates to 5-15 minutes of pure waiting time, disrupting flow and slowing experimentation.

We present MSLL (Multi-Stack LL Parser), a runtime parser engine that interprets ANTLR4-compatible grammars directly without code generation, achieving sub-second iteration time. MSLL uses a multi-stack parsing approach inspired by Generalized LL (GLL) parsing: when encountering ambiguity, it forks multiple stacks and explores alternatives in parallel, pruning failed branches. This enables MSLL to handle left recursion and ambiguous constructs that challenge traditional LL parsers, while maintaining the practical engineering benefits of a working implementation.

MSLL achieves broad compatibility with ANTLR4's G4 grammar format, supporting lexer modes, channels, fragments, and parser rules with alternatives. We implement several optimizations including stack pooling for memory efficiency, flag-based pruning to avoid redundant work, and an epsilon-alongside mechanism for resolving FIRST/FOLLOW conflicts.

We evaluate MSLL on three real-world grammars (JSON, JavaScript, Python) with both small-scale and large-scale tests. Our workflow comparison shows MSLL provides 1.5-3× faster iteration cycles compared to ANTLR4. While MSLL's runtime throughput (1,900-69,000 tokens/sec) is 100× slower than ANTLR4 and degrades at scale, it excels in the development phase. We propose a hybrid workflow: use MSLL for rapid grammar development and debugging, then deploy with ANTLR4 for production performance.

**Keywords:** Parser, Runtime Parsing, LL Parsing, ANTLR4, Grammar Compatibility, Multi-Stack Parsing, Development Workflow