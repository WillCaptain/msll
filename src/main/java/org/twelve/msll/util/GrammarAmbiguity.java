package org.twelve.msll.util;

import org.twelve.msll.lexer.Token;
import org.twelve.msll.parser.MsllStack;
import org.twelve.msll.parsetree.Flag;
import org.twelve.msll.parsetree.ParseNode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GrammarAmbiguity {
    private static AtomicInteger counter = new AtomicInteger();
    private final int id;
    private final ParseNode checkNode;
    private final Map<Token, Flag> checkEnds = new HashMap();
    private final Flag flag;
    private boolean isReady = false;

    public GrammarAmbiguity(MsllStack stack) {
        this.id = counter.incrementAndGet();
        this.checkNode = stack.getLast();
        this.flag = stack.flag();
    }

    public void makeItDone(Token token, MsllStack stack){
        if(!this.isReady) return;
        Flag first = checkEnds.get(token);
        if(first==null){
            checkEnds.put(token,stack.flag());
        }else{
            if(first.expired()){
                stack.expire();
            }else {
                stack.setAmbiguous(this.flag);
                first.setAmbiguous(this.flag);
            }
        }
    }

    public void getReady(){
        this.isReady = true;
    }

    public ParseNode checkNode() {
        return this.checkNode;
    }

    public int id(){
        return this.id;
    }
    public Flag flag(){
        return this.flag;
    }
}
