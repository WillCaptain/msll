package org.twelve.msll.exception;

import org.twelve.msll.parsetree.ParseNode;

public class ParseError {
    private final ParseErrCode errorCode;
    private final ParseNode node;
    private final String message;

    public ParseError(ParseNode node, ParseErrCode errorCode, String message) {
        this.errorCode = errorCode;
        this.node = node;
        this.message = message;
    }

    public ParseNode node() {
        return this.node;
    }

    public ParseErrCode errorCode() {
        return this.errorCode;
    }

    public String message(){
        return this.message;
    }

    @Override
    public String toString() {
        return this.errorCode().toString().toLowerCase()+ (message.isEmpty() ?"":(": " + message));
    }
}
