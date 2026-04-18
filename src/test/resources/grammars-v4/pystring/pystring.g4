// Minimal Python-style triple-quoted-string module.
//
// Exercises PR-2: CodeCache's generalised multi-line delimiter scanner.
// A module is a sequence of triple-quoted string literals, each of which
// may span arbitrarily many physical lines.
grammar pystring;

module : (TSTR)+ EOF ;

TSTR : '"""' .*? '"""' ;
WS   : [ \t\r\n]+ -> skip ;
