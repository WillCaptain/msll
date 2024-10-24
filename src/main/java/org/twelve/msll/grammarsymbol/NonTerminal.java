package org.twelve.msll.grammarsymbol;

import org.twelve.msll.parser.Symbol;
import org.twelve.msll.parsetree.NonTerminalNode;

public class NonTerminal extends SymbolType {

    private final boolean isStart;
    private boolean fixed = false;

    public NonTerminal(String name) {
        this(name,false);
    }

    @Override
    public NonTerminalNode parse(Symbol symbol) {
        return new NonTerminalNode(symbol);
    }

    public NonTerminal(String name, boolean isStart) {
        super(name);
        this.isStart = isStart;
    }

    public boolean isStart() {
        return this.isStart;
    }

    public void fix(){
        this.fixed = true;
    }
    public boolean fixed(){
        return this.fixed;
    }
}
