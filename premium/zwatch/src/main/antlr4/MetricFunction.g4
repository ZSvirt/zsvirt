grammar MetricFunction;

@header {
   package org.zstack.zwatch.api.antlr4;
}

function
    : expression
    ;

expression
    : func '(' argumentList ')'
    ;

func
    : ID
    ;

arguements
    : ID '=' INT  #argInt
    | ID '=' STRING #argString
    | ID '=' FLOAT #argFloat
    | ID '=' VALUE #argChar
    ;

argumentList
    : arguements
    | arguements ( ',' arguements )+
    |
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
    : '"' (~'"')+ '"' | '\'' + (~'\'') + '\''
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
