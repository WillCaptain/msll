package org.twelve.msll.parser;

import org.twelve.msll.exception.AggregateGrammarSyntaxException;
import org.twelve.msll.exception.GrammarSyntaxException;
import org.twelve.msll.grammar.Grammar;
import org.twelve.msll.grammar.Grammars;
import org.twelve.msll.grammar.Production;
import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.lexer.Lexer;
import org.twelve.msll.lexer.RegexLexer;
import org.twelve.msll.lexer.Token;
import org.twelve.msll.lexer.TokenBuffer;
import org.twelve.msll.parsetree.*;
import org.twelve.msll.util.GrammarAmbiguity;
import org.twelve.msll.util.Constants;
import org.twelve.msll.util.Tool;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.twelve.msll.util.Tool.cast;

/**
 * Multi-Stack LL(*) (MSLL) Parser
 * Designed for parsing complex grammars in LL(*) format without fixed K limitation.
 * <p>
 * Overview:
 * The MSLL parser is built on LL(1) principles but introduces dynamic stack management to handle complex
 * grammars that go beyond the capability of traditional LL(1) or LL(K) parsers.
 * <p>
 * Workflow:
 * 1. When the parser encounters multiple productions with the same FIRST token, it duplicates the current
 * parsing stack into multiple stacks.
 * 2. Each duplicated stack will be assigned a different production rule to pursue.
 * 3. As each stack processes the input independently, further duplications may occur if new ambiguities arise
 * (e.g., multiple productions sharing the same FIRST token).
 * 4. If a stack fails to match the input, it is discarded.
 * 5. The process continues until either:
 * - Exactly one stack reaches the end of the input (parse success).
 * - No stack successfully matches the input (parse failure).
 * - More than one stack remains valid, indicating a FIRST-FIRST conflict (parse failure).
 * <p>
 * Result:
 * The MSLL parser dynamically manages multiple parsing paths to resolve complex grammatical ambiguities without
 * requiring precomputed K values, making it more flexible for grammar design and debugging.
 * <p>
 * The way we treat the G4 file as a language in itself, described by a CFG grammar,
 * adds an interesting meta-layer to the parsing process.
 * In essence, we are parsing a grammar (G4) to generate another grammar (CFG),
 * which is then used by the MSLL parser to parse the actual custom language.
 * <p>
 * This recursive approach exemplifies how grammars can be used as "languages for languages,"
 * allowing you to manipulate and understand the G4 files themselves as structured input, just like any programming language.
 *
 * @author huizi 2024
 * @since 1.0
 */
public abstract class MsllParser<P extends ParserTree> {
    /**
     * cfg format grammars: grammar -> [production]
     */
    private final Grammars grammars;

    /**
     * Predict table for token matching.
     * Allows FIRST-FIRST conflicts, meaning a single token can match multiple productions within the same grammar.
     * This enables the parser to explore multiple parsing paths for complex grammar structures.
     */
    private final PredictTable predictTable;

    /**
     * Lexer for tokenizing the input string.
     * Implemented using regular expressions (regex), offering simpler and cleaner code compared to the DFA approach.
     * While this may result in slightly lower performance than a DFA-based lexer, it provides a more straightforward implementation.
     */
    private final Lexer lexer;

    /**
     * A list of active parsing stacks used by the MSLL parser to manage multiple parsing paths.
     * Each stack represents an independent parse attempt, exploring a unique production in cases of ambiguity.
     * As the parser progresses, stacks are dynamically added, duplicated, or eliminated based on token matches.
     * The parsing continues until only one stack remains, representing the successful parse, or until all stacks are eliminated.
     */
    private final MsllStacks stacks;

    /**
     * Represents the raw parsing result before the Abstract Syntax Tree (AST) is constructed.
     * The parse tree contains all the detailed parsing information, including every token and rule matched during the parsing process.
     * This structure is a direct reflection of the grammar and serves as an intermediate step before the AST is built, where unnecessary details are removed for further semantic analysis.
     */
    private final P parseTree;

