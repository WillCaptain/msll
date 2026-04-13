# 7. Use Cases and Limitations

This section discusses when to use MSLL versus ANTLR4, and current limitations.

## 7.1 When to Use MSLL

MSLL is well-suited for scenarios where development velocity matters more than runtime performance.

### Grammar Prototyping

When designing a new language or DSL, developers iterate rapidly on syntax alternatives. MSLL's instant feedback accelerates this process:

**Example**: A developer designing a configuration language tries 20 different syntax variations in 30 minutes. With MSLL, each change takes <1 second to test. With ANTLR4, the same session would take 5-10 minutes of waiting for builds.

### Grammar Debugging

Debugging grammar conflicts and ambiguities requires many small changes and tests. MSLL eliminates the build delay:

**Example**: Fixing a shift/reduce conflict in an expression grammar requires testing multiple precedence rules. MSLL allows immediate testing of each variant.

### Educational Use

Students learning parser design benefit from immediate feedback. MSLL makes parser construction more interactive and engaging:

**Example**: A compiler course uses MSLL for lab assignments. Students experiment with grammar rules and see results instantly, improving learning outcomes.

### Language Experimentation

Researchers exploring new language features need rapid iteration. MSLL supports quick experimentation:

**Example**: A researcher testing different syntax for async/await constructs can try dozens of variations in an afternoon.

### Small-Scale Tools

Tools processing small inputs don't need high throughput. MSLL's simplicity outweighs the performance cost:

**Example**: A code formatter for a DSL with <1000 line files parses in <1 second with MSLL, which is acceptable for interactive use.

## 7.2 When to Use ANTLR4

ANTLR4's generated parsers are preferred when runtime performance is critical.

### Production Compilers

Compilers processing large codebases need high throughput:

**Example**: A Java compiler parsing millions of lines of code requires ANTLR4's 2M+ tokens/sec throughput. MSLL's 22K tokens/sec would be too slow.

### IDE Integration

IDEs parse code continuously as users type. Low latency is essential:

**Example**: An IDE parsing a 10,000-line file needs <100ms response time. ANTLR4 achieves this; MSLL would take several seconds.

### Build Systems

CI/CD pipelines parsing thousands of files need efficiency:

**Example**: A build system parsing 5,000 source files in parallel benefits from ANTLR4's speed. MSLL would increase build time significantly.

### Performance-Critical Tools

Code analyzers, formatters, and refactoring tools process large codebases:

**Example**: A static analyzer scanning a 1M line codebase needs ANTLR4's throughput to complete in reasonable time.

### Stable Grammars

Once a grammar is stable and rarely changes, the build overhead becomes negligible:

**Example**: A production language with a stable grammar changes once per quarter. The 30-second build time is acceptable.

## 7.3 Hybrid Workflow

A practical approach combines both tools:

### Phase 1: Development (MSLL)
- Rapid iteration on grammar design
- Quick debugging of conflicts
- Experimentation with alternatives
- Testing on small sample files

### Phase 2: Validation (Both)
- Test grammar with both MSLL and ANTLR4
- Verify compatibility
- Benchmark performance
- Identify any MSLL-specific issues

### Phase 3: Production (ANTLR4)
- Deploy ANTLR4 generated parser
- Achieve high throughput
- Integrate with build systems
- Maintain with occasional MSLL iterations

This workflow provides fast development and high production performance.

## 7.4 Current Limitations

MSLL has several limitations that affect advanced use cases.

### Indirect Left Recursion

MSLL handles direct left recursion but not indirect:

```
// Direct (supported)
expr : expr '+' term | term;

// Indirect (not supported)
expr : term;
term : expr '+' factor | factor;
```

**Workaround**: Transform grammar to use direct left recursion.

### Complex Semantic Actions

MSLL supports simple actions but not complex side effects:

```
// Simple (supported)
expr : NUMBER {$value = $NUMBER.int};

// Complex (not supported)
expr : NUMBER {updateSymbolTable($NUMBER); checkTypes();};
```

**Workaround**: Perform semantic analysis in a separate tree-walking pass.

### Advanced Predicates

MSLL supports simple boolean predicates but not complex lookahead:

```
// Simple (supported)
expr : {isExpression()}? term;

// Complex (not supported)
expr : {LA(1) == ID && LA(2) == LPAREN}? functionCall;
```

**Workaround**: Restructure grammar to avoid complex predicates.

### Performance Ceiling

MSLL's 22K tokens/sec throughput is a fundamental limit of the interpretation approach. Further optimization may improve this by 2-3×, but MSLL will never match generated parsers.

**Workaround**: Use ANTLR4 for performance-critical applications.

### Memory Overhead

MSLL uses 30-40% more memory than ANTLR4 due to multiple stacks. For very large files (>1M tokens), this can be significant.

**Workaround**: Process large files in chunks or use ANTLR4.

## 7.5 Future Work

Potential improvements to address limitations:

1. **Indirect left recursion**: Extend the algorithm to detect and handle indirect recursion
2. **Performance optimization**: Implement bytecode compilation or JIT for hot paths
3. **Better error messages**: Improve error reporting with suggestions
4. **More G4 features**: Support additional ANTLR4 features (e.g., advanced predicates)
5. **Incremental parsing**: Parse only changed portions of input
6. **Parallel parsing**: Use multiple threads for independent stacks

## 7.6 Summary

MSLL is a specialized tool for grammar development, not a replacement for ANTLR4. It excels at rapid iteration and experimentation but sacrifices runtime performance. Users should choose the right tool for their use case or combine both in a hybrid workflow.