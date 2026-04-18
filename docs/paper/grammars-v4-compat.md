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

Run on 11 vendored grammars from ANTLR's `grammars-v4` repository (plus
two minimal grammars we wrote ourselves to stress specific features).

| Grammar      | Stage | Samples | Notes                                                        |
|--------------|-------|---------|--------------------------------------------------------------|
| abnf         | OK    | 1/1     | FIRST/FOLLOW auto-fork (PR-L1); previously failed on L1.     |
| calculator   | OK    | 2/2     | Float, scientific notation; char-ranges `'0'..'9'` (PR-3).   |
| csv          | OK    | 1/1     | Newlines are real tokens (PR-1).                             |
| json         | OK    | 2/2     | Nested objects/arrays, fragments, Unicode escapes.           |
| pystring     | OK    | 1/1     | Triple-quoted strings spanning multiple physical lines (PR-2). |
| sexpression  | OK    | 1/1     | Atom/list recursion; char-ranges (PR-3).                     |
| url          | OK    | 1/1     | The original smoke test.                                     |
| focal        | PARSE | 0/1     | **Residual L2′** — built-in STRING terminal conflict.        |
| properties   | PARSE | 0/1     | **Limitation L3** — no lexer modes.                          |
| ini          | PARSE | 0/1     | **Limitation L3** — no lexer modes.                          |
| javascript   | LOAD  | 0/1     | **Limitation L4** — embedded target-language actions.        |

**Summary: 7/11 grammars fully compatible, unmodified.**

The `javascript` row uses the *unmodified, upstream* `JavaScriptLexer.g4`
and `JavaScriptParser.g4` pulled straight from
`antlr/grammars-v4/javascript/javascript/`. We include it deliberately
as a negative result: it is the first grammar in the matrix big enough
to exercise MSLL's remaining major gap (L4), and keeps us honest about
what "ANTLR4 compatibility" actually takes.

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

