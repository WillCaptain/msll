package org.twelve.msll.grammar;

import org.twelve.msll.grammarsymbol.*;
import org.twelve.msll.parser.Symbol;
import org.twelve.msll.parsetree.*;
import org.twelve.msll.util.Constants;
import org.twelve.msll.util.RegexString;
import org.twelve.msll.util.Tool;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.twelve.msll.util.Tool.cast;

/**
 * ParserTreeGrammarBuilder is responsible for building CFG grammar structures based on parse trees
 * generated from G4 grammar file(ParserGrammarParser) and lexer files(LexerRuleParser).
 * It uses the parse tree to construct non-terminals and
 * terminals for the grammar, which can then be used in the MSLL parser.
 * <p>
 * The parse trees are derived from the G4 grammar and lexer trees and transformed into CFG grammar rules.
 * <p>
 * huizi 2024
 */
public class ParserTreeGrammarBuilder extends GrammarBuilder {
    private final G4GrammarTree parserTree;
    private final G4GrammarTree lexerTree;

    /**
     * Constructor for initializing the ParserTreeGrammarBuilder with the parser and lexer parse trees,
     * along with the sets of non-terminals and terminals.
     * <p>
     * This class processes the parse trees to extract grammar and lexer rules, creating the corresponding
     * terminal and non-terminal symbols for the CFG.
     *
     * @param parserTree   The parse tree representing the grammar rules.
     * @param lexerTree    The parse tree representing the lexer rules.
     * @param nonTerminals The set of non-terminal symbols for the grammar.
     * @param terminals    The set of terminal symbols for the grammar.
     */
    public ParserTreeGrammarBuilder(G4GrammarTree parserTree, G4GrammarTree lexerTree, NonTerminals nonTerminals, Terminals terminals) {
        super(nonTerminals, terminals);
        this.parserTree = parserTree;
        this.lexerTree = lexerTree;
    }

    /**
     * Initializes the grammar by building terminals from the lexer parse tree and non-terminals from
     * the parser parse tree. This method is called to set up the grammar before it can be used.
     */
    @Override
    public void initialize() {
        buildTerminals();
        buildNonTerminals();
    }