    /**
     * Stores the non-terminal symbols defined in the grammar.
     * Non-terminals represent abstract syntactic constructs or grammar rules, such as expressions, statements, or blocks.
     * These symbols help define the structure of the language, representing patterns that are composed of other non-terminals or terminals.
     * Non-terminals guide the parser in recognizing high-level language structures.
     */
    protected final NonTerminals nonTerminals;

    /**
     * Stores the terminal symbols defined in the grammar.
     * Terminals represent the actual tokens or lexical elements produced by the lexer, such as keywords, operators, or punctuation.
     * These are the concrete symbols that appear in the input stream, and the parser matches them against the grammar's rules.
     * Terminals are the lowest-level constructs in the grammar and are crucial for tokenizing the input.
     */
    protected final Terminals terminals;
    private PARSE_STATUS status = PARSE_STATUS.NOT_STARTED;

    /**
     * Constructs a parser instance using the provided cfg formatted grammar and parsing components.
     * <p>
     * All arguments are injected through a parser builder, ensuring that the parser is initialized
     * with the correct grammar rules, prediction table, and token definitions.
     *
     * @param grammars     The complete set of grammar rules, defining the language syntax. will be constructed out of parser
     * @param predictTable The prediction table used for token matching and resolving ambiguous parses.
     * @param nonTerminals The set of non-terminal symbols, representing abstract grammar rules.
     * @param terminals    The set of terminal symbols, representing the concrete tokens in the input.
     * @param reader       The input source to be parsed, typically providing the token stream.
     */
    /**
     * Root NonTerminalNode of the parse tree – kept as a field so that the
     * panic-mode error-recovery logic can attach recovered statement subtrees
     * without going through the (possibly abstract) parse-tree API.
     */
    private final NonTerminalNode startNode;

    public MsllParser(Grammars grammars, PredictTable predictTable, NonTerminals nonTerminals, Terminals terminals, Reader reader) {
        this.grammars = grammars;
        this.lexer = new RegexLexer(reader, terminals);
        this.nonTerminals = nonTerminals;
        this.terminals = terminals;
        MsllStack stack = MsllStack.apply();
        stack.push(new EndNode(terminals));

        NonTerminalNode start = new NonTerminalNode(new Symbol<>(grammars.getStart().nonTerminal()));
        stack.push(start);
        this.startNode = start;
        this.parseTree = createParseTree(start);
        this.predictTable = predictTable;
        stacks = new MsllStacks();

        stacks.add(stack);
    }

    public Integer maxStackSize() {
        return this.stacks.maxStackSize();
    }

    public Integer totalStackSize() {
        return this.stacks.totalStackSize();
    }

    /**
     * Creates and initializes the parse tree starting from the given non-terminal node.
     * <p>
     * This method is abstract, allowing for multiple implementations of the parse tree structure.
     * Each implementation can define how the parse tree is constructed and managed based on the
     * specific requirements of the grammar and parsing strategy.
     *
     * @param start The root node (NonTerminalNode) from which the parse tree will be built.
     * @return An instance of the parse tree (P), customized per the implementation.
     */
    protected abstract P createParseTree(NonTerminalNode start);

