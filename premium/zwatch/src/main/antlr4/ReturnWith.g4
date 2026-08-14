grammar ReturnWith;

@header {
   package org.zstack.zwatch.returnwith.antlr4;
}

expression
    : expr (',' expr)*
    ;

expr
    : ID '=' value
    ;

value
    : STRING #valueStr
    | INT #valueNum
    | func #valueFunc
    ;

func
    : ID '(' (~')')* ')'
    ;

ID
    : (CHAR | '_')+
    ;

INT : '-'? NUMBER;

FLOAT
    : INT '.' NUMBER+
    ;

WS
    : [ \t\r\n]+ -> skip ;

STRING
    : '"' (~'"')* '"'
    | '\'' (~'\'')* '\''
    ;

VALUE
    : CHAR+
    ;


fragment CHAR
    : 'a'..'z'+
    | 'A'..'Z'+
    ;

fragment NUMBER
    : '0'..'9'+
    ;