    /**
     * Constructs terminal symbols from the lexer parse tree using the new lex_body-based structure.
     *
     * <p>Two passes are performed:
     * <ol>
     *   <li>Fragment rules – compiled to Java regex strings and stored in a local map for inlining.</li>
     *   <li>Regular rules  – compiled to Java regex (with fragment references resolved) and registered
     *       as {@link Terminal} instances.</li>
     * </ol>
     */
    private void buildTerminals() {
        LexerRuleTree lexerRuleTree = (LexerRuleTree) this.lexerTree;
        // Ordered map so earlier fragments can be referenced by later ones
        Map<String, String> fragmentRegexes = new LinkedHashMap<>();

        // Pass 1: compile fragment rules
        for (NonTerminalNode fragGrammar : lexerRuleTree.allFragments()) {
            NonTerminalNode terminalNode = findChild(fragGrammar, Constants.TERMINAL);
            NonTerminalNode lexBody     = findChild(fragGrammar, Constants.LEX_BODY);
            if (terminalNode == null || lexBody == null) continue;

            String fragName  = terminalNode.nodes().get(0).toString();
            String fragRegex = LexerRuleCompiler.compile(lexBody, fragmentRegexes);
            fragmentRegexes.put(fragName, fragRegex);
        }

        // Pass 2a: compile regular lexer rules into the SAME inline map so that
        // cross-rule references (e.g. `INTEGER : DIGIT+ ;` referencing `DIGIT`)
        // expand to the referenced rule's regex rather than to Pattern.quote("DIGIT").
        // This mirrors ANTLR4 semantics: every lexer rule is inlineable when
        // referenced from another lexer rule, whether it's declared `fragment`
        // or not. Forward references are resolved by iterating until the map
        // stabilises: on each pass every rule is re-compiled against the current
        // map, so a reference to a rule compiled later in pass N-1 picks up its
        // real regex in pass N. Bounded by the rule count to guard against
        // pathological inputs.
        List<AbstractMap.SimpleEntry<String, NonTerminalNode>> regularRules
                = lexerRuleTree.allGrammarsWithModes();
        int maxPasses = regularRules.size() + 1;
        for (int pass = 0; pass < maxPasses; pass++) {
            boolean changed = false;
            for (AbstractMap.SimpleEntry<String, NonTerminalNode> entry : regularRules) {
                NonTerminalNode grammar = entry.getValue();
                NonTerminalNode terminalNode = findChild(grammar, Constants.TERMINAL);
                NonTerminalNode lexBody     = findChild(grammar, Constants.LEX_BODY);
                if (terminalNode == null || lexBody == null) continue;
                String name = terminalNode.nodes().get(0).toString();
                if (name.contains("'")) continue;
                String regex = LexerRuleCompiler.compile(lexBody, fragmentRegexes);
                String prev = fragmentRegexes.get(name);
                if (!regex.equals(prev)) {
                    fragmentRegexes.put(name, regex);
                    changed = true;
                }
            }
            if (!changed) break;
        }

        // Pass 2b: the map is now stable – emit the actual Terminals.
        for (AbstractMap.SimpleEntry<String, NonTerminalNode> entry : regularRules) {
            String mode = entry.getKey();
            NonTerminalNode grammar = entry.getValue();

            NonTerminalNode terminalNode = findChild(grammar, Constants.TERMINAL);
            NonTerminalNode lexBody      = findChild(grammar, Constants.LEX_BODY);
            if (terminalNode == null || lexBody == null) continue;

            String name = terminalNode.nodes().get(0).toString();
            if (name.contains("'")) continue; // msll internal helper rule convention

            TerminalNode lexerCommandNode = findTerminalChild(grammar, Constants.LEXER_COMMAND);

            String regex = fragmentRegexes.getOrDefault(name,
                    LexerRuleCompiler.compile(lexBody, fragmentRegexes));
            // Pattern.quote("x") produces \Qx\E.  For plain keyword terminals we
            // must store the literal pattern (without \Q/\E) so that
            // Terminals.fromPattern("return") can still find the "Return" terminal.
            Terminal terminal = isLiteralQuote(regex)
                    ? new Terminal(name, regex.substring(2, regex.length() - 2))
                    : new Terminal(name, new RegexString(regex));
            terminal.setCommand(lexerCommandNode == null ? null : lexerCommandNode.toString());
            terminal.setMode(mode);
            this.terminals.addSymbol(terminal);
        }

        // Built-in: EOF is the G4 name for end-of-file (internally END).
        // Register it so parser rules like `program : stmt* EOF ;` resolve correctly.
        Terminal eofAlias = new Terminal("EOF", new RegexString(Constants.END));
        this.terminals.addIfAbsent(eofAlias);
    }

    /**
     * Returns true when the regex was produced by Pattern.quote() for a simple string,
     * i.e. it is exactly \Q<content>\E with no nested \Q or \E inside.
     */
    private static boolean isLiteralQuote(String regex) {
        if (!regex.startsWith("\\Q") || !regex.endsWith("\\E")) return false;
        String inner = regex.substring(2, regex.length() - 2);
        return !inner.contains("\\E");  // nested \E would break the literal assumption
    }

    /** Finds the first NonTerminalNode child with the given type name. */
    private static NonTerminalNode findChild(NonTerminalNode parent, String typeName) {
        for (ParseNode child : parent.nodes()) {
            if (child instanceof NonTerminalNode
                    && child.symbol().type().name().equals(typeName)) {
                return cast(child);
            }
        }
        return null;
    }

    /** Finds the first TerminalNode child with the given type name. */
    private static TerminalNode findTerminalChild(NonTerminalNode parent, String typeName) {
        for (ParseNode child : parent.nodes()) {
            if (child instanceof TerminalNode
                    && child.symbol().type().name().equals(typeName)) {
                return cast(child);
            }
        }
        return null;
    }

    /**
     * Constructs non-terminal symbols from the parser parse tree.
     * <p>
     * This method processes the parser tree to extract non-terminal symbols and create corresponding
     * non-terminal entries in the grammar. It creates the grammar productions associated with each non-terminal.
     */
    private void buildNonTerminals() {
        buildGrammarSymbols(this.parserTree, (nodes, head, productions, i) -> {
            createNonTerminal(head, productions.nodes(), i == 0);
        });
    }

