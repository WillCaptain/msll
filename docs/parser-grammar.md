# MSLL Parser Grammar Reference

A parser grammar file defines the **structure** of your language — how
tokens produced by the lexer are assembled into a parse tree.  
MSLL's `.gm` parser syntax is nearly identical to ANTLR4 (G4).

---

## File Structure

```antlr
/* optional block comment */
parser grammar MyParser;

options {
    tokenVocab = MyLexer;    // ← link to the lexer grammar
}

// — parser rules follow —
ruleName : alternative1 | alternative2 ;
```

> **Naming convention** — parser rules begin with a **lower-case** letter.  
> Terminal names (tokens from the lexer) begin with an **UPPER-CASE** letter.

---

## Alternatives (`|`)

A rule with multiple alternatives tries each one in order.

```antlr
statement
    : variableDecl
    | assignment
    | returnStatement
    | expression ';'?
    ;
```

---

## Sequences

Elements separated by whitespace must all match in order.

```antlr
returnStatement : Return expression ';' ;
//                ──────────────────────
//                     1     2      3   (must appear in this order)
```

---

## Inline String Literals

You can reference tokens by their literal value rather than their terminal
name.  MSLL resolves them to the correct terminal automatically.

```antlr
// These two rules are equivalent:
variableDecl : Let ID '=' expression ';' ;
variableDecl : Let ID EQUAL expression SEMI ;
```

---

## Quantifiers

| Syntax | Meaning        | Example |
|--------|----------------|---------|
| `x?`   | zero or one    | `';'?` — optional semicolon |
| `x*`   | zero or more   | `statement*` — zero or more statements |
| `x+`   | one or more    | `ID (',' ID)*` — comma-separated list |

```antlr
program         : statement* EOF ;
statement       : importDecl | exportDecl | declaration ;
paramList       : '(' (param (',' param)*)? ')' ;
param           : ID (':' typeName)? ('=' expression)? ;
```

---

## Grouping

Parentheses group sub-expressions inline, avoiding the need for a helper rule.

```antlr
// without grouping — needs a helper rule
multiplicativeExpr : unary (mulOp unary)* ;
mulOp : '*' | '/' | '%' ;

// with inline grouping — more concise
multiplicativeExpr : unary (('*' | '/' | '%') unary)* ;
```

---

## Terminal References

Terminals are referenced by their UPPER-CASE name from the lexer grammar.

```antlr
literal
    : NUMBER
    | STRING
    | BooleanLiteral
    | NullLiteral
    ;

identifier : ID | Yield | Async ;   // context-sensitive keywords
```

---

## Semantic Predicates

Predicates are Java boolean expressions wrapped in `{…}?` that gate a
rule branch at **runtime**.  They solve the classic "no-line-terminator"
problem in JavaScript-style grammars without grammar duplication.

```antlr
// Only matches if the parser context says "no line terminator here"
returnStatement
    : Return {notLineTerminator()}? expression ';'
    | Return ';'
    ;

// Arrow function: parameter list must not be preceded by a line break
arrowFunction
    : arrowFunctionParameters {notLineTerminator()}? '=>' arrowFunctionBody
    ;
```

The predicate identifier is passed through to the host parser class where
you implement the actual check.

---

## Alternative Labels (`#`)

Label individual alternatives to generate named parse-tree accessor methods
or to identify which branch was chosen.

```antlr
expression
    : expression '+' expression                     # addExpr
    | expression '-' expression                     # subExpr
    | expression '*' expression                     # mulExpr
    | expression '/' expression                     # divExpr
    | '(' expression ')'                            # parenExpr
    | NUMBER                                        # numericLiteral
    | ID                                            # identifierRef
    ;
```

---

## Operator Associativity

Annotate alternatives with `<assoc=right>` to make an operator
right-associative (default is left-associative).

```antlr
expression
    : <assoc=right> expression '**' expression      # exponentExpr
    | expression '*' expression                     # mulExpr
    | expression '+' expression                     # addExpr
    ;
```

---

## Options Block

```antlr
options {
    tokenVocab = OutlineLexer;   // which lexer grammar to use
}
```

---

## Left Recursion — Just Write It Naturally

MSLL's multi-stack engine handles **direct left recursion** without any
grammar transformations.  Write your grammar the way you think, not the
way LL(k) forces you.

```antlr
// Left-recursive rules work out of the box
expression
    : expression '.' ID                             # memberAccess
    | expression '[' expression ']'                 # indexAccess
    | expression '(' argList? ')'                   # functionCall
    | expression '+' expression                     # addition
    | '-' expression                                # unaryMinus
    | NUMBER                                        # number
    | ID                                            # identifier
    ;
```

> Under the hood, MSLL clones the parse stack at every ambiguous choice and
> prunes branches that fail to match — naturally resolving left recursion
> without grammar rewrites.

---

## Full Example — Outline DSL Parser

This is the actual parser grammar for the **Outline** language — a
dynamically-typed DSL with type inference powered by MSLL.

```antlr
parser grammar OutlineParser;
options { tokenVocab = OutlineLexer; }

root : statement+ ;

statement
    : comment
    | variable_declarator
    | assignment
    | return_statement
    | empty_statement
    ;

variable_declarator
    : ('let' | 'var') {notLineTerminator()} assignment
    ;

return_statement : 'return' expression ';' ;
empty_statement  : expression ';'? ;

// ── expressions ──────────────────────────────────────────────
expression        : numeric_expression ;

numeric_expression
    : term_expression (('+' | '-') term_expression)*
    ;

term_expression
    : unary_expression (('*' | '/' | '%' | '^') unary_expression)*
    ;

unary_expression
    : ('++' | '--' | '-') factor_expression
    | factor_expression ('++' | '--')?
    ;

factor_expression
    : literal
    | ID
    | entity
    | array
    | map
    | lambda
    | function
    | factor_expression '[' NUMBER ']'                              # array_accessor
    | factor_expression '.' ID                                      # entity_member_accessor
    | factor_expression '(' (expression (',' expression)*)? ')'    # function_call
    | '(' expression ')'
    ;

// ── literals & constructors ───────────────────────────────────
literal : NUMBER | STRING | This ;

entity  : '{' property_assignment (',' property_assignment)* ','? '}' ;
array   : '[' expression (',' expression)* ']' ;
map     : '[' expression ':' expression (',' expression ':' expression)* ']' ;

property_assignment
    : (('let' | 'var') {notLineTerminator()})? ID ':' expression
    ;

// ── functions & lambdas ───────────────────────────────────────
lambda : lambda_args '->' expression ;
lambda_args : ID | function_args' ;
function_args' : '(' (ID (',' ID)*)? ')' ;
function : 'fx' function_args' block ;

block    : '{' statement+ '}' ;
assignment : ID ('=' | '+=' | '-=' | '*=' | '/=') expression ';' ;
```

---

## Grammar File Skeleton

```antlr
parser grammar MyParser;

options {
    tokenVocab = MyLexer;
}

// Entry point — MSLL requires an explicit root rule
root : topLevelStatement+ EOF ;

topLevelStatement
    : functionDeclaration
    | classDeclaration
    | statement
    ;

// ... your rules here
```

---

## Quick Reference Card

```
rule       : alt1 | alt2 | alt3 ;      // alternatives
rule       : a b c ;                   // sequence
rule       : a? b* c+ ;                // quantifiers
rule       : (a | b | c) ;            // inline group
rule       : TOKEN 'literal' ;         // terminals
rule       : {predicate()}? a | b ;    // semantic predicate
rule       : a # label1 | b # label2 ; // alternative labels
rule       : <assoc=right> a OP a ;    // right-associativity
```
