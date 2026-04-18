// Minimal INI-style grammar, mode-driven.
//
// Like properties, the RHS of '=' must escape the KEY character class to
// match end-of-line text. Use a lexer mode so VALUE only lives after '='.
// Section headers are handled in DEFAULT_MODE where '[' ... ']' is structural.
//
// Blank lines between sections are expressed as a standalone `NL` row
// rather than `row : ... | ;` because MSLL's .gm format has no standalone
// epsilon alternative.
grammar ini;

file    : row+ EOF ;
row     : section NL | entry NL | comment NL | NL ;
section : LBRACK KEY RBRACK ;
entry   : KEY EQ VALUE ;
comment : COMMENT ;

LBRACK  : '[' ;
RBRACK  : ']' ;
EQ      : '=' -> pushMode(VAL) ;
KEY     : [A-Za-z_] [A-Za-z_0-9.\-]* ;
COMMENT : [;#] ~[\r\n]* ;
NL      : '\n' ;
WS      : [ \t]+ -> skip ;

mode VAL;
VALUE   : ~[\r\n]+ -> popMode ;
NL_VAL  : '\n' -> type(NL), popMode ;
WS_VAL  : [ \t]+ -> skip ;
