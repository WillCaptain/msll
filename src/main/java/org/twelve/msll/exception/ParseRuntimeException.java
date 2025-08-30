package org.twelve.msll.exception;

public class ParseRuntimeException extends RuntimeException{
    private final ParseErrCode errCode;
    private final String stackTrace;

    public ParseRuntimeException(ParseErrCode errCode){
        this(errCode,errCode.name());
    }
    ParseRuntimeException(ParseErrCode errCode, String stackTrace){
        this.errCode = errCode;
        this.stackTrace = stackTrace;
    }

    public ParseErrCode errCode() {
        return this.errCode;
    }
}
