# ANTLR `grammars-v4` Compatibility

*Companion data for MSLL's RQ1 "how much of real-world ANTLR4 does MSLL
accept, unmodified?" Updated by running:*

```bash
mvn -q test -Dtest=GrammarsV4CompatTest
# → target/grammars-v4-compat.md (auto-regenerated)
```

This page pins the current state, interprets the numbers, and maps each
failing grammar to the architectural limitation it exposes.

## 1. Current matrix

Run on 10 vendored grammars from ANTLR's `grammars-v4` repository (plus
two minimal grammars we wrote ourselves to stress specific features).

| Grammar      | Stage | Samples | Notes                                                        |
|--------------|-------|---------|--------------------------------------------------------------|
| calculator   | OK    | 2/2     | Float, scientific notation; char-ranges `'0'..'9'` (PR-3).   |
| csv          | OK    | 1/1     | Newlines are real tokens (PR-1).                             |
| json         | OK    | 2/2     | Nested objects/arrays, fragments, Unicode escapes.           |
| pystring     | OK    | 1/1     | Triple-quoted strings spanning multiple physical lines (PR-2). |
| sexpression  | OK    | 1/1     | Atom/list recursion; char-ranges (PR-3).                     |
| url          | OK    | 1/1     | The original smoke test.                                     |
| abnf         | PARSE | 0/1     | **Limitation L1** — non-adaptive lookahead (§3).             |
| focal        | PARSE | 0/1     | **Limitation L2** — ambiguous maximal munch.                 |
| properties   | PARSE | 0/1     | **Limitation L3** — no lexer modes.                          |
| ini          | PARSE | 0/1     | **Limitation L3** — no lexer modes.                          |

**Summary: 6/10 grammars fully compatible, unmodified.**

## 2. What changed to reach 6/10

The initial run landed at **1/3** (URL only). Three focused fixes took it
to the current number, each scoped to one architectural surface and each
guarded by a regression test alongside the vendored grammar that forced
the fix.

### PR-0: Grammar-loading state leak
`MyParserBuilder` shared singleton `Terminals`/`NonTerminals` instances
across invocations, silently accumulating terminal tables from one
grammar load into the next. Two consecutive loads produced a "ghost
state" that broke any grammar-specific predicate table. Fixed by adding
`newMy()` factories and wiring `MyParserBuilder` to fresh instances per
load. **Regression test:** `NoGrammarLeakTest`.

### PR-1: Newline-as-token synthesis (*csv, properties, ini, pystring*)
MSLL's `RegexLexer` is line-oriented: the line separator is consumed
internally and never surfaces as a token. Grammars that treat `\n` as
syntactically significant (CSV rows, property entries, INI lines)
therefore failed at the parser.

*Fix.* Probe the grammar's terminal table once per load. If any terminal
matches the literal `\n` lexeme (in any of its ANTLR-equivalent forms:
`'\n'`, `'\\n'`, `'\r\n'`, `'\\r\\n'`), synthesise that terminal on every
line boundary. Grammars without such a terminal see no change.
**Regression test:** `NewlineTokenSynthesisTest`.

### PR-2: Multi-line token cache (*pystring, block comments*)
`CodeCache` used to hard-code a single `/*...*/` delimiter pair and only
triggered multi-line mode when a line literally started with `/*`. That
blocked Python / Kotlin / Scala triple-quoted strings, and any
`/*...*/` that opened mid-line.

*Fix.* Replace the hard-coded rule with an ordered delimiter table
(`"""..."""`, `'''...'''`, `/*...*/`), scan the earliest opener anywhere
on the line, honour in-line balancing, and accumulate unmatched openers
across physical lines. The external contract — `isMultiLine()` plus
`getLine()` returning the joined logical line — is unchanged, so
`RegexLexer` was not touched. **Regression test:** `MultiLineTokenTest`.

### PR-3: G4 char-range rewrite (*calculator, sexpression*)
ANTLR4's `'X' .. 'Y'` range shorthand reached `LexerRuleCompiler` as
three adjacent string literals rather than a character class. Added a
pre-processing step in `G4ToGMConverter` that rewrites the range into a
standard `[X-Y]` class before compilation.

## 3. What the remaining failures tell us

The four PARSE failures are not bugs in the conversion pipeline; they
pinpoint three *intrinsic* MSLL architectural choices. Each is an honest
limitation to name in the paper's discussion, not something to paper
over in the matrix.

### L1 — Non-adaptive LL lookahead (*abnf*)
ANTLR4's adaptive LL(\*) resolves ambiguity by exploring the input
arbitrarily far ahead; MSLL commits to a fixed-length prediction. The
ABNF grammar's `rule_ : ID '=' '/'? elements ; root : rule_* ;`
repetition is unambiguous with adaptive lookahead (ANTLR scans forward,
sees the next `ID '='`, and starts a fresh `rule_`) but unambiguous*ly*
fails under MSLL: once inside the first `rule_`'s `elements`, the parser
has no way to know the next `ID '='` is a boundary and chokes on `=`.
This is inherent to the parsing class and will not be fixed without
adopting a more expressive prediction automaton.

### L2 — Ambiguous maximal munch across lexer rules (*focal*)
When two lexer rules can match the same prefix of the same length
(e.g. `DIGIT : [0-9] ;` and `INTEGER : [0-9]+ ;` on a single-digit
input), ANTLR4 breaks the tie in favour of the rule declared *first*
while still allowing later rules to win on longer inputs. MSLL's
first-match-wins behaviour is coarser and does not recompute when a
shorter match would satisfy the current parse state. Focal's numeric
rules trip over this.

### L3 — No lexer modes (*properties, ini*)
Real `.properties` and `.ini` grammars lean on ANTLR4 **lexer modes** to
flip into a "rest-of-line value" sub-lexer once `=` or `:` has been
seen. Without modes, `KEY : [A-Za-z_][A-Za-z_0-9.\-]*` and
`VALUE : ~[\r\n]+` compete for the same prefix (`host` in
`host=localhost`), and the first-declared wins, breaking the entry.
Adding lexer modes is a well-scoped piece of future work (PR-4) but
reaches well beyond the current PR-1/PR-2/PR-3 fix set.

## 4. Reading the table for the paper

> *MSLL accepts 6 of 10 representative ANTLR4 grammars-v4 samples
> unmodified. The four rejected grammars each pinpoint a known
> limitation of MSLL's parsing class or lexer (Table X, §Y). None of
> the rejected grammars required changes to the converted `.gm` output
> itself; every failure surfaced at either the fixed-lookahead parser
> (ABNF), the first-match-wins lexer (Focal), or the absence of lexer
> modes (properties, ini).*

That framing matches what a CCF-B reviewer expects: compatibility
matrix with honest failure attribution, not a cherry-picked 100% score.

## 5. Reproducibility

- Grammars vendored under `src/test/resources/grammars-v4/<name>/`.
- Each case is one directory with one or more `.g4` files and a
  `samples/` folder.
- `GrammarsV4CompatTest` auto-discovers subdirectories, runs them
  through `G4GrammarLoader.loadG4String`, parses each sample, and
  regenerates this table at `target/grammars-v4-compat.md`.
- Probe-level regressions for each PR live next to the harness:
  `NoGrammarLeakTest`, `NewlineTokenSynthesisTest`,
  `MultiLineTokenTest`, `LexerFeatureProbeTest`.
