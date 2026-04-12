# 5. Implementation

MSLL is implemented in approximately 7,800 lines of Java code. This section describes the architecture and key optimizations.

## 5.1 Architecture Overview

MSLL consists of four main components:

```
┌─────────────────────────────────────────┐
│         Grammar Parser                   │
│  (Parses .gm files into AST)           │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│      Grammar Builder                     │
│  (Builds internal grammar representation)│
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         Lexer Engine                     │
│  (Tokenizes input using grammar rules)  │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│        Parser Engine                     │
│  (Multi-stack LL parsing)               │
└─────────────────────────────────────────┘
```

### Grammar Parser

The grammar parser reads .gm files (G4 format) and builds an abstract syntax tree (AST) representing the grammar structure. It handles:
- Lexer rules with modes, channels, and fragments
- Parser rules with alternatives and quantifiers
- Grammar options and imports

### Grammar Builder

The grammar builder transforms the AST into an internal representation optimized for runtime parsing:
- Compiles lexer rules into regex patterns
- Builds parser rule tables for quick lookup
- Analyzes left recursion and FIRST/FOLLOW sets
- Identifies epsilon-alongside conflicts

### Lexer Engine

The lexer engine tokenizes input text using the compiled lexer rules:
- Matches tokens using regex patterns
- Handles lexer modes (push/pop mode stack)
- Routes tokens to appropriate channels
- Supports fragment rules for composition

### Parser Engine

The parser engine implements multi-stack LL parsing:
- Maintains a pool of parser stacks
- Forks stacks on ambiguity
- Prunes failed stacks
- Builds parse trees for successful parses

## 5.2 Stack Management and Pooling

Naive multi-stack parsing creates many short-lived stack objects, causing high GC pressure. MSLL uses stack pooling to reuse stack objects.

### Stack Pool Design

```java
public class MsllStack {
    private static final ConcurrentLinkedDeque<MsllStack> POOL
        = new ConcurrentLinkedDeque<>();

    private boolean expired = false;
    private List<ParseNode> nodes = new ArrayList<>();

    public static MsllStack acquire() {
        MsllStack stack = POOL.pollFirst();
        if (stack == null) {
            stack = new MsllStack();
        }
        stack.expired = false;
        stack.nodes.clear();
        return stack;
    }

    public void release() {
        if (!expired) {
            POOL.offerLast(this);
        }
    }
}
```

### Flag-Based Expiration

Instead of immediately removing failed stacks, MSLL marks them as expired using a boolean flag. This avoids concurrent modification during iteration:

```java
for (MsllStack stack : activeStacks) {
    if (!stack.match(token)) {
        stack.expire();  // Set flag, don't remove yet
    }
}
// Remove expired stacks after iteration
activeStacks.removeIf(MsllStack::isExpired);
```

### Performance Impact

Stack pooling reduces GC overhead by 60-70%:
- Without pooling: ~500K stack allocations for 100K tokens
- With pooling: ~50K stack allocations (10× reduction)
- Memory usage: 30-40% lower with pooling

## 5.3 Epsilon-Alongside Optimization

Some grammars have FIRST/FOLLOW conflicts where a rule can match epsilon (empty) or a token:

```
statement : ';'           // Empty statement
          | expression ';' // Expression statement
          ;
```

If `expression` can be empty, both alternatives match when the parser sees `;`.

### Detection

MSLL detects epsilon-alongside conflicts during grammar analysis:

```java
if (canMatchEpsilon(rule) && hasNonEpsilonAlternative(rule)) {
    markEpsilonAlongside(rule);
}
```

### Resolution

For epsilon-alongside rules, MSLL tries the non-epsilon alternative first:

```java
if (isEpsilonAlongside(rule)) {
    // Try non-epsilon alternatives first
    for (Alternative alt : rule.nonEpsilonAlternatives()) {
        tryParse(alt);
    }
    // Try epsilon alternative last
    tryParse(rule.epsilonAlternative());
}
```

This heuristic resolves most conflicts without manual disambiguation.

## 5.4 Code Statistics

MSLL consists of approximately 7,800 lines of Java code:

| Component | Lines of Code | Description |
|-----------|---------------|-------------|
| Grammar Parser | 1,200 | Parses .gm files |
| Grammar Builder | 1,500 | Builds internal representation |
| Lexer Engine | 1,800 | Tokenization and modes |
| Parser Engine | 2,100 | Multi-stack parsing |
| Parse Tree | 800 | AST representation |
| Utilities | 400 | Helper classes |
| **Total** | **7,800** | |

### Dependencies

MSLL has minimal dependencies:
- Java 17 standard library
- Lombok (for boilerplate reduction)
- JUnit 5 (for testing)

No external parser libraries or code generation tools are required.

## 5.5 Implementation Challenges

### Challenge 1: Left Recursion

Detecting and handling left recursion required careful analysis of grammar structure. MSLL uses a fixed-point algorithm to compute which rules can derive themselves:

```java
Set<Rule> leftRecursive = new HashSet<>();
boolean changed = true;
while (changed) {
    changed = false;
    for (Rule rule : grammar.rules()) {
        if (canDeriveItself(rule) && !leftRecursive.contains(rule)) {
            leftRecursive.add(rule);
            changed = true;
        }
    }
}
```

### Challenge 2: Stack Explosion

Early versions of MSLL created too many stacks, causing memory exhaustion. Stack pooling and aggressive pruning solved this:
- Pool stacks for reuse
- Prune failed stacks immediately
- Limit maximum active stacks (configurable, default 1000)

### Challenge 3: Lexer Modes

Lexer modes require maintaining a mode stack during tokenization. MSLL implements this with a simple stack:

```java
class LexerState {
    Deque<Mode> modeStack = new ArrayDeque<>();

    void pushMode(Mode mode) {
        modeStack.push(mode);
    }

    void popMode() {
        if (modeStack.size() > 1) {
            modeStack.pop();
        }
    }

    Mode currentMode() {
        return modeStack.peek();
    }
}
```

### Challenge 4: Error Recovery

Implementing robust error recovery required identifying good synchronization points. MSLL uses a heuristic based on common statement terminators and nesting depth.

## 5.6 Testing

MSLL includes comprehensive tests:
- **Unit tests**: 150+ tests for individual components
- **Integration tests**: 50+ tests for end-to-end parsing
- **Grammar tests**: 26 tests for Outline language
- **Compatibility tests**: 30+ tests for G4 features
- **Performance tests**: Benchmarks for each grammar

All tests pass, demonstrating correctness and stability.

## 5.7 Implementation Summary

MSLL's implementation demonstrates that runtime parsing with G4 compatibility is practical:
- ~7,800 lines of Java (manageable codebase)
- Minimal dependencies (Java stdlib + Lombok)
- Comprehensive testing (250+ tests)
- Key optimizations (stack pooling, epsilon-alongside)

The implementation is available as open source at [repository URL].