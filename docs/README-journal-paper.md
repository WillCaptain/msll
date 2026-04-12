# MSLL: A Runtime Multi-Stack LL Parser with ANTLR4 Grammar Compatibility

**Target Journal:** Frontiers of Computer Science (CCF-B)

**Authors:** [Your Name and Affiliations]

**Date:** March 2026

---

## Document Structure

This paper is organized into the following sections:

1. [Abstract](00-abstract.md)
2. [Introduction](01-introduction.md)
3. [Background](02-background.md)
4. [Related Work](03-related-work.md)
5. [Design](04-design.md)
6. [Implementation](05-implementation.md)
7. [Evaluation](06-evaluation.md)
8. [Use Cases and Limitations](07-use-cases.md)
9. [Conclusion and Future Work](08-conclusion.md)

---

## Paper Status

**Current Status:** Draft v1.0 - Complete restructure for journal submission

**Changes from Conference Version:**
- Added comprehensive Related Work section (was missing)
- Expanded evaluation to 5 grammars (was only Outline)
- Added honest performance comparison with ANTLR4
- Repositioned as practical tool, not novel algorithm
- Fixed misleading claims about line count and performance
- Added workflow comparison showing iteration time advantage
- Added Use Cases section explaining when to use MSLL vs ANTLR4
- Added Limitations and Future Work

**Target Metrics:**
- Page count: 15-20 pages (journal format)
- Sections: 8 main sections + references
- Evaluation: 5 real-world grammars
- Performance data: Throughput, memory, iteration time
- Compatibility: ~90% of G4 features

**Next Steps:**
1. Implement and run evaluation experiments
2. Collect actual performance data
3. Create figures and tables
4. Polish writing and formatting
5. Get feedback from colleagues
6. Submit to Frontiers of Computer Science

---

## Key Messages

1. **Problem:** Parser generators require code generation (5-35s), disrupting development flow
2. **Solution:** MSLL interprets G4 grammars at runtime with instant iteration (<1s)
3. **Trade-off:** 100× slower runtime (22K vs 2M tokens/sec) but 15-35× faster iteration
4. **Positioning:** Practical tool for grammar development, not replacement for ANTLR4
5. **Contribution:** Demonstrates runtime parsing with G4 compatibility is practical and useful

---

## Evaluation Plan

See [evaluation-plan.md](evaluation-plan.md) for detailed experiment design.

**Grammars:**
1. JSON - Simple baseline
2. JavaScript - Complex with left recursion
3. Python - Indentation-sensitive
4. Java - Large production grammar
5. Outline - Custom DSL

**Metrics:**
- Correctness: All test cases pass
- Performance: Throughput (tokens/sec), memory usage
- Workflow: Iteration time comparison
- Compatibility: G4 feature coverage (~90%)

---

## Writing Guidelines

**Tone:**
- Honest about limitations and trade-offs
- Position as practical tool, not breakthrough
- Acknowledge GLL as theoretical foundation
- Compare fairly with ANTLR4 (both strengths and weaknesses)

**Avoid:**
- Overclaiming novelty
- Misleading performance claims
- Ignoring related work
- Dismissing ANTLR4

**Emphasize:**
- Practical utility for grammar development
- Instant iteration advantage
- G4 compatibility value
- Clear use cases and limitations
