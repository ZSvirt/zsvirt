grammar TriggerExpression;

@header {
   package org.zstack.monitoring.trigger.expression.antlr4;
}

trigger
    : expression
    ;

expression
    : item ('{' argumentList '}')? operator constant
    ;

item
    : ID
    ;

arguements
    : ID '=' INT  #argInt
    | ID '=' STRING #argString
    ;

argumentList
    : arguements
    | arguements ( ',' arguements )+
    |
    ;

constant
    : FLOAT
    | UNIT
    | INT
    ;

operator
    : '-'
    | '*'
    | '/'
    | '+'
    | '-'
    | '<'
    | '<='
    | '>'
    | '>='
    | '='
    | '!='
    ;

ID
    : (CHAR | '_' | '-' | ':' | '.')+
    ;

INT : '-'? NUMBER;

UNIT
    : NUMBER ('s'|'m'|'h'|'d'|'w'|'K'|'M'|'G'|'T'|'P'|'E'|'Z'|'Y')
    ;

FLOAT
    : INT '.' NUMBER+
    ;

WS
    : [ \t\r\n]+ -> skip ;

STRING
    : '"' (~'"')+ '"' | '\'' + (~'\'') + '\''
    ;


fragment CHAR
    : 'a'..'z'+
    | 'A'..'Z'+
    ;

fragment NUMBER
    : '0'..'9'+
    ;