    /**
     * Core method for parsing a single token in the MSLL parser.
     * <p>
     * This method parses tokens without using a lookahead strategy, consuming tokens one by one immediately without going back.
     * a cursor is used to traverse the token buffer, no lookahead, nor look backward waste
     * <p>
     * This function is triggered for each token and is also invoked recursively when descending into nested or leveled grammar rules.
     * The parsing process involves multiple stacks, represented by the `stackList`, which handles different potential parsing paths.
     * These stacks are dynamically managed to explore various grammar rules, and are updated throughout the recursive parsing process.
     *
     * @param tokens    The buffer of tokens to be parsed.
     * @param cursor    The current position in the token buffer used to look ahead during parsing.
     * @param stackList A list of active MSLL stacks involved in this parsing call, tracking different parse paths.
     * @param lineIndex An atomic counter used to keep track of the current line index in the source input.
     */
    private void parseToken(TokenBuffer tokens, int cursor, List<MsllStack> stackList, AtomicInteger lineIndex) {
        Token token = tokens.get(cursor);
        if (token == null) {//error in lexing
            throw new RuntimeException("something wrong in lexing...");
        }
        if(!token.channel().isEmpty()){
//            lineIndex.set(cursor);
            return;
        }

        List<MsllStack> all = new ArrayList<>();
        all.addAll(stackList);
        for (MsllStack stack : all) {
            //check predicate
            checkPredicate(tokens, token, stack);
            if (token.terminal() == terminals.EOL) {
                lineIndex.set(cursor);
                continue;
            }
            if (stack.size() == 0) {
                this.stacks.remove(stack);
                break;
            }
            ParseNode node = cursor > 0 ? stack.pop(tokens.get(cursor - 1)) : stack.pop();
            List<MsllStack> more;
            try {
                if (node instanceof NonTerminalNode) {
                    more = matchNonTerminalToken(stack, (NonTerminalNode) node, token, tokens.getLine(token.location().line().number()));
                    parseToken(tokens, cursor, more, lineIndex);
                } else {
                    matchTerminalToken(stack, cast(node), token, tokens.getLine(token.location().line().number()));
                    if (token.terminal() == terminals.END && stack.size() == 0) {
                        if(this.status == PARSE_STATUS.DONE){
                            this.status = PARSE_STATUS.AMBIGUOUS;
                        }
                        if(this.status == PARSE_STATUS.RUNNING){
                           this.status = PARSE_STATUS.DONE;
                           lineIndex.set(cursor);
                       }
                    }
                }
            } catch (GrammarSyntaxException e) {
                if (!this.stacks.isEmpty()) {
                    this.stacks.remove(stack);
                    stack.expire();
                }
                if (this.stacks.size() == 0) {
                    throw new GrammarSyntaxException(e.getMessage());
                }
            }finally {
                if(this.status==PARSE_STATUS.AMBIGUOUS){
                    throw new GrammarSyntaxException("the parsing is ambiguous");
                }
            }
        }
    }

    private void checkPredicate(TokenBuffer tokens, Token token, MsllStack stack) {
        if (stack.size() > 0) {
            ParseNode lookHead = stack.peek();
            if (lookHead.symbol().type().name().contains(Constants.PREDICATE_ABLE)) {
                new GrammarPredicate(lookHead.symbol().name().replaceAll("\\{|\\}", "")).test(token, tokens);
                stack.pop();
                lookHead.parent().removeNode(lookHead);
            }
        }
    }

    /**
     * Handles the parsing of a non-terminal symbol at the top of the stack.
     * <p>
     * This method matches the non-terminal against the grammar rules defined in the predict table using the input token.
     * The process involves the following steps:
     * <p>
     * 1. If the symbol on top of the stack is a non-terminal, find the corresponding grammar rule for the non-terminal
     * using the predict table. The FIRST set of the non-terminal is used to determine potential matching productions.
     * <p>
     * 2. If no matching productions are found, a grammar error is thrown, originating from the `predictTable.match()` method.
     * <p>
     * 3. If one or more productions match the input token, create a new node corresponding to the non-terminal.
     * <p>
     * 4. If multiple productions match, duplicate the current parsing stack for each production, allowing the parser
     * to explore multiple paths in parallel.
     * <p>
     * 5. Pop the non-terminal from the stack and push the symbols from the matched production onto the stack, preparing
     * for the next token match.
     *
     * @param stack The current parsing stack being processed.
     * @param node  The non-terminal node at the top of the stack.
     * @param token The token from the input stream currently being matched.
     * @param line  The current line in the input source, used for error tracking and reporting.
     * @return A list of new or updated parsing stacks resulting from the matching process.
     */
    private List<MsllStack> matchNonTerminalToken(MsllStack stack, final NonTerminalNode node, Token token, String line) {
        Grammar grammar = grammars.get(node.name());
        List<Production> productions = this.predictTable.match(token, grammar, line).stream().filter(p -> p != null && !p.isEmpty()).collect(Collectors.toList());
        List<MsllStack> all = new ArrayList<>();
        all.add(stack);
        if (productions.size() == 0) {
            return all;
        }
        GrammarAmbiguity grammarAmbiguity = null;
        if (productions.size() > 1) {
            grammarAmbiguity = new GrammarAmbiguity(stack);
            for (int j = 0; j < productions.size(); j++) {
                MsllStack matched = MsllStack.apply(stack, grammarAmbiguity, grammar.name() + ":" + productions.get(j).toString());
                this.stacks.add(matched);
                all.add(matched);
            }
            this.stacks.remove(stack);
            all.remove(stack);
            stack.free();
//            parseTree.addK(productions.size());
        }
        for (int j = 0; j < productions.size(); j++) {
            Production production = productions.get(j);
            node.setExplain(production.explain());
            MsllStack matched = all.get(j);
            List<Symbol> symbols = production.symbols();
            // 反向遍历该non terminal node type命中production的所有symbol
            // 这些symbol创建SyntaxNode加入到该non terminal node下面
            // 并且推入栈，替代node的位置，开启下一层次的匹配
            for (int i = symbols.size() - 1; i >= 0; i--) {
                Symbol symbol = symbols.get(i);
                if (symbol.type() == terminals.EPSILON) {
                    continue;
                }
                ParseNode newNode = symbol.type().parse(symbol);
                node.addNode(newNode, 0);
                matched.push(newNode);
            }
        }
        if (grammarAmbiguity != null) {
            grammarAmbiguity.getReady();
        }
        return all;
    }

