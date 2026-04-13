# 5. Implementation

MSLL is implemented in ~7,800 lines of Java with two key optimizations: stack pooling and flag-based pruning.

## 5.1 Stack Pooling

Multi-stack parsing creates many short-lived stack objects, causing high GC pressure. MSLL uses a `ConcurrentLinkedDeque` as a stack pool for O(1) reuse. When a stack is no longer needed, it returns to the pool rather than being garbage collected. When a new stack is needed, the parser first checks the pool before allocating. This reduces memory allocation overhead by ~60% in our benchmarks.

## 5.2 Flag-Based Pruning

Each stack carries a `Flag` object that marks whether the stack is active or expired. When a stack fails to match input, its flag is set to expired. Parse tree nodes reference these flags, allowing automatic removal of nodes from failed parse paths without traversing the entire tree. This avoids the complexity of GLL's Shared Packed Parse Forest (SPPF) while still efficiently handling ambiguity.

## 5.3 Epsilon-Alongside Optimization

When a rule has both epsilon (empty) and non-epsilon alternatives (e.g., `a : b | ε`), traditional LL parsers face FIRST/FOLLOW conflicts. MSLL resolves this by trying the non-epsilon alternative first, then falling back to epsilon if it fails. This heuristic works well in practice without requiring complex conflict resolution.

**Code statistics**: The implementation consists of ~7,800 lines of Java across lexer engine (~2,000 lines), parser engine (~3,000 lines), grammar processing (~1,500 lines), and utilities (~1,300 lines).
