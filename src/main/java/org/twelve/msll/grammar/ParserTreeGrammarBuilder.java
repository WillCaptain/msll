package org.twelve.msll.grammar;

import org.twelve.msll.grammarsymbol.*;
import org.twelve.msll.parser.Symbol;
import org.twelve.msll.parsetree.*;
import org.twelve.msll.util.Constants;
import org.twelve.msll.util.RegexString;
import org.twelve.msll.util.Tool;

import java.util.ArrayList;
import java.util.List;
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
     * Constructs terminal symbols from the lexer parse tree.
     * <p>
     * This method processes the lexer tree to extract terminal symbols and create corresponding
     * terminal entries in the grammar. For each terminal, it checks for an optional lexer command and
     * assigns it to the terminal.
     */
    private void buildTerminals() {
        buildGrammarSymbols(this.lexerTree, (nodes, head, productions, i) -> {
            TerminalNode lexerCommand = nodes.get(3).symbol().name().equals(Constants.LEXER_COMMAND) ? cast(nodes.get(3)) : null;
            createTerminal(head.nodes().get(0).toString(), cast(productions.nodes().get(0)), lexerCommand);
        });
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
     * Creates a terminal symbol and adds it to the terminals set.
     * <p>
     * This method creates a terminal from the given name and production value. It supports different types
     * of terminal symbols, such as regex-based terminals. An optional lexer command can also be associated
     * with the terminal.
     *
     * @param name         The name of the terminal symbol.
     * @param value        The value or pattern representing the terminal.
     * @param lexerCommand The optional lexer command associated with the terminal.
     */
    private void createTerminal(String name, NonTerminalNode value, TerminalNode lexerCommand) {
        Terminal terminal;
        if (name.contains("'")) return;
        switch (value.nodes().get(0).symbol().name()) {
            case Constants.REGEX:
                terminal = new Terminal(name, new RegexString(value.toString()));//.token().lexeme()
                break;
            default:
                terminal = new Terminal(name, value.lexeme());
        }
        terminal.setCommand(lexerCommand == null ? null : lexerCommand.toString());
        this.terminals.addSymbol(terminal);
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
