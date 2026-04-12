# MSLL Evaluation Test Results
## Date: March 29, 2026

## Test Summary

### JavaScript Grammar (Simple Subset)
**Status:** ✅ ALL TESTS PASSED

**Grammar Files:**
- `javascriptLexer-simple.gm` - Simplified JavaScript lexer
- `javascriptParser-simple.gm` - Simplified JavaScript parser

**Test Results:**
1. ✅ test_simple_variable - `let x = 5;`
2. ✅ test_function_declaration - `function add(a, b) { return a + b; }`
3. ✅ test_left_recursive_expression - `let result = a + b * c - d;`
4. ✅ test_if_statement - `if (x > 0) { return x; }`
5. ✅ test_performance_medium_js - 1000 statements

**Performance Data:**
- Input: 1000 variable declarations (`let var0 = 0; let var1 = 1; ...`)
- Tokens: ~5000 tokens (5 tokens per statement)
- Time: 72 ms
- **Throughput: 69,444 tokens/sec**

**Notes:**
- This is a simplified JavaScript grammar, not the full JavaScript spec
- Supports: variables, functions, expressions, if/while statements
- Does NOT support: complex features like template strings, async/await, classes, etc.
- Left recursion in expressions works correctly

### JSON Grammar
**Status:** ❌ TESTS FAILED - Parser hangs

**Grammar Files:**
- `jsonLexer.gm` - JSON lexer
- `jsonParser.gm` - JSON parser

**Issues:**
- Parser hangs when trying to parse even simple JSON objects like `{}`
- Lexer loads successfully
- Problem appears to be in the parser, possibly related to how MSLL handles certain grammar patterns
- Attempted fixes:
  - Simplified grammar to minimal subset
  - Fixed NUMBER regex
  - Still hangs during parse() call

**Conclusion:**
- JSON grammar needs further debugging
- May require changes to grammar structure or MSLL parser implementation
- Skipped for now due to time constraints (user requirement: no test > 10 seconds)

## Comparison with Paper Claims

### Paper Claims (Section 6.3):
| Grammar | Claimed Throughput |
|---------|-------------------|
| JSON | 28,000 tok/s |
| JavaScript | 22,000 tok/s |
| Python | 19,000 tok/s |
| Java | 18,000 tok/s |
| Outline | 25,000 tok/s |
| **Average** | **22,400 tok/s** |

### Actual Test Results:
| Grammar | Actual Throughput | Status |
|---------|------------------|--------|
| JavaScript (simple) | 69,444 tok/s | ✅ Tested |
| JSON | N/A | ❌ Hangs |
| Python | Not tested | ⏳ Pending |
| Java | Not tested | ⏳ Pending |
| Outline | ~25,000 tok/s | ✅ Existing tests |

### Analysis:

**Why is JavaScript throughput higher than claimed?**
1. **Simpler grammar**: Our simplified JavaScript grammar is much simpler than a full JavaScript grammar
2. **Simpler input**: Variable declarations are simpler to parse than complex expressions, functions, etc.
3. **Small scale**: 5000 tokens is relatively small; larger inputs might show different performance
4. **JVM warmup**: Short tests may benefit from JVM optimizations

**Recommendations:**
1. Test with larger inputs (100K+ tokens) to get more realistic throughput numbers
2. Test with more complex JavaScript code (nested functions, complex expressions)
3. Use the full JavaScript grammar (not simplified) for accurate comparison
4. Fix JSON grammar issues before including in paper

## Test Infrastructure

**Files Created:**
- `JavaScriptEvaluationTest.java` - JUnit tests for JavaScript
- `JSONEvaluationTest.java` - JUnit tests for JSON (not working)
- `SimpleJSONTest.java` - Standalone JSON test
- `DebugJSONTest.java` - Debug version with step-by-step output
- `DebugJSONSimpleTest.java` - Minimal JSON grammar test
- `DebugJSONLexerTest.java` - Lexer-only test (compilation error)

**Grammar Files Created:**
- `javascriptLexer-simple.gm` - Working
- `javascriptParser-simple.gm` - Working
- `jsonParser-simple.gm` - Still hangs

## Next Steps

### Immediate (to complete evaluation):
1. ❌ Fix JSON grammar hanging issue
2. ⏳ Create and test Python grammar
3. ⏳ Test with larger inputs (100K+ tokens)
4. ⏳ Measure memory usage
5. ⏳ Test full JavaScript grammar (not simplified)

### For Paper:
1. ✅ Can use JavaScript test results (with caveats about simplified grammar)
2. ✅ Can use existing Outline test results
3. ❌ Cannot use JSON results (not working)
4. ⏳ Need Python and Java results
5. ⏳ Need to clarify that some results are from simplified grammars

### Alternative Approach:
Since we have limited time and JSON is problematic:
1. Focus on JavaScript (working) and Outline (existing)
2. Document that evaluation used simplified grammars
3. Note JSON issues as "future work" or "known limitations"
4. Adjust paper claims to match actual test results
5. Be honest about limitations rather than claiming untested results

## Conclusion

We successfully tested JavaScript grammar with MSLL and achieved good results (69K tokens/sec). However, JSON grammar has issues that need more investigation. The paper's evaluation section should be updated to reflect:
1. Actual test results (not estimates)
2. Clarification that some grammars are simplified subsets
3. Known limitations (JSON hanging issue)
4. Honest assessment of what was tested vs. what was claimed
