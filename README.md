### Overview
The Multi-Stack LL(*) (MSLL) parser is a lightweight runtime parsing engine designed for rapid grammar prototyping, interactive debugging, and domain-specific language (DSL) development. Unlike traditional LL(*) parsers (e.g., ANTLR), MSLL dynamically resolves ambiguities through stack duplication and pruning, eliminating the need for static DFA generation or recompilation.

This repository contains:
- The MSLL parsing engine (Java-based).
- Example grammars and test cases.
- Tools for FIRST/FOLLOW set computation.
- A reference implementation for a custom language.

Meanwhile, the Multi-Stack LL(*) (MSLL) parser is not a competitor to ANTLR, but rather a complementary runtime enhancement for ANTLR's development workflow. MSLL enables:
- Instant grammar prototyping without ANTLR's code generation step
- Real-time ambiguity debugging during grammar design
- Seamless migration to ANTLR for production use

````mermaid
graph LR
    A[Grammar Design] --> B[MSLL Runtime Parser]
    B --> C{Grammar Stable?}
    C -->|No| B
    C -->|Yes| D[Generate ANTLR Parser]
````
### Key Features
1. Dynamic Stack Management
- Stack Duplication: When ambiguity is detected, MSLL clones the current stack for each possible production.
- Pruning: Invalid stacks are discarded during parsing, ensuring only valid paths proceed.

2. Parse Tree Tagging
- Each parse tree node is tagged with its originating stack, enabling clear debugging of ambiguous paths.

3. Grammar Compatibility
- Supports standard .g4-style grammar definitions (similar to ANTLR).
- No need for left-factoring or left-recursion elimination.

### Comparison with ANTLR/LL(*) Parsers
|Feature |	MSLL |	ANTLR (LL(*)) |
|--------|-------|----------------|
|Runtime Parsing |	Yes (no codegen)	| No (requires DFA generation)|
|Grammar Changes |	Instant feedback	| Recompilation needed |
|Ambiguity Handling |	Multi-stack exploration |	Precomputed DFA paths|
|Performance |	Slower in high ambiguity |	Optimized for production |

### When to Use MSLL vs. ANTLR
|Scenario |	MSLL |	ANTLR|
|---------|------|-------|
|Grammar prototyping |	✅ Ideal |	❌ Recompiles|
|Production deployment |	⚠️ Temporary |	✅ Optimized|
|Ambiguity exploration	| ✅ Visual	| ❌ Opaque|

### Repository Structure
````
msll/  
├── src/  
│   ├── parser/  
│   │   ├── MsllParser.java       # Core MSLL parsing engine  
│   │   ├── MyParser.java         # Example parser for a custom language  
│   │   └── ...  
│   ├── grammar/  
│   │   ├── GrammarBuilder.java   # FIRST/FOLLOW set computation  
│   │   └── ...  
│   └── lexer/
|   └──...  
├── tests/  
│   ├── CfgGrammarTest.java       # parse g4 grammar 
│   ├── MyGrammarTest.java    # parse your grammar  
│   └── ...  
├── README.md                     # This file  
└── ...  
````
### Getting Started
#### Prerequisites
- Java 8+ (OpenJDK or Oracle JDK)
- Maven (for dependency management, if applicable)

#### Building & Running
- Clone the repository:
````bash
git clone https://github.com/WillCaptain/msll.git
cd msll
```` 
- Example Usage:
````java
MyParserBuilder builder = new MyParserBuilder("testParser.gm", "testLexer.gm");  
MyParser parser = builder.createParser("let var_a = 3 + 6;\n");  
ParserTree tree = parser.parse();  
````
### Future Work
- Optimizations: Bounded stack pruning, parallel stack evaluation.
- IDE Integration: Live grammar debugging in VS Code/IntelliJ.
- Formal Verification: Proofs for termination and correctness.

### License
MIT License - Free for academic and commercial use. See [LICENSE](https://opensource.org/license/mit) for details.
