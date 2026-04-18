// Minimal .properties-style grammar.
//
// Line-oriented: each non-empty, non-comment line is either KEY '=' VALUE
// or KEY ':' VALUE, terminated by an explicit newline token. Exercises the
// newline-synthesis shim added by PR-1.
grammar properties;

file     : (line NL)+ EOF ;
line     : entry | comment | ;
entry    : KEY SEP VALUE ;
comment  : COMMENT ;

KEY     : [A-Za-z_] [A-Za-z_0-9.\-]* ;
SEP     : [=:] ;
VALUE   : ~[\r\n]+ ;
COMMENT : [#!] ~[\r\n]* ;
NL      : '\n' ;
WS      : [ \t]+ -> skip ;
