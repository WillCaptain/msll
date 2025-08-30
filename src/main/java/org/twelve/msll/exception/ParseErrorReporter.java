package org.twelve.msll.exception;

import org.twelve.msll.parsetree.ParseNode;
import org.twelve.msll.parsetree.ParserTree;


public class ParseErrorReporter {

    public static void report(ParserTree tree, ParseNode node, ParseErrCode errCode, String message) {
//        if(!node.inferred()) return;
        ParseError err = tree.addError(new ParseError(node, errCode, message));
        if(err!=null){
            System.out.println(err);
        }
    }

}
