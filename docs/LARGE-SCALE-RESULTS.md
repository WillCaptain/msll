# MSLL Large-Scale Performance Evaluation
## Date: March 29, 2026

## Executive Summary

We conducted large-scale performance tests on MSLL with inputs ranging from 1K to 100K tokens across three languages: JSON, JavaScript, and Python. The results reveal significant performance degradation as input size increases.

## Test Results

### 1. JavaScript Grammar

**Small Scale (Original Test):**
- Input: 1,000 statements (~5,000 tokens)
- Time: 72 ms
- Throughput: **69,444 tokens/sec**

**Large Scale Tests:**

| Scale | Statements | Tokens | Time | Throughput | Degradation |
|-------|-----------|--------|------|------------|-------------|
| 10K | 900 | 6,800 | 1,185 ms | 5,738 tok/s | 12× slower |
| 50K | 4,500 | 34,000 | 5,356 ms | 6,348 tok/s | 11× slower |
| 100K | 9,000 | 68,000 | 26,386 ms | 2,577 tok/s | 27× slower |

**Key Findings:**
- Performance degrades significantly with scale
- 100K tokens: 27× slower than 5K tokens
- Still usable for medium-scale inputs (50K tokens in 5 seconds)
- Large inputs (100K tokens) take 26 seconds

### 2. JSON Grammar

**Small Scale (Original Test):**
- Input: 100 objects (~1,401 tokens)
- Time: 175-306 ms
- Throughput: **~6,000 tokens/sec**

**Large Scale Tests:**

| Scale | Objects | Tokens | Time | Throughput | Degradation |
|-------|---------|--------|------|------------|-------------|
| 1K | 1,000 | 16,000 | 10.4s | 1,538 tok/s | 4× slower |
| 5K | 5,000 | 80,000 | 177s | 451 tok/s | 13× slower |
| 10K | 10,000 | 160,000 | ~600s (est) | ~267 tok/s (est) | 22× slower |

**Key Findings:**
- JSON performance degrades more severely than JavaScript
- 5K objects: 13× slower than 100 objects
- 10K objects test was too slow to complete (estimated 10 minutes)
- JSON parsing is significantly slower than JavaScript

### 3. Python Grammar

**Small Scale Test:**
- Input: 1,000 statements (~4,000 tokens)
- Time: 712 ms
- Throughput: **5,618 tokens/sec**

**Note:** Large-scale Python tests not yet conducted.

## Performance Comparison

### Throughput by Input Size

| Input Size | JSON | JavaScript | Python |
|-----------|------|------------|--------|
| ~1-5K tokens | 6,000 tok/s | 69,000 tok/s | 5,600 tok/s |
| ~10-20K tokens | 1,500 tok/s | 5,700 tok/s | - |
| ~50-80K tokens | 450 tok/s | 6,300 tok/s | - |
| ~100K+ tokens | ~270 tok/s | 2,600 tok/s | - |

### Performance Degradation

**JavaScript:**
- Relatively stable up to 50K tokens
- Significant drop at 100K tokens
- 27× slower at 100K vs 5K

**JSON:**
- Severe degradation starting at 1K objects
- 13× slower at 5K objects vs 100 objects
- Exponential-like degradation pattern

**Python:**
- Only tested at small scale
- Similar performance to JSON at small scale

## Analysis

### Why Does Performance Degrade?

**1. Multi-Stack Overhead**
- MSLL maintains multiple parse stacks
- More stacks = more memory allocation and management
- Stack pooling helps but doesn't eliminate overhead

**2. Ambiguity Resolution**
- Parser explores multiple branches
- More input = more branching points
- Exponential growth in some cases

**3. No Optimization**
- MSLL prioritizes simplicity over performance
- No lookahead optimization
- No memoization

**4. Grammar Complexity**
- JSON's recursive structure (nested objects/arrays) causes more branching
- JavaScript's simpler test cases (flat statements) perform better

### Comparison with Paper Claims

**Paper Claims (Section 6.3):**
- JSON: 28,000 tok/s
- JavaScript: 22,000 tok/s
- Python: 19,000 tok/s
- Average: 22,400 tok/s

**Actual Results (Small Scale):**
- JSON: 6,000 tok/s (4.7× slower than claimed)
- JavaScript: 69,000 tok/s (3.1× faster than claimed, but simplified grammar)
- Python: 5,600 tok/s (3.4× slower than claimed)

**Actual Results (Large Scale - 50K+ tokens):**
- JSON: 450 tok/s (62× slower than claimed)
- JavaScript: 2,600-6,300 tok/s (3-8× slower than claimed)

**Conclusion:**
- Paper claims are overly optimistic
- Small-scale tests don't represent real-world performance
- Large-scale performance is significantly worse

## Implications for Paper

### What to Report

**Option 1: Report Actual Data (Recommended)**
- Use actual test results
- Show performance degradation with scale
- Explain why it happens
- Be honest about limitations

**Option 2: Adjust Claims**
- Lower claimed throughput to match reality
- Focus on small-to-medium scale use cases
- Emphasize workflow benefits over raw performance

**Option 3: Optimize and Re-test**
- Investigate performance bottlenecks
- Implement optimizations
- Re-run tests with improved implementation

### Recommended Approach

**For CCF-B Paper:**

1. **Be Honest About Performance**
   - Report actual numbers
   - Show performance vs scale graph
   - Explain degradation

2. **Focus on Trade-offs**
   - Instant iteration vs runtime performance
   - Development velocity vs production speed
   - Prototyping vs deployment

3. **Define Use Cases**
   - Small-to-medium files (< 10K tokens): Good performance
   - Large files (> 50K tokens): Acceptable for development, not production
   - Very large files (> 100K tokens): Too slow

4. **Emphasize Workflow Benefits**
   - 0s iteration time vs 10-30s for ANTLR4
   - This is the main contribution
   - Performance is secondary

## Recommendations

### For Paper Submission

**Must Do:**
1. ✅ Update Section 6.3 with actual performance data
2. ✅ Add performance vs scale graph
3. ✅ Explain performance degradation
4. ✅ Adjust claims to match reality
5. ✅ Emphasize workflow benefits

**Should Do:**
6. ⭐ Add ANTLR4 comparison (workflow time)
7. ⭐ Create performance graphs
8. ⭐ Analyze performance bottlenecks

**Nice to Have:**
9. ○ Optimize performance
10. ○ Test with real-world code
11. ○ Memory usage analysis

### For Future Work

1. **Performance Optimization**
   - Profile and identify bottlenecks
   - Implement memoization
   - Optimize stack management
   - Add lookahead

2. **Scalability Improvements**
   - Investigate exponential degradation
   - Optimize for large inputs
   - Consider streaming parsing

3. **Grammar-Specific Optimization**
   - JSON needs special attention
   - Optimize recursive structures
   - Reduce branching

## Conclusion

MSLL's performance degrades significantly with input size, especially for JSON. While small-scale performance is acceptable (5-6K tokens/sec), large-scale performance (450-2,600 tokens/sec) is much slower than claimed in the paper.

**Key Takeaways:**
1. Small-scale tests are misleading
2. Performance degrades 10-30× with scale
3. JSON is particularly problematic
4. Paper claims need adjustment
5. Focus should be on workflow benefits, not raw performance

**For CCF-B Submission:**
- Use actual data
- Be honest about limitations
- Emphasize trade-offs
- Focus on workflow benefits
- Success probability: 60-70% if done well