### PR-L1: Auto FIRST/FOLLOW fork (*abnf*)
MSLL already owns a multi-stack runtime — at a non-terminal with
multiple matching productions, it forks the stack, explores in parallel,
and prunes forks that die on subsequent tokens. The pre-existing code
however *filtered out* ε productions unless the `(grammar, terminal)`
pair was on a hand-curated whitelist (`epsilonAlongsideGrammars`), so
classical LL(1) conflicts (Kleene closure over a token also in the
closure's FOLLOW set) silently took the greedy branch and broke
grammars like ABNF where the greedy branch is wrong.

*Fix.* When `PredictTable` is built, scan every cell: if it holds both
an ε and a non-ε production, remember it. The G4 loader flips
`setAutoEpsilonAlongsideEnabled(true)` so any such cell keeps ε
alongside non-ε at parse time; the multi-stack runtime then forks and
lets whichever branch consumes the rest of the input win. Existing
legacy `.gm` grammars keep the byte-exact old behaviour by leaving the
flag off. **Regression tests:** `FirstFollowConflictForkTest`,
`AbnfProbeTest`.

### PR-L2: Cross-rule inlining in `LexerRuleCompiler` (*focal*)
MSLL's lexer-rule compiler was only inlining `fragment` rules. Any
*non-fragment* lexer rule that referenced another lexer rule fell
through to `Pattern.quote(name)` — the reference got emitted as a
literal string match against the rule *name*. Under focal.g4's
`INTEGER : DIGIT+` and `VARIABLE : ALPHA (ALPHA | DIGIT)*`, the
compiled INTEGER pattern matched the literal string "DIGIT" and could
not match any digits at all; the lexer then had no choice but to
produce the primitive `DIGIT` token, which the parser was not expecting.
This was the real cause of the apparent "first-match-wins lexer tie
break" symptom we originally labelled L2.

*Fix.* Treat every regular lexer rule the same way fragments are
treated: register its compiled regex into the shared `fragments` map so
that subsequent rules inline it on reference. Forward references are
resolved by iterating the compile pass until the map stabilises (a
fixed-point loop bounded by the rule count). **Regression test:**
`LexerRuleInliningTest`.

## 3. What the remaining failures tell us

The four PARSE failures are not bugs in the conversion pipeline; they
pinpoint three *intrinsic* MSLL architectural choices. Each is an honest
limitation to name in the paper's discussion, not something to paper
over in the matrix.

### L1 — ~~Non-adaptive LL lookahead~~ Resolved via multi-stack fork (PR-L1)
*Originally attributed to fixed-length lookahead.* In fact MSLL's
multi-stack runtime is expressive enough to cover the ABNF case — it
just needed to be **told** the FIRST/FOLLOW cell was ambiguous. PR-L1
now detects those cells at table-build time and flips the runtime into
fork-both-branches mode for G4-loaded grammars, and ABNF parses
unmodified. True adaptive LL(\*) remains future work, but the
commonly-cited symptom (Kleene closure whose continuation token is
also in FOLLOW) is covered.

### L2 — ~~Ambiguous maximal munch~~ Cross-rule inlining (PR-L2)
*Originally attributed to lexer tie-breaking.* Diagnostics showed the
real issue: MSLL was only inlining rules declared `fragment`; regular
lexer rules referenced from another rule body were silently emitted as
literal `Pattern.quote(name)` matches. `INTEGER : DIGIT+` compiled to a
regex that matched the literal string "DIGIT", which of course never
matched actual input, so the primitive `DIGIT` rule was the only one
that could fire. PR-L2 makes every lexer rule inlineable, resolves
forward references by fixed-point iteration, and focal's numeric rules
parse correctly.

*Residual L2′.* Focal still fails at the vendored sample, but one token
later and for a different reason: MyParser installs a built-in
`STRING` terminal (`"..."`) for Outline-style languages, and that
terminal out-competes focal's own `STRING_LITERAL`. This is a
cross-contamination between MSLL's built-in lexer seed and G4-loaded
grammars, not a lexer-tie-breaking limitation; fixing it is scoped as
a follow-up.

### L3 — No lexer modes (*properties, ini*)
Real `.properties` and `.ini` grammars lean on ANTLR4 **lexer modes** to
flip into a "rest-of-line value" sub-lexer once `=` or `:` has been
seen. Without modes, `KEY : [A-Za-z_][A-Za-z_0-9.\-]*` and
`VALUE : ~[\r\n]+` compete for the same prefix (`host` in
`host=localhost`), and the first-declared wins, breaking the entry.
Adding lexer modes is a well-scoped piece of future work (PR-4) but
reaches well beyond the current PR-1/PR-2/PR-3 fix set.

### L4 — Embedded target-language actions (*javascript*)
The upstream grammars-v4 `JavaScriptLexer.g4` / `JavaScriptParser.g4`
interleave the grammar with Java/JavaScript action blocks (`{...}`)
and semantic predicates (`{expr}?`) — `{this.lineTerminatorAhead()}?`,
`{this.IsInTemplateString()}?`, `{this.ProcessOpenBrace();}`, etc.
ANTLR4's *generator* compiles these to runtime code; MSLL is a
*runtime* interpreter and currently (a) has no target-language
evaluator wired in and (b) the lift/convert pipeline does not fully
strip or stub these inline blocks, so an internal token (`||` inside
an alternative whose predicate got mangled) surfaces back to the
`.gm` loader. Two paths forward for a future PR-5: (i) treat every
`{...}` as a no-op at convert time and drop semantic predicates to
"always true" — sound for parse-shape evaluation, lossy for the
grammars that actually depend on runtime state; (ii) add a minimal
predicate stub runtime. Neither is required for the core MSLL claim
and both are explicitly listed as non-goals of the current paper.

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
  `MultiLineTokenTest`, `LexerFeatureProbeTest`,
  `FirstFollowConflictForkTest` (PR-L1), `LexerRuleInliningTest` (PR-L2).
