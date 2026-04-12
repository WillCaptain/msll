# Evaluation Experiments Plan

## Goal
Demonstrate MSLL's capability on real-world grammars and measure performance trade-offs vs ANTLR4.

## Grammars to Test

### 1. JSON (Simple, Unambiguous)
- **Purpose**: Baseline for simple grammar
- **Characteristics**: No left recursion, unambiguous, widely understood
- **Test cases**: Valid JSON objects, arrays, nested structures
- **Metrics**: Throughput (tokens/sec), correctness

### 2. JavaScript Subset (Complex, Left-Recursive)
- **Purpose**: Demonstrate handling of complex language features
- **Characteristics**: Left recursion in expressions, ambiguous constructs
- **Test cases**: Expressions, function declarations, control flow
- **Metrics**: Throughput, left recursion handling

### 3. Python Subset (Indentation-Sensitive)
- **Purpose**: Show lexer mode handling for indentation
- **Characteristics**: INDENT/DEDENT tokens, statement blocks
- **Test cases**: Functions, if/while blocks, nested indentation
- **Metrics**: Throughput, mode switching correctness

### 4. Java (Large Grammar)
- **Purpose**: Stress test with production-scale grammar
- **Characteristics**: Large rule set, complex type system
- **Test cases**: Class declarations, methods, generics
- **Metrics**: Throughput on large files

### 5. Outline (Custom Language)
- **Purpose**: Show applicability to domain-specific languages
- **Characteristics**: Custom syntax, specific use case
- **Test cases**: Existing test suite
- **Metrics**: Throughput, feature coverage

## Performance Comparison

### MSLL Measurements
- Parse throughput (tokens/sec) for each grammar
- Memory usage during parsing
- Grammar load time (instant)

### ANTLR4 Baseline
- Code generation time (5-30 seconds)
- Compilation time (5-10 seconds)
- Parse throughput (1-3M tokens/sec)
- Total iteration time: 10-40 seconds

### Workflow Comparison
- MSLL: Edit grammar → Reload → Test (< 1 second)
- ANTLR4: Edit grammar → Generate → Compile → Test (10-40 seconds)

## Implementation Tasks

1. Create JSON grammar (.gm files)
2. Create Python subset grammar
3. Verify JavaScript grammar exists and works
4. Create comprehensive test cases for each grammar
5. Implement performance benchmarks
6. Measure and document results
7. Create comparison tables and figures

## Expected Results

- MSLL throughput: 15-30K tokens/sec (varies by grammar complexity)
- ANTLR4 throughput: 1-3M tokens/sec (100x faster)
- MSLL iteration time: < 1 second (instant)
- ANTLR4 iteration time: 10-40 seconds (100x slower)

**Key insight**: MSLL trades runtime performance for development velocity.