// Minimal INI-style grammar.
//
// Section headers, key=value entries, and ';'/'#' comments. Newlines are
// significant (handled by PR-1's newline synthesis).
grammar ini;

file    : (row NL)+ EOF ;
row     : section | entry | comment | ;
section : LBRACK KEY RBRACK ;
entry   : KEY EQ VALUE ;
comment : COMMENT ;

LBRACK  : '[' ;
RBRACK  : ']' ;
EQ      : '=' ;
KEY     : [A-Za-z_] [A-Za-z_0-9.\-]* ;
VALUE   : ~[\r\n]+ ;
COMMENT : [;#] ~[\r\n]* ;
NL      : '\n' ;
WS      : [ \t]+ -> skip ;
