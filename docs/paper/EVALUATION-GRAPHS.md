# MSLL Evaluation Graphs

## Performance Degradation at Scale

### JavaScript Performance

| Input Size | Tokens | Throughput (tok/s) |
|-----------|--------|-------------------|
| 5K statements | 25,000 | 24,510 |
| 10K statements | 50,000 | 11,962 |
| 50K statements | 250,000 | 2,336 |
| 100K statements | 500,000 | 1,163 |

**Degradation pattern**: Near-quadratic (21× slower at 20× input size)

### JSON Performance

| Input Size | Tokens | Throughput (tok/s) |
|-----------|--------|-------------------|
| 100 objects | 1,401 | 6,254 |
| 1K objects | 16,000 | 1,939 |
| 5K objects | 80,000 | 406 |
| 10K objects | 160,000 | 194 |

**Degradation pattern**: Approximately linear (10× slower at 10× input size)

### Python Performance

| Input Size | Tokens | Throughput (tok/s) |
|-----------|--------|-------------------|
| 1K statements | ~4,000 | 5,618 |

## Workflow Comparison

### Iteration Time per Change

| Workflow | Time (seconds) | Steps |
|----------|---------------|-------|
| MSLL | 6.11 | 1: Run test |
| ANTLR4 | 8.95 | 3: Generate (1.15s) + Compile (1.80s) + Test (6.0s) |

**Speedup**: MSLL is 1.46× faster (46% improvement)

### Cumulative Time for 30 Iterations

| Workflow | Total Time | Calculation |
|----------|-----------|-------------|
| MSLL | 3.1 minutes | 6.11 × 30 = 183s |
| ANTLR4 | 4.5 minutes | 8.95 × 30 = 269s |

**Time saved**: 86 seconds (1.4 minutes)

### Ideal Scenario (No Maven Overhead)

| Workflow | Time per Iteration |
|----------|-------------------|
| MSLL | <1 second |
| ANTLR4 | ~3 seconds |

**Ideal speedup**: 3×

## Visualization Notes

To generate graphs from this data, use matplotlib or similar visualization tools:

```python
import matplotlib.pyplot as plt

# Performance degradation
js_tokens = [25000, 50000, 250000, 500000]
js_throughput = [24510, 11962, 2336, 1163]

plt.plot(js_tokens, js_throughput, 'o-', label='JavaScript')
plt.xlabel('Input Size (tokens)')
plt.ylabel('Throughput (tokens/sec)')
plt.title('MSLL Performance Degradation')
plt.xscale('log')
plt.yscale('log')
plt.legend()
plt.grid(True)
plt.savefig('performance-degradation.png')
```

```python
# Workflow comparison
workflows = ['MSLL', 'ANTLR4']
times = [6.11, 8.95]

plt.bar(workflows, times, color=['green', 'red'])
plt.ylabel('Iteration Time (seconds)')
plt.title('Development Workflow Comparison')
plt.savefig('workflow-comparison.png')
```
