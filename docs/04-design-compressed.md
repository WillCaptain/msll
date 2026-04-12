# 4. Design

MSLL uses a multi-stack parsing approach inspired by GLL to handle ambiguity and left recursion while maintaining compatibility with ANTLR4's G4 grammar format.

## 4.1 Multi-Stack Parsing

**Core algorithm**: MSLL maintains multiple parser stacks and explores alternative parse paths in parallel. When encountering a rule with multiple alternatives, the parser forks stacks to try each possibility. Failed stacks are pruned immediately, and parsing succeeds if any stack reaches EOF successfully.

**Handling ambiguity**: Consider the rule `expr : term '+' expr | term`. When parsing a term, MSLL creates two stacks: one attempting `term '+' expr` and another attempting just `term`. Both stacks parse independently. If the first stack fails (no `'+'` follows), it is pruned while the second continues.

**Key difference from GLL**: While GLL uses a Graph-Structured Stack (GSS) to share stack nodes, MSLL uses independent stacks with pooling for simplicity. This trades some theoretical efficiency for implementation simplicity and faster development iteration.

## 4.2 Left Recursion Handling

Traditional LL parsers fail on left-recursive rules like `expr : expr '+' term | term` due to infinite recursion. MSLL handles this through iterative deepening:

1. **First iteration**: Try non-left-recursive alternatives first (`term`)
2. **Subsequent iterations**: If successful, try left-recursive alternatives (`expr '+' term`)
3. **Termination**: Stop when no more input can be consumed

For example, parsing "1 + 2 + 3" proceeds as: parse "1" as term → try `expr '+' term` with "+ 2 + 3" → try again with "+ 3" → done. This produces left-associative trees: `((1 + 2) + 3)`.

## 4.3 G4 Grammar Compatibility

MSLL supports ~85% of commonly-used G4 features to enable reuse of existing ANTLR4 grammars:

**Lexer features**: Modes (context-dependent tokenization), channels (HIDDEN for whitespace), fragments (reusable components), character classes, regex patterns.

**Parser features**: Alternatives (`a | b | c`), quantifiers (`?`, `*`, `+`), labels (`left=expr`), direct left recursion.

**Limitations**: Indirect left recursion, complex semantic actions, and advanced predicates are not supported. However, most real-world grammars use only direct left recursion and simple actions, making these limitations rarely problematic in practice.

**Runtime loading**: Unlike existing GLL implementations (GLL Combinators, GoGLL) which require compilation or code generation, MSLL interprets grammar files directly at runtime. Developers modify `.gm` files and immediately test changes without any build step—a workflow impossible with current GLL tools.
