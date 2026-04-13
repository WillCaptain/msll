# MSLL: A Runtime Multi-Stack LL Parser with ANTLR4 Grammar Compatibility

## Paper Outline

### Abstract (200-250 words)
- Problem: Parser generators require code generation and recompilation, slowing iteration
- Solution: MSLL - runtime parser engine using multi-stack approach for ambiguity handling
- Key contribution: G4 grammar compatibility without code generation
- Results: Instant iteration, handles left recursion, 22K tok/s on real grammars
- Trade-off: 100x slower than ANTLR4 but eliminates build step

### 1. Introduction (2 pages)
- Motivation: Parser development workflow bottleneck
- Problem statement: Code generation vs runtime parsing trade-off
- MSLL approach: GLL-inspired multi-stack parsing with G4 compatibility
- Contributions:
  1. Runtime parser engine with G4 grammar compatibility
  2. Stack pooling and flag-based pruning optimizations
  3. Epsilon-alongside mechanism for FIRST/FOLLOW conflicts
  4. Evaluation on 5 real-world grammars (JSON, JavaScript, Python, Java, Outline)
- Paper organization

### 2. Background (2 pages)
- 2.1 LL Parsing and Limitations
- 2.2 Parser Generators (ANTLR4, Yacc/Bison)
- 2.3 Code Generation Workflow Overhead
- 2.4 Left Recursion Problem

### 3. Related Work (3 pages)
- 3.1 Generalized Parsing Algorithms
  - GLL (Scott & Johnstone, 2010) - theoretical foundation
  - GLR (Tomita, 1985)
  - Earley parsing
- 3.2 Parser Generators
  - ANTLR4 and ALL(*) algorithm
  - Yacc/Bison (LALR)
  - PEG parsers (Packrat)
- 3.3 Runtime Parsing Approaches
  - Interpreter-based parsers
  - Dynamic parsing systems
- 3.4 Positioning: MSLL as practical GLL implementation with G4 compatibility

### 4. Design (3 pages)
- 4.1 Multi-Stack Parsing Model
  - Fork on ambiguity, prune on failure
  - Parallel branch exploration
- 4.2 G4 Grammar Compatibility
  - Lexer modes, channels, fragments
  - Parser rules, alternatives, actions
- 4.3 Left Recursion Handling
- 4.4 Error Recovery Strategy

### 5. Implementation (3 pages)
- 5.1 Architecture Overview
- 5.2 Stack Management and Pooling
  - ConcurrentLinkedDeque for reuse
  - Flag-based expiration mechanism
- 5.3 Epsilon-Alongside Optimization
- 5.4 Code Statistics (~7,800 lines Java)

### 6. Evaluation (4 pages)
- 6.1 Experimental Setup
- 6.2 Grammar Coverage
  - JSON (simple, unambiguous)
  - JavaScript subset (complex, left-recursive)
  - Python subset (indentation-sensitive)
  - Java (large grammar)
  - Outline (custom language)
- 6.3 Performance Analysis
  - Throughput: 22K tok/s average
  - Comparison with ANTLR4 (~2M tok/s)
  - Memory usage
- 6.4 Workflow Comparison
  - Iteration time: instant vs 5-30s
  - Development experience
- 6.5 G4 Compatibility Testing

### 7. Use Cases and Limitations (2 pages)
- 7.1 When to Use MSLL
  - Rapid prototyping
  - Educational purposes
  - Grammar debugging
- 7.2 When to Use ANTLR4
  - Production systems
  - Performance-critical applications
- 7.3 Current Limitations

### 8. Conclusion and Future Work (1 page)
- Summary of contributions
- Future directions: performance optimization, more G4 features

### References (2 pages)
- GLL, GLR, Earley, ANTLR4, ALL(*), PEG, etc.

---

## Target Journal
**Frontiers of Computer Science** (CCF-B)
- Focus: Software engineering, programming languages
- Accepts tool papers with practical contributions
- Page limit: 15-20 pages
- Review cycle: 3-6 months with revisions
