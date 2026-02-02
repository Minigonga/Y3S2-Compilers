grammar Javamm;

@header {
    package pt.up.fe.comp2025;
}

CLASS : 'class' ;
INT : 'int' ;
FLOAT: 'float' ;
BOOLEAN: 'boolean';
STRING: 'String';
PUBLIC : 'public' ;
RETURN : 'return' ;
IMPORT: 'import' ;
IF: 'if' ;
ELSE: 'else' ;
WHILE: 'while' ;
FOR: 'for' ;
NEW: 'new' ;

INTEGER : [0] | ([1-9][0-9]*);
ID : [a-zA-Z_$]([a-zA-Z_0-9$])* ;

WS : [ \t\n\r\f]+ -> skip ;

LINE_COMMENT : '//' ~[\r\n]* -> skip;
BLOCK_COMMENT : '/*' .*? '*/' -> skip;

program
    : (importDecl)* classDecl EOF //feito
    ;

importDecl
    : IMPORT name+=ID ('.' name+=ID)* ';' //feito
    ;

classDecl  //feito
    : (PUBLIC)? CLASS name= ID ('extends' superClass = ID)?
        '{'
        (varDecl)*
        (methodDecl)*
        '}'
    ;

varDecl //feito
    : type name= ID ';'
    ;

type
    : name= INT '[' ']'  #Array
    | name= STRING '[' ']'    #StringArray
    | name= INT '...'    #Varargs
    | name= INT     #Int
    | name= ID      #Id
    | name= FLOAT   #Float
    | name= BOOLEAN #Boolean
    | name= STRING  #String
    ;

methodDecl locals[boolean isPublic=false]  //feito
    : (PUBLIC {$isPublic=true;})? (isStatic='static')?
        type name=ID
        '(' (param (',' param)*)? ')'
        '{' (varDecl | stmt)* returnStmt? '}'
    ;
param //feito
    : type name=ID
    ;

returnStmt //feito
    : RETURN expr ';'
    ;

stmt
    : expr ';' #ExprStmt //feito
    | '{' (stmt)* '}' #Brackets
    | ifExpr (elseifExpr)* (elseExpr) #IfStmt //feito
    | WHILE '(' (expr) ')' stmt #WhileStmt //feito
    | FOR '(' stmt expr ';' expr ')' stmt #ForStmt //n deve ser preciso
    | name=ID '[' expr ']' '=' expr ';' #ArrayStmt //feito
    | expr '=' expr ';' #AssignStmt //feito
    ;

ifExpr
    : IF '(' expr ')' stmt //feito
    ;

elseifExpr
    : ELSE IF '(' expr ')' stmt //feito
    ;

elseExpr
    : ELSE stmt
    ;

expr
    : '(' expr ')' #ParenthesisExpr //feito
    | value = '!' expr #Negation //feito
    | expr '[' expr ']' #ArrayAccessExpr //feito
    | expr op= ('*' | '/') expr #BinaryExpr //feito
    | expr op= ('+'| '-') expr #BinaryExpr //feito
    | expr op=('<' | '>') expr #BinaryExpr //feito
    | expr op= ('&&' | '||') expr #BinaryExpr //feito
    | expr '.length' #Length //feito
    | value=INTEGER #IntegerLiteral //feito
    | name=ID #VarRefExpr //feito
    | method=ID '(' (expr (',' expr)*)? ')' #ClassMethodCallExpr
    | expr '.' method=ID '(' (expr (',' expr)*)? ')' #MethodCallExpr //feito pode ter problemas ja corrigi alguns
    | value= 'true' #BooleanExpr //feito
    | value= 'false' #BooleanExpr //feito
    | value= 'this' #ObjectCallExpr //feito (n sei se tem problemas)
    | NEW INT '[' expr ']' #ArrayDeclaration //feito acho que direito
    | NEW name=ID '(' (expr (',' expr) *)? ')' #NewClass //feito
    | '[' (expr ',')* expr ']' #ArrayInitExpr //feito
    | expr op=('++' | '--') #IncrementExpr // não é preciso fazer
    ;

