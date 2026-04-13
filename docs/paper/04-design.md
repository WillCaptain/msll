# 4. Design

This section describes MSLL's design, including the multi-stack parsing model, G4 grammar compatibility, left recursion handling, and error recovery.

## 4.1 Multi-Stack Parsing Model

MSLL uses a multi-stack approach inspired by GLL parsing [3]. The key idea is to maintain multiple parser stacks simultaneously and explore alternative parse paths in parallel.

### Basic Algorithm

The parsing algorithm works as follows:

```
1. Initialize with a single stack containing the start rule
2. For each input token:
   a. For each active stack:
      - Try to match the token against the current rule
      - If match succeeds, advance the stack
      - If match fails, mark the stack as failed
   b. Remove all failed stacks
   c. If no stacks remain, parsing fails
3. If any stack reaches EOF successfully, parsing succeeds
```

### Handling Ambiguity

When the parser encounters a rule with multiple alternatives, it forks the stack:

```
expr : term '+' expr    // Alternative 1
     | term             // Alternative 2
     ;
```

If the current token is a term, both alternatives are possible. MSLL creates two stacks:
- Stack 1: Try to match `term '+' expr`
- Stack 2: Try to match `term`

Both stacks continue parsing independently. If Stack 1 fails (no `'+'` follows), it is pruned. Stack 2 continues and may succeed.

### Example: Parsing "3 + 4"

```
Input: 3 + 4
Grammar: expr : term '+' expr | term
         term : NUMBER

Step 1: Start with stack [expr]
Step 2: Fork on expr alternatives
  Stack A: [term, '+', expr]
  Stack B: [term]
Step 3: Match NUMBER (3) in both stacks
  Stack A: ['+', expr]  (term matched)
  Stack B: []           (term matched, done)
Step 4: Match '+' in Stack A
  Stack A: [expr]       ('+' matched)
  Stack B: DONE         (successful parse)
Step 5: Stack A continues with "4"
  Stack A: [term]
Step 6: Match NUMBER (4)
  Stack A: []           (term matched, done)
  Stack A: DONE         (successful parse)

Result: Both stacks succeed (ambiguous grammar)
```

In practice, MSLL can be configured to return the first successful parse or all possible parses.

## 4.2 G4 Grammar Compatibility

MSLL aims for broad compatibility with ANTLR4's G4 grammar format to leverage existing grammars.

### Lexer Compatibility

MSLL supports key lexer features:

**Modes**: Lexer modes allow different tokenization rules in different contexts. For example, string literals may use a different mode to handle escape sequences:

```
STRING_START: '"' -> pushMode(STRING_MODE);
mode STRING_MODE;
STRING_CHAR: ~["\n];
STRING_END: '"' -> popMode;
```

**Channels**: Tokens can be sent to different channels (e.g., HIDDEN for whitespace/comments):

```
WS: [ \t\r\n]+ -> channel(HIDDEN);
```

**Fragments**: Reusable lexer rule components:

```
fragment DIGIT: [0-9];
NUMBER: DIGIT+;
```

### Parser Compatibility

MSLL supports standard parser features:

**Alternatives**: Multiple ways to parse a rule:

```
statement : ifStmt | whileStmt | returnStmt;
```

**Quantifiers**: Optional (?), zero-or-more (*), one-or-more (+):

```
parameters : parameter (',' parameter)*;
```

**Labels**: Named elements for semantic actions:

```
expr : left=expr '+' right=expr;
```

### Limitations

MSLL does not support:
- Indirect left recursion (only direct)
- Complex semantic actions with side effects
- Advanced predicates beyond simple boolean checks
- Code generation options (superClass, language targets)

Most real-world grammars use only direct left recursion and simple actions, so these limitations rarely affect practical use.

## 4.3 Left Recursion Handling

Left recursion is a common pattern in expression grammars:

```
expr : expr '+' term
     | expr '-' term
     | term
     ;
```

Traditional LL parsers cannot handle this because they would enter infinite recursion. MSLL handles left recursion through its multi-stack approach.

### Detection

MSLL detects left recursion during grammar analysis by checking if a rule can derive itself as the first symbol:

```
expr → expr ...  (left-recursive)
```

### Handling Strategy

When MSLL encounters a left-recursive rule:

1. **First iteration**: Try the non-left-recursive alternative first (`term`)
2. **Subsequent iterations**: If successful, try left-recursive alternatives (`expr '+' term`)
3. **Termination**: Stop when no more input can be consumed

This approach builds the parse tree bottom-up for left-recursive rules while maintaining top-down parsing for other rules.

### Example: Parsing "1 + 2 + 3"

```
Grammar: expr : expr '+' term | term
         term : NUMBER

Iteration 1: Parse "1" as term
  Result: expr(term(1))

Iteration 2: Try expr '+' term with remaining "+ 2 + 3"
  Match '+', parse "2" as term
  Result: expr(expr(term(1)), '+', term(2))

Iteration 3: Try expr '+' term with remaining "+ 3"
  Match '+', parse "3" as term
  Result: expr(expr(expr(term(1)), '+', term(2)), '+', term(3))

Iteration 4: No more input, done
```

This produces a left-associative parse tree: `((1 + 2) + 3)`.

## 4.4 Error Recovery

When parsing fails, MSLL uses panic-mode error recovery to continue parsing and report multiple errors.

### Panic Mode

When all stacks fail:

1. **Skip tokens**: Discard input tokens until a synchronization point
2. **Rebuild stacks**: Create new stacks at the synchronization point
3. **Continue parsing**: Resume parsing from the synchronized position

### Synchronization Points

MSLL uses common statement terminators as synchronization points:
- Semicolons (`;`)
- Closing braces (`}`)
- Newlines (in line-oriented languages)

### Example

```
Input: let x = ; let y = 5;
                ^
                Error: missing expression

Recovery:
1. Detect error at ';' (expected expression)
2. Skip to next ';'
3. Resume parsing at "let y = 5;"
4. Report error but continue parsing
```

This allows MSLL to report multiple errors in a single pass, improving the development experience.

## 4.5 Design Summary

MSLL's design combines:
- **Multi-stack parsing** for handling ambiguity and left recursion
- **G4 compatibility** for leveraging existing grammars
- **Runtime interpretation** for instant iteration
- **Error recovery** for better error reporting

The design prioritizes simplicity and usability over performance, making MSLL suitable for grammar development and educational use.