# MSLL Lexer Grammar Reference

A lexer grammar file defines how raw text is split into tokens.  
MSLL uses `.gm` files with near-identical syntax to ANTLR4 (G4).

---

## File Structure

```antlr
/* optional block comment */
lexer grammar MyLexer;        // ← grammar declaration (required)

channels { HIDDEN, ERROR }    // ← optional channel declarations

options { ... }               // ← optional options block

// — token rules follow —
RuleName : body -> command? ;
```

---

## Token Rules

### String Literals

Use single or double quotes for exact-match tokens.  
Keywords must be defined **before** more general identifier rules so the
**Keyword Priority** principle picks them first.

```antlr
// keywords
Let    : 'let';
Const  : 'const';
Return : 'return';

// operators
Arrow  : '->';
Dot    : '.';
```

### Regex Patterns

Wrap patterns in `/"…"/` for full Java-regex power,  
or write ANTLR-style character classes directly.

```antlr
// ANTLR-style character class
Digit      : [0-9];
HexDigit   : [0-9a-fA-F];
Letter     : [a-zA-Z_];

// Java-regex wrapped in /"…"/
NUMBER     : /"(\+|\-)?0|[1-9][0-9_]*"/;
ID         : /"(\b[a-z_]\w*)"/;

// any character (matches newlines too, like ANTLR's `.`)
AnyChar    : .;

// single quoted character
Newline    : '\n';
```

### Quantifiers

All ANTLR4 quantifiers are supported, including **lazy** variants.

| Syntax | Meaning              |
|--------|----------------------|
| `a*`   | zero or more (greedy)|
| `a+`   | one or more (greedy) |
| `a?`   | zero or one          |
| `a*?`  | zero or more (lazy)  |
| `a+?`  | one or more (lazy)   |

```antlr
// greedy vs lazy — critical for comments
BlockComment  : '/*' .*? '*/' ;   // lazy: stops at first */
BlockCommentG : '/*' .* '*/'  ;   // greedy: would consume too much
```

### Character Classes

```antlr
Vowel       : [aeiou];           // any vowel
NonDigit    : ~[0-9];            // negation: anything that is NOT a digit
NonNewline  : ~[\r\n];           // anything except CR / LF
```

### Grouping and Alternation

```antlr
Sign        : ('+' | '-');
HexPrefix   : '0' ('x' | 'X');
```

---

## Fragment Rules

Fragments are **building blocks** — they produce no tokens on their own
but can be referenced by other rules.  This is MSLL's equivalent of ANTLR4's
`fragment` keyword (and analogous to msll's internal `ruleName'` convention).

```antlr
// Fragments — reusable components
fragment HexDigit       : [0-9a-fA-F];
fragment DecimalDigit   : [0-9];
fragment IdentStart     : [a-zA-Z_$];
fragment IdentPart      : [a-zA-Z0-9_$];

// Rules that reference fragments
HexLiteral  : '0' [xX] HexDigit+;
DecLiteral  : DecimalDigit+;
Identifier  : IdentStart IdentPart*;

// Fragments used for string escapes
fragment DoubleStringChar
    : ~["\\\r\n]          // any char except quote / backslash / newline
    | '\\' ['"\\bfnrtv]   // recognised escape sequences
    ;
StringLiteral : '"' DoubleStringChar* '"';
```

---

## Lexer Commands

Lexer commands appear after `->` at the end of a rule body.

### `-> channel(NAME)` — Route to a channel

Tokens routed to a channel are removed from the default token stream but
remain accessible (useful for comments, whitespace, etc.).

```antlr
channels { HIDDEN, ERROR }

WhiteSpace         : [ \t\r\n]+        -> channel(HIDDEN);
BlockComment       : '/*' .*? '*/'     -> channel(HIDDEN);
LineComment        : '//' ~[\r\n]*     -> channel(HIDDEN);
```

### `-> skip` — Discard immediately

Like `-> channel(HIDDEN)` but the token is thrown away entirely.

```antlr
WS : [ \t\r\n]+ -> skip;
```

### `-> type(TOKEN)` — Relabel the token type

Emit the matched text under a different terminal name.  
Commonly used to unify tokens across lexer modes.

```antlr
// Inside TEMPLATE mode, a closing backtick is relabelled as BackTick
BackTickInside : '`' -> type(BackTick), popMode;
```

### `-> pushMode(MODE)` / `-> popMode` — Mode transitions

See the [Lexer Modes](#lexer-modes) section below.

---

## Lexer Modes

Modes let you switch the active set of token rules based on context —
essential for constructs like template strings or embedded XML.

```antlr
lexer grammar TemplateLexer;

// ── DEFAULT_MODE ──────────────────────────────────────────────
BackTick  : '`' -> pushMode(TEMPLATE);   // enter template string
ID        : [a-zA-Z_][a-zA-Z0-9_]*;
WS        : [ \t\r\n]+ -> skip;

// ── TEMPLATE mode ─────────────────────────────────────────────
mode TEMPLATE;

BackTickInside             : '`'  -> type(BackTick), popMode;       // end of template
TemplateExpressionStart    : '${'  -> pushMode(DEFAULT_MODE);       // ${...} expression
TemplateStringAtom         : ~[`\\$]+;                              // plain text content
TemplateStringEscapedChar  : '\\' . ;                               // \n, \t, etc.
```

When inside TEMPLATE mode, **only TEMPLATE mode rules are active**.
`pushMode(DEFAULT_MODE)` re-activates JS tokens inside `${…}`.

---

## Full Example — A Simple Expression Lexer

```antlr
lexer grammar ExprLexer;

channels { HIDDEN }

// — whitespace (hidden) ————————————————————
WS : [ \t\r\n]+ -> channel(HIDDEN);

// — operators —————————————————————————————
Plus    : '+';
Minus   : '-';
Star    : '*';
Slash   : '/';
LParen  : '(';
RParen  : ')';

// — literals ——————————————————————————————
fragment Digit   : [0-9];
fragment Digits  : Digit+;
fragment Frac    : '.' Digits;
fragment Exp     : [eE] [+\-]? Digits;

Number : Digits Frac? Exp?;
```

---

## Token Priority Rules

When multiple rules could match at the same position, MSLL picks the winner
by applying three rules **in order**:

| Priority | Rule | Example |
|----------|------|---------|
| 1 | **Maximal Munch** — longest match wins | `<=` beats `<` |
| 2 | **Keyword Priority** — string literal beats regex | `let` keyword beats ID regex |
| 3 | **Definition Order** — first defined wins on ties | order your rules intentionally |

```antlr
// Keywords MUST come before the ID regex rule
If    : 'if';
Else  : 'else';
While : 'while';
ID    : [a-zA-Z_][a-zA-Z0-9_]*;   // would otherwise match keywords too
```

---

## Channels Reference

| Channel | Typical use |
|---------|-------------|
| (default) | Tokens passed to the parser |
| `HIDDEN` | Whitespace, comments — ignored by parser, accessible for tools |
| `ERROR` | Unrecognised characters — enables error recovery |

```antlr
channels { HIDDEN, ERROR }

LineComment : '//' ~[\r\n]* -> channel(HIDDEN);
ErrorChar   : .             -> channel(ERROR);   // catch-all for unknowns
```