    /**
     * Handles the parsing of a terminal symbol at the top of the stack.
     * <p>
     * This method verifies that the terminal symbol at the top of the stack exactly matches the token at the top of the input.
     * For example, if the terminal symbol on the stack is "IF", then the corresponding token from the input should represent
     * the keyword "if".
     * <p>
     * If the token matches the terminal, the parser proceeds. If the token does not match, a grammar error is raised.
     *
     * @param stack The current parsing stack being processed.
     * @param node  The terminal node at the top of the stack, representing the expected token.
     * @param token The token from the input stream currently being matched against the terminal.
     * @param line  The current line in the input source, used for error reporting and tracking.
     */
    private void matchTerminalToken(MsllStack stack, TerminalNode node, Token token, String line) {
        if (token.terminal() == node.terminal()) {
            // 如果terminal匹配正确，将实际token放入该terminal node。一般对变量型node有价值，比如ID
            node.setToken(token);
        } else {
            String keywordHint = (!token.terminal().isRegex() && node.terminal().name().equals("ID"))
                    ? System.lineSeparator() + "Hint: '" + token.lexeme() + "' is a reserved keyword and cannot be used as an identifier."
                    : "";
            Tool.grammarError(stack,
                    "unexpected token: " + token.lexeme() + ", expected token in " + (node.parent() == null ? node.name() : node.parent().name()) + " is "
                            + node.terminal().name() + ", at line:" + token.location().line().number() + ", position: " + token.location().lineStart() + " - "
                            + token.location().lineEnd() + System.lineSeparator() + line + keywordHint);
        }
    }

    /**
     * Polishes the parse tree to remove unnecessary or redundant nodes, such as empty nodes or expired nodes discarded
     * during the MSLL parsing process.
     * <p>
     * The raw parse tree may contain duplicated or overly complex structures due to recursive productions in the grammar.
     * For example, productions like 'NAMESPACE -> ID IDS' and 'IDS -> ε | .NAMESPACE' can create a layered structure
     * instead of a simple ID.ID.ID format. This method cleans up such redundancies and simplifies the structure.
     * <p>
     * Non-terminal nodes are refined, transforming the parse tree into a more concise and correct format while retaining
     * all the essential grammar information.
     *
     * @return A polished parse tree that retains the core structure but removes redundant and useless information, making
     * the tree easier to work with and interpret.
     */
    protected P done() {
        this.stacks.removeIf(s -> s.size() == 0);
        if (!this.stacks.isEmpty()) {
            Tool.grammarError("the parser is not finished with correct input");
        }
        parseTree.polish();
        // Reset the static MsllStack pool after every completed parse to prevent
        // unbounded accumulation across successive parse() calls (O(n) scan fix).
        MsllStack.reset();
        return parseTree;
    }

