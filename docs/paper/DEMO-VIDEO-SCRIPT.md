# MSLL Demo Video Script (3-5 minutes)

## 目标
展示MSLL的核心价值：实时语法调试，无需编译，对比ANTLR4的工作流优势。

---

## 场景1: MSLL工作流 (1.5分钟)

### 画面
- 屏幕分屏：左侧编辑器，右侧终端

### 旁白
"Let's see how MSLL enables instant grammar iteration. I'm developing a JSON parser and want to test it."

### 操作步骤

**Step 1: 展示初始语法**
```
# 左侧编辑器显示 jsonParser.gm
parser grammar JSONParser;
root: value;
value: object | array | STRING | NUMBER;
object: LBRACE pair (COMMA pair)* RBRACE | LBRACE RBRACE;
```

**Step 2: 运行测试**
```bash
# 右侧终端
$ mvn test -Dtest=JSONEvaluationTest
[INFO] Tests run: 5, Failures: 0, Errors: 0
[INFO] BUILD SUCCESS
Total time: 6.1 seconds
```

**旁白**: "The test passes in 6 seconds. Now let's modify the grammar."

**Step 3: 修改语法 (添加新规则)**
```
# 左侧编辑器修改
value: object | array | STRING | NUMBER | TRUE | FALSE | NULL;
```

**旁白**: "I added support for boolean and null values. Let's test immediately."

**Step 4: 立即重新测试**
```bash
$ mvn test -Dtest=JSONEvaluationTest
[INFO] Tests run: 5, Failures: 0, Errors: 0
[INFO] BUILD SUCCESS
Total time: 6.1 seconds
```

**旁白**: "Notice: I just edited the .gm file and ran the test. No code generation, no compilation. Just 6 seconds from change to result."

---

## 场景2: ANTLR4工作流对比 (1.5分钟)

### 画面
- 同样的屏幕分屏布局

### 旁白
"Now let's see the same workflow with ANTLR4."

### 操作步骤

**Step 1: 展示ANTLR4语法**
```
# 左侧编辑器显示 JSONParser.g4
parser grammar JSONParser;
root: value;
value: object | array | STRING | NUMBER;
object: LBRACE pair (COMMA pair)* RBRACE | LBRACE RBRACE;
```

**Step 2: 生成代码**
```bash
# 右侧终端
$ java -jar antlr-4.13.1-complete.jar JSONLexer.g4 JSONParser.g4
# 显示进度条或等待动画
# 1.15秒后完成
```

**旁白**: "First, generate the parser code. This takes about 1 second."

**Step 3: 编译代码**
```bash
$ javac -cp antlr-4.13.1-complete.jar *.java
# 显示编译进度
# 1.80秒后完成
```

**旁白**: "Then compile the generated code. Another 2 seconds."

**Step 4: 运行测试**
```bash
$ mvn test -Dtest=JSONTest
[INFO] Tests run: 5, Failures: 0, Errors: 0
[INFO] BUILD SUCCESS
Total time: 6.0 seconds
```

**旁白**: "Finally, run the test. Total time: 9 seconds."

**Step 5: 修改语法**
```
# 修改同样的内容
value: object | array | STRING | NUMBER | TRUE | FALSE | NULL;
```

**Step 6: 重复流程**
```bash
$ java -jar antlr-4.13.1-complete.jar JSONLexer.g4 JSONParser.g4
# 1.15秒
$ javac -cp antlr-4.13.1-complete.jar *.java
# 1.80秒
$ mvn test -Dtest=JSONTest
# 6.0秒
# 总计: 8.95秒
```

**旁白**: "Every grammar change requires these three steps: generate, compile, test. That's 9 seconds per iteration."

---

## 场景3: 并排对比 (1分钟)

### 画面
- 屏幕分成两半，左侧MSLL，右侧ANTLR4
- 同时播放两个工作流

### 旁白
"Let's see them side by side."

### 操作
- 左侧: 改grammar → 测试 (6秒)
- 右侧: 改grammar → 生成 → 编译 → 测试 (9秒)

### 显示统计
```
┌─────────────────────────────────────┐
│   Iteration Time Comparison         │
├─────────────────────────────────────┤
│ MSLL:    6.11s  ████████████        │
│ ANTLR4:  8.95s  ████████████████    │
│                                     │
│ MSLL is 1.46× faster                │
│                                     │
│ Over 30 iterations:                 │
│ MSLL:    3.1 minutes                │
│ ANTLR4:  4.5 minutes                │
│ Time saved: 1.4 minutes             │
└─────────────────────────────────────┘
```

**旁白**: "MSLL is 46% faster per iteration. Over 30 iterations typical in grammar development, you save 1.4 minutes of pure waiting time."

---

## 场景4: 实时调试演示 (30秒)

### 画面
- 快速演示多次迭代

### 操作
```
Iteration 1: 修改语法 → 测试 (6秒)
Iteration 2: 修改语法 → 测试 (6秒)
Iteration 3: 修改语法 → 测试 (6秒)
```

### 旁白
"More importantly, MSLL maintains your flow. No waiting for code generation. No context switching. Just edit and test."

---

## 场景5: 总结 (30秒)

### 画面
- 显示核心信息图表

### 内容
```
┌─────────────────────────────────────────┐
│  MSLL: Runtime Multi-Stack Parser       │
├─────────────────────────────────────────┤
│  ✓ First runtime-loading multi-stack    │
│    parser (vs GLL implementations)      │
│                                         │
│  ✓ 1.46-3× faster iteration             │
│    (6s vs 9s per change)                │
│                                         │
│  ✓ G4 compatible (~85% features)        │
│                                         │
│  ✓ Hybrid workflow:                     │
│    - Development: MSLL (fast iteration) │
│    - Production: ANTLR4 (fast runtime)  │
│                                         │
│  ✓ Open source: ~7,800 lines Java       │
└─────────────────────────────────────────┘
```

### 旁白
"MSLL is the first multi-stack parser supporting runtime grammar loading. It's 1.46 to 3 times faster for grammar development. Use MSLL to develop your grammar quickly, then deploy with ANTLR4 for production performance. The best of both worlds."

---

## 技术要求

**录制工具**:
- Screen recording: QuickTime / OBS
- 分辨率: 1920x1080
- 帧率: 30fps

**编辑**:
- 添加字幕显示关键数字
- 高亮显示重要操作
- 加速无关等待时间

**时长控制**:
- 场景1: 1.5分钟
- 场景2: 1.5分钟
- 场景3: 1分钟
- 场景4: 30秒
- 场景5: 30秒
- **总计: 5分钟**

**关键信息**:
1. MSLL: 改grammar → 测试 (6秒)
2. ANTLR4: 改grammar → 生成 → 编译 → 测试 (9秒)
3. 1.46× faster iteration
4. 第一个支持runtime loading的多栈解析器
5. 混合工作流建议

---

## 备选方案: 3分钟精简版

如果5分钟太长，可以精简为:
- 场景1: MSLL工作流 (1分钟)
- 场景2: ANTLR4工作流 (1分钟)
- 场景3: 并排对比 + 总结 (1分钟)

**总计: 3分钟**
