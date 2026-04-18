// Minimal .properties-style grammar, mode-driven.
//
// Entries are KEY SEP VALUE pairs where VALUE runs to end-of-line. KEY and
// VALUE would otherwise share the same character class ([A-Za-z0-9_.-]) so
// a greedy lexer cannot tell "localhost" on the left of '=' from
// "localhost" on the right. The ANTLR4-idiomatic fix is a lexer mode: the
// SEP token flips the lexer into a "value" mode where only VALUE / NL are
// active, then NL pops back to DEFAULT_MODE. This file is the L3 regression
// for MSLL's lexer-mode support.
grammar properties;

file    : line+ EOF ;
line    : entry NL | comment NL | NL ;
entry   : KEY SEP VALUE ;
comment : COMMENT ;

KEY     : [A-Za-z_] [A-Za-z_0-9.\-]* ;
SEP     : [=:] -> pushMode(VAL) ;
COMMENT : [#!] ~[\r\n]* ;
NL      : '\n' ;
WS      : [ \t]+ -> skip ;

mode VAL;
VALUE   : ~[\r\n]+ -> popMode ;
NL_VAL  : '\n' -> type(NL), popMode ;
WS_VAL  : [ \t]+ -> skip ;