    /**
     * Initiates the parsing process by scanning the input source using the lexer asynchronously, while parsing tokens in parallel.
     * <p>
     * This method leverages asynchronous tokenization and parallel parsing to efficiently process the input, ensuring that
     * complex grammar structures can be handled quickly and concurrently. The lexer asynchronously generates tokens from the
     * source input, while the parser processes these tokens in parallel, updating the parse tree dynamically as the tokens are
     * matched with the grammar rules.
     * <p>
     * The parsing process continues until the entire input is consumed, or until the parser detects a critical grammar error.
     * Once parsing is complete, the method returns a fully constructed and polished parse tree.
     *
     * @return A fully parsed and polished parse tree, representing the complete structure of the input source.
     */
    public P parse() {
        this.status = PARSE_STATUS.RUNNING;
        TokenBuffer tokens = lexer().scan();
        List<GrammarSyntaxException> collectedErrors = new ArrayList<>();
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            AtomicInteger lineIndex = new AtomicInteger(-1);
            AtomicInteger cursor = new AtomicInteger(0);
            while (cursor.get() == 0 || cursor.get() < tokens.size() || !tokens.get(tokens.size() - 1).terminal().name().equals(Constants.END_STR)) {
                try {
                    this.parseToken(tokens, cursor.getAndIncrement(), this.stacks, lineIndex);
                } catch (GrammarSyntaxException e) {
                    // Panic-mode recovery: collect this error and try to resume
                    // at the next statement boundary if the subclass supports it.
                    String recoverySymbol = syntaxErrorRecoverySymbol();
                    if (recoverySymbol != null && this.stacks.isEmpty()) {
                        collectedErrors.add(e);
                        // Advance past the next ';' (statement terminator)
                        int pos = cursor.get();
                        while (pos < tokens.size()) {
                            Token t = tokens.get(pos++);
                            if (t != null && t.terminal() != null
                                    && !t.terminal().isRegex()
                                    && t.terminal().pattern().equals(";")) {
                                break;
                            }
                        }
                        cursor.set(pos);
                        // Rebuild stacks so we can parse the next statement
                        if (!rebuildStacksForRecovery(recoverySymbol)) {
                            throw e; // can't recover – propagate original error
                        }
                        this.status = PARSE_STATUS.RUNNING;
                    } else {
                        throw e;
                    }
                }
            }
            // After the whole token stream, re-raise collected errors as an aggregate
            if (!collectedErrors.isEmpty()) {
                throw new AggregateGrammarSyntaxException(collectedErrors);
            }
        });
        try {
            future.get();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof AggregateGrammarSyntaxException agg) {
                throw agg;
            }
            if (cause instanceof GrammarSyntaxException gse) {
                throw gse;
            }
            e.printStackTrace();
            throw new GrammarSyntaxException("parsing error: " + e.getMessage());
        }
        return this.done();
    }

    /**
     * Override in subclasses to enable panic-mode error recovery.
     * Return the name of the grammar non-terminal that represents a
     * top-level recoverable unit (e.g. {@code "statement"}).
     * Returning {@code null} (the default) disables recovery.
     */
    protected String syntaxErrorRecoverySymbol() {
        return null;
    }

    /**
     * After a parse error, clears the stacks and pushes a fresh parse attempt
     * for the given non-terminal, attaching it to the root parse-tree node.
     * Returns {@code true} if recovery was set up successfully.
     */
    private boolean rebuildStacksForRecovery(String symbolName) {
        Grammar grammar = grammars.get(symbolName);
        if (grammar == null) return false;
        // Clear all failed stacks
        this.stacks.removeIf(s -> true);
        // Create a fresh recovery node and wire it into the existing tree root
        NonTerminalNode recoveryNode = new NonTerminalNode(new Symbol<>(grammar.nonTerminal()));
        startNode.addNode(recoveryNode);
        // Push onto a brand-new stack
        MsllStack freshStack = MsllStack.apply();
        freshStack.push(new EndNode(terminals));
        freshStack.push(recoveryNode);
        this.stacks.add(freshStack);
        return true;
    }

    /**
     * Provides access to the current lexer instance.
     * <p>
     * This method can be overridden in the future to support different lexer implementations, offering flexibility in how
     * the input source is tokenized. The current implementation returns the default regex lexer used by the parser.
     *
     * @return The current lexer instance responsible for tokenizing the input.
     */
    public Lexer lexer() {
        return this.lexer;
    }
}
