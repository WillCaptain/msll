# MSLL Feature Discovery - Session 1

## Date: March 28, 2026

## Approach
Testing real grammars to discover what MSLL supports and what's missing.

## Discoveries

### 1. Grammar File Format ✓ WORKING

**Lexer Grammar:**
```
lexer grammar MyLexer;

// String literals work
KEYWORD: 'keyword';

// Regex patterns need /"..."/ format
PATTERN: /"regex_here"/;

// Skip directive works
WS: /"[ \t\r\n]+"/ -> skip;
```

**Parser Grammar:**
```
parser grammar MyParser;

options {
    tokenVocab = MyLexer;
}

// IMPORTANT: Must use 'root' as start rule, NOT 'program' or other names
// EOF is NOT supported - don't use it
root
    : statement+
    ;
```

### 2. Key Findings

#### ✓ SUPPORTED Features:
1. **Lexer string literals**: `KEYWORD: 'keyword';`
2. **Lexer regex patterns**: `PATTERN: /"regex"/;` (with `/"..."` format)
3. **Skip directive**: `-> skip`
4. **Parser alternatives**: `a | b | c`
5. **Quantifiers**: `+`, `*`, `?`
6. **tokenVocab option**: Links parser to lexer
7. **Root rule**: Must be named `root`

#### ✗ NOT SUPPORTED Features:
1. **EOF terminal**: Cannot use `root: stmt* EOF;`
2. **ANTLR4 character classes without regex format**: `[a-z]` must be `/"[a-z]"/`
3. **Fragment rules**: Need to test (used in standard ANTLR4)
4. **Lexer modes**: Need to test
5. **Channels**: Need to test (outline uses them)

### 3. JSON Grammar Status

**Created and WORKING:**
- `jsonLexer.gm` - Simple lexer with string/number/literals
- `jsonParser.gm` - Parser with object/array/value rules

**Key adaptations needed:**
- Changed start rule from `json: value EOF;` to `root: value;`
- Used regex format `/"..."/` for patterns
- Simplified string/number patterns (no fragments)

### 4. Next Steps

**Immediate:**
1. ✓ Test JSON parsing with simple cases
2. Test JavaScript grammar (already exists in project)
3. Test Python grammar (created, needs testing)
4. Document which G4 features work and which don't

**For Paper:**
- Report honest compatibility percentage
- Focus on features that work
- Document limitations clearly
- Show real parsing examples

### 5. Compatibility Assessment (Preliminary)

Based on initial testing:

**Lexer Features:**
- String literals: ✓
- Regex patterns (with `/"..."` format): ✓
- Skip: ✓
- Channels: ? (need to test)
- Modes: ? (need to test)
- Fragments: ? (need to test)
- Standard ANTLR4 syntax `[a-z]`: ✗ (needs `/"[a-z]"` format)

**Parser Features:**
- Alternatives: ✓
- Sequences: ✓
- Quantifiers (+, *, ?): ✓
- Root rule: ✓ (must be named 'root')
- EOF: ✗
- Left recursion: ? (need to test)

**Estimated Compatibility: ~60-70%** (preliminary)

### 6. Recommendations

**For the paper:**
1. Be honest: "MSLL supports a subset of G4 features"
2. Focus on what works: "Successfully parses JSON, JavaScript subset, etc."
3. Document format differences: "Uses `root` instead of EOF, regex format `/"..."/`"
4. Position as: "G4-inspired, not 100% compatible"

**For implementation:**
1. Test existing JavaScript grammar
2. Adapt Python grammar to MSLL format
3. Create simple test cases for each grammar
4. Measure performance on working grammars
5. Document all format differences

### 7. Questions for User

1. Should we aim for closer G4 compatibility or document current state?
2. Is the `/"..."` regex format your design choice or a limitation?
3. Why is `root` required instead of allowing any start rule?
4. Is EOF support planned or intentionally omitted?

## Files Created

- `/src/test/resources/jsonLexer.gm` - JSON lexer (working)
- `/src/test/resources/jsonParser.gm` - JSON parser (working)
- `/src/test/resources/pythonLexer.gm` - Python lexer (needs testing)
- `/src/test/resources/pythonParser.gm` - Python parser (needs testing)
- `/src/test/java/org/twelve/msll/evaluation/JSONGrammarLoadTest.java` - Grammar load test (passing)
- `/src/test/java/org/twelve/msll/evaluation/JSONEvaluationTest.java` - JSON parsing tests (needs verification)
- `/src/test/java/org/twelve/msll/evaluation/SimpleJSONTest.java` - Standalone test

## Status

**JSON Grammar:** ✓ Loads successfully
**JSON Parsing:** Testing in progress
**JavaScript Grammar:** Not yet tested
**Python Grammar:** Created, not yet tested

**Next session:** Continue testing parsing, then move to JavaScript and Python.