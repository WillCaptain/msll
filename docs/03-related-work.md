# 3. Related Work

Parser construction has been a fundamental problem in computer science for decades. This section reviews generalized parsing algorithms, parser generators, and runtime parsing approaches, positioning MSLL within this landscape.

## 3.1 Generalized Parsing Algorithms

Traditional LL and LR parsers require unambiguous grammars and cannot handle certain language constructs efficiently. Generalized parsing algorithms address these limitations by exploring multiple parse paths simultaneously.

**Generalized LR (GLR) Parsing.** Tomita [1] introduced GLR parsing in 1985, extending LR parsing to handle ambiguous grammars by maintaining multiple parse stacks in a graph-structured stack (GSS). When the parser encounters a conflict, it forks the stack and explores both alternatives. GLR parsers can parse any context-free grammar but are based on bottom-up LR parsing, which requires complex state tables and is difficult to understand and debug.

**Earley Parsing.** Earley's algorithm [2] is a top-down parsing method that can parse any context-free grammar in O(n³) time, with O(n²) for unambiguous grammars and O(n) for most LR(k) grammars. While elegant and general, Earley parsing is typically slower than GLR in practice and less commonly used in modern parser generators.

**Generalized LL (GLL) Parsing.** Scott and Johnstone [3] introduced GLL parsing in 2010 as a top-down alternative to GLR. Like GLR, GLL handles ambiguous grammars by exploring multiple parse paths, but it uses a more intuitive LL-style approach with explicit stacks rather than state tables. GLL maintains multiple parser stacks and uses a graph-structured stack (GSS) to share common stack suffixes, achieving efficient parsing of ambiguous grammars while preserving the clarity of recursive descent parsing.

**GLL Implementations.** Several tools implement GLL parsing, but all follow traditional compiler toolchain workflows. GLL Combinators [11] embeds GLL parsing in Scala as a parser combinator library, but grammar changes require recompiling the Scala project. GoGLL [12] is a parser generator that produces Go or Rust code from grammar files, requiring code generation and compilation like ANTLR4. Other implementations like PEGLL and Rascal's GLL support either require code generation or are embedded in specific language workbenches. To our knowledge, no existing GLL implementation supports runtime grammar loading without compilation.

MSLL is directly inspired by GLL parsing but differs in two key aspects. First, while GLL uses a GSS for stack sharing, MSLL uses independent stacks with pooling for simplicity. Second, and more importantly, MSLL is the first multi-stack parser to support runtime grammar loading, enabling instant iteration without any build step. This makes MSLL uniquely suited for grammar development workflows where iteration speed matters more than theoretical completeness.

## 3.2 Parser Generators

Parser generators automate parser construction by generating code from grammar specifications. They are the dominant approach in production systems.

**ANTLR4 and ALL(*).** ANTLR (ANother Tool for Language Recognition) [4] is one of the most widely used parser generators. ANTLR4 introduced the ALL(*) algorithm [5], which uses augmented transition networks (ATNs) and performs lookahead dynamically at runtime to resolve ambiguities. ALL(*) can handle direct left recursion and many ambiguous constructs. However, ANTLR4 requires code generation: the grammar is compiled into Java, C++, Python, or other target languages, and any grammar change requires regeneration and recompilation. For large grammars, this build step can take 5-30 seconds, disrupting the development workflow.

**Yacc/Bison.** Yacc (Yet Another Compiler Compiler) [6] and its GNU implementation Bison are classic LALR parser generators. They generate efficient bottom-up parsers but require careful grammar design to avoid shift/reduce and reduce/reduce conflicts. Like ANTLR4, they require code generation and recompilation for every grammar change.

**PEG Parsers.** Parsing Expression Grammars (PEG) [7] use ordered choice rather than ambiguous alternatives, eliminating ambiguity by design. PEG parsers like Packrat [8] achieve linear-time parsing through memoization but can have high memory overhead. PEG grammars are not compatible with CFG-based grammars like ANTLR4's G4 format.

## 3.3 Runtime Parsing Approaches

