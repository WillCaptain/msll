package org.twelve.msll.parsetree;

import org.twelve.msll.grammarsymbol.SymbolType;
import org.twelve.msll.lexer.Location;
import org.twelve.msll.parser.Symbol;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ParseNode<T extends SymbolType> {
    private static AtomicLong counter = new AtomicLong();
    protected ParserTree parserTree;
    private Flag flag;
    protected Symbol<T> symbol;
    protected NonTerminalNode parent;
    private final long id;

    private Map<String,Object> tag = new HashMap<>();

    public ParseNode(Symbol<T> symbol){
        this.symbol = symbol;
        this.id = counter.getAndIncrement();
    }

    public void setFlag(Flag flag) {
        this.flag = flag;
    }

    public Flag flag(){
        return this.flag;
    }

    public String name(){
        return this.symbol.name();
    }

    protected void setParent(NonTerminalNode parent) {
        this.parent = parent;
    }

    public Symbol<T> symbol(){
        return this.symbol;
    }

    public abstract Location location();

    public NonTerminalNode parent(){
        return this.parent;
    }

    public void setParseTree(ParserTree parserTree) {
        this.parserTree = parserTree;
    }

    public ParserTree parseTree() {
        return this.parserTree;
    }

    @Override
    public String toString() {
        return this.symbol.toString();
    }
    public long id() {
        return this.id;
    }

    public void setTag(String key, Object value) {
        this.tag.put(key,value);
    }
    public Object getTag(String key){
        return this.tag.get(key);
    }

    public abstract String lexeme();

}
