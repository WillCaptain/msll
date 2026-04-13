# 8. Conclusion and Future Work

Parser generators like ANTLR4 are powerful tools for building language processors, but their code generation workflow creates friction during grammar development. Every grammar change requires regeneration and recompilation, which can take 5-35 seconds for large grammars. This delay disrupts developer flow and slows iteration.

We presented MSLL (Multi-Stack LL Parser), a runtime parser engine that interprets ANTLR4 G4 grammars directly without code generation. MSLL uses a multi-stack parsing approach inspired by GLL parsing: when the parser encounters ambiguity, it forks multiple stacks and explores alternatives in parallel, pruning failed branches. This enables MSLL to handle left recursion and ambiguous constructs while maintaining the simplicity of LL parsing.

MSLL achieves broad compatibility with ANTLR4's G4 grammar format, supporting approximately 90% of commonly-used features including lexer modes, channels, fragments, parser alternatives, quantifiers, and direct left recursion. We implemented several optimizations including stack pooling for memory efficiency, flag-based pruning to avoid redundant work, and an epsilon-alongside mechanism for resolving FIRST/FOLLOW conflicts.

We evaluated MSLL on five real-world grammars: JSON, JavaScript subset, Python subset, Java, and a custom language. MSLL correctly parses all test cases, demonstrating correctness across diverse language features. Performance measurements show MSLL achieves an average throughput of 22,400 tokens per second, approximately 100× slower than ANTLR4's generated parsers but with instant iteration time (<1 second vs 5-35 seconds).

This performance trade-off makes MSLL well-suited for grammar development use cases where iteration speed matters more than runtime performance: grammar prototyping, debugging, educational settings, language experimentation, and small-scale tools. For production systems requiring high throughput, ANTLR4 remains the better choice. A practical hybrid workflow uses MSLL during development for rapid iteration, then switches to ANTLR4 for production deployment.

## Future Work

Several directions could extend MSLL's capabilities:

**Indirect left recursion.** MSLL currently handles only direct left recursion. Extending the algorithm to detect and handle indirect recursion would improve grammar compatibility.

**Performance optimization.** While MSLL will never match generated parsers, bytecode compilation or JIT optimization could improve throughput by 2-3×, making it viable for larger files.

**Incremental parsing.** Parsing only changed portions of input would reduce latency for interactive tools like editors.

**Parallel parsing.** Using multiple threads to explore independent parse stacks could improve throughput on multi-core systems.

**Better error messages.** Enhanced error reporting with suggestions and fix-it hints would improve the development experience.

**More G4 features.** Supporting additional ANTLR4 features like advanced predicates and complex semantic actions would increase compatibility.

**Language targets.** Implementing MSLL in other languages (C++, Python, JavaScript) would broaden its applicability.

## Availability

MSLL is implemented in approximately 7,800 lines of Java and is available as open source under the MIT license at [repository URL]. The implementation includes comprehensive tests, documentation, and example grammars.

## Acknowledgments

We thank the ANTLR community for creating the G4 grammar format and providing a rich ecosystem of grammars to test against. We also thank the reviewers for their valuable feedback.

---

**In summary**, MSLL demonstrates that runtime parsing with G4 compatibility is practical and useful for grammar development. By trading runtime performance for development velocity, MSLL fills a gap in the parser tooling ecosystem and complements existing parser generators like ANTLR4.