Some systems avoid code generation by interpreting grammars at runtime.

**Interpreter-Based Parsers.** Some parser frameworks interpret grammar rules directly without generating code. For example, ANTLR3 had an interpreter mode, though it was slower and less commonly used than the generated parser. These interpreters typically handle only simple LL(k) grammars and struggle with left recursion and ambiguity.

**Dynamic Parsing Systems.** Research systems like DynSem [9] and Rascal [10] provide dynamic parsing capabilities for language workbenches. However, these systems are typically designed for meta-programming environments rather than as standalone parser libraries, and they do not focus on compatibility with existing grammar formats.

## 3.4 Positioning MSLL

MSLL occupies a unique position in this landscape. It combines:

1. **GLL-inspired multi-stack parsing** for handling ambiguity and left recursion, but with a simpler implementation using independent stacks with pooling rather than a GSS.

2. **Runtime grammar loading** to eliminate code generation overhead, enabling instant iteration during grammar development. Unlike all existing GLL implementations which require compilation or code generation, MSLL interprets grammar files directly at runtime.

3. **ANTLR4 G4 grammar compatibility** (~85% of common features) to leverage the large ecosystem of existing grammars rather than requiring a new grammar format or embedding grammars in host language code.

The key trade-off is performance: MSLL achieves 5,000-69,000 tokens/sec throughput on small inputs, approximately 100× slower than ANTLR4's generated parsers. However, for use cases where development velocity matters more than runtime performance—such as grammar prototyping, educational tools, and language experimentation—this trade-off is acceptable.

MSLL is not intended to replace ANTLR4 in production systems. Rather, it complements ANTLR4 by providing a faster development workflow during the grammar design phase. Once a grammar is stable, developers can switch to ANTLR4 for production deployment. This hybrid workflow combines MSLL's iteration speed (1.46-3× faster) with ANTLR4's runtime performance (100× faster).

---

**References**

[1] M. Tomita, "Efficient Parsing for Natural Language," Kluwer Academic Publishers, 1985.

[2] J. Earley, "An Efficient Context-Free Parsing Algorithm," Communications of the ACM, vol. 13, no. 2, pp. 94-102, 1970.

[3] E. Scott and A. Johnstone, "GLL Parsing," Electronic Notes in Theoretical Computer Science, vol. 253, no. 7, pp. 177-189, 2010.

[4] T. Parr, "The Definitive ANTLR 4 Reference," Pragmatic Bookshelf, 2013.

[5] T. Parr, S. Harwell, and K. Fisher, "Adaptive LL(*) Parsing: The Power of Dynamic Analysis," ACM SIGPLAN Notices, vol. 49, no. 10, pp. 579-598, 2014.

[6] S. C. Johnson, "Yacc: Yet Another Compiler-Compiler," Computing Science Technical Report No. 32, Bell Laboratories, 1975.

[7] B. Ford, "Parsing Expression Grammars: A Recognition-Based Syntactic Foundation," ACM SIGPLAN Notices, vol. 39, no. 1, pp. 111-122, 2004.

[8] B. Ford, "Packrat Parsing: Simple, Powerful, Lazy, Linear Time," Proceedings of the 7th ACM SIGPLAN International Conference on Functional Programming, pp. 36-47, 2002.

[9] V. Vergu, P. Neron, and E. Visser, "DynSem: A DSL for Dynamic Semantics Specification," Proceedings of the 26th International Conference on Rewriting Techniques and Applications, pp. 365-378, 2015.

[10] P. Klint, T. van der Storm, and J. Vinju, "RASCAL: A Domain Specific Language for Source Code Analysis and Manipulation," Proceedings of the 9th IEEE International Working Conference on Source Code Analysis and Manipulation, pp. 168-177, 2009.

[11] D. Spiewak, "GLL Combinators: A Parser Combinator Library Based on the GLL Algorithm," GitHub repository, https://github.com/djspiewak/gll-combinators

[12] "GoGLL: General Context-free Parsing Made Easy," https://goccmack.github.io/