    /**
     * Helper method to build grammar symbols from a given parse tree.
     * <p>
     * This method iterates through the grammar nodes in the parse tree, filtering out comments and processing
     * each grammar rule. It then invokes a provided builder function to create terminal or non-terminal entries.
     *
     * @param tree    The parse tree representing the grammar or lexer rules.
     * @param builder The builder function used to construct terminals or non-terminals.
     */
    private void buildGrammarSymbols(G4GrammarTree tree, SymbolBuilder builder) {
        //build grammar
        List<ParseNode> nodes = tree.grammarRoot().nodes().stream().filter(n -> n.symbol().name().equals(Constants.GRAMMAR)).collect(Collectors.toList());
        for (int i = 0; i < nodes.size(); i++) {
            ParseNode node = nodes.get(i);
            if (node.name().equals(Constants.COMMENT)) {
                continue;
            }
            if (!node.name().equals(Constants.GRAMMAR)) {
                continue;
            }
            NonTerminalNode grammar = cast(node);
            List<ParseNode> pNodes = grammar.nodes().stream().filter(n -> !n.name().equals(Constants.COMMENT)).collect(Collectors.toList());//ignore comments
            NonTerminalNode head = cast(pNodes.get(0));
            NonTerminalNode productions = cast(pNodes.get(2));
            builder.build(pNodes, head, productions, i);
        }
    }


    /**
     * Creates a non-terminal symbol and its associated productions, then adds them to the grammar.
     *
     * This method processes the non-terminal and its productions, creating a Grammar object for the non-terminal
     * and associating it with its production rules. It also handles special cases such as epsilon productions,
     * string patterns, and predicates.
     *
     * @param head        The head non-terminal of the grammar rule.
     * @param productions The list of production nodes associated with the non-terminal.
     * @param isStart     Whether this non-terminal is the start symbol of the grammar.
     */
    private void createNonTerminal(NonTerminalNode head, List<ParseNode> productions, boolean isStart) {
        String headName = head.nodes().get(0).toString();
        NonTerminal nonHead = this.nonTerminals.addNonTerminal(headName, isStart);
        AtomicInteger predicateId = new AtomicInteger();
        if (head.getTag(Constants.FIX) != null && (boolean) head.getTag(Constants.FIX)) {
            nonHead.fix();
        }
        Grammar grammar = this.createGrammar(headName);// new Grammar(non, this.terminals);
        for (ParseNode node : productions) {
            if (node instanceof TerminalNode) continue;
            NonTerminalNode production = cast(node);
            List<Symbol> symbols = new ArrayList();
            String explain = "";
            ASSOC assoc = ASSOC.NONE;
            for (ParseNode n : production.nodes()) {
                switch (n.symbol().name()) {
                    case Constants.NON_TERMINAL:
                        String name = ((NonTerminalNode) n).nodes().get(0).lexeme();
                        symbols.add(new Symbol(this.nonTerminals.addNonTerminal(name), name));
                        break;
                    case Constants.EPSILON_STR:
                        symbols.add(new Symbol(Terminal.EPSILON));
                        break;
                    case Constants.TERMINAL:
                        Terminal t1 = terminals.fromName(n.lexeme());
                        if(t1==null){
                            Tool.lexerError("terminal "+n.lexeme()+" is not found");
                        }else {
                            symbols.add(new Symbol(t1));
                        }
                        break;
                    case Constants.STRING:
                        Terminal t2 = terminals.fromPattern(n.lexeme());
                        if(t2==null){
                            Tool.lexerError("terminal "+n.lexeme()+" is not found");
                        }else {
                            symbols.add(new Symbol(t2));
                        }
                        break;
                    case Constants.COMMENT:
//                        System.out.println("comment: \"" + n.lexeme().substring(2) + "\" is ignored");
                        break;
                    case Constants.EXPLAIN:
                        explain = n.lexeme().substring(1);
                        break;
                    case Constants.ASSOCIATE:
                        assoc = ASSOC.valueOf(((NonTerminalNode) n).nodes().get(3).symbol().name());
                        break;
                    case Constants.PREDICATE:
                        symbols.add(new Symbol(terminals.addIfAbsent(new Terminal(Constants.PREDICATE_ABLE + "_" + predicateId.incrementAndGet(), n.name())), n.lexeme()));
                        break;
                    default:
//                        System.out.println("some symbol not matched!!");
                        break;
                }
            }
            new Production(grammar, symbols, nonTerminals, terminals, explain, assoc);
        }
    }
}

/**
 * Functional interface for building grammar symbols (terminals or non-terminals).
 *
 * This interface is used as a callback for processing grammar symbols during the construction of
 * terminals and non-terminals.
 */
interface SymbolBuilder {
    void build(List<ParseNode> node, NonTerminalNode head, NonTerminalNode productions, int index);
}
