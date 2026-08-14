grammar ZQL;

@header {
package org.zstack.zql.antlr4;
}

zqls
    : zql (';' zql)* EOF
    ;

zql
    : query #queryGrammar
    | count #countGrammar
    | sum #sumGrammar
    | search #searchGrammar
    ;

entity
    : ID
    ;

field
    : ID
    | ID ('.' ID)+
    ;

multiFields
    : ID (',' ID)+
    ;

operator
    : '='
    | '!='
    | '>'
    | '>='
    | '<'
    | '<='
    | 'is null'
    | 'is not null'
    | 'in'
    | 'not in'
    | 'like'
    | 'not like'
    | 'has'
    | 'not has'
    ;

value
    : STRING
    | INT
    | FLOAT
    | BOOLEAN
    | '(' value (',' value)* ')'
    ;

listValue
    : 'list(' value (',' value)* ')'
    ;

logicalOperator
    : AND
    | OR
    ;

complexValue
    : value #simpleValue
    | '(' subQuery ')' #subQueryValue
    | ('(')? getQuery '(' input',' output (',' apiparams)* ')' (')')? #apiGetValue
    | ('(')? getQuery '(' output',' input (',' apiparams)* ')' (')')? #apiGetValue
    ;

getQuery: GET;

apiparams
    : namedAsKey equal value
    | namedAsKey equal listValue
    ;

input
    : INPUT equal namedAsValue
    ;

output
    : OUTPUT equal namedAsValue
    ;

expr
    : field operator complexValue?
    ;

exprAtom
    : ID    #columnNameExprAtom
    | '(' exprAtom (',' exprAtom)* ')'  #nestedExprAtom
    | left=exprAtom mathOperator right=exprAtom #mathExprAtom
    ;

equal
    : '='
    ;

condition
    : '(' condition ')' #parenthesisCondition
    | left=condition (op=logicalOperator right=condition)+ #nestCondition
    | expr #simpleCondition
    ;

queryTarget
    : entity #onlyEntity
    | entity '.' field #withSingleField
    | entity '.' multiFields #withMultiFields
    ;

function
    : DISTINCT
    ;

queryTargetWithFunction
    : queryTarget #withoutFunction
    | function '(' queryTargetWithFunction ')' #withFunction
    ;

orderByExpr
    : exprAtom ORDER_BY_VALUE
    ;

orderBy
    : ORDER_BY orderByExpr (',' orderByExpr)*
    ;

limit
    : LIMIT INT
    ;

offset
    : OFFSET INT
    ;

restrictByExpr
    : entity '.' ID operator value?
    | ID operator value?
    ;

restrictBy
    : RESTRICT_BY '(' restrictByExpr (',' restrictByExpr)* ')'
    ;

returnWithExprBlock
    : '{' (~'}' | returnWithExprBlock)* '}'
    ;

returnWithExpr
    : ID ('.' ID)* #returnWithExprId
    | ID returnWithExprBlock #returnWithExprFunction
    ;

returnWith
    : RETURN_WITH '(' returnWithExpr (',' returnWithExpr)* ')'
    ;

groupByExpr
    : ID (',' ID)*
    ;

groupBy
    : GROUP_BY groupByExpr
    ;

subQueryTarget
    : entity ('.' ID)+
    ;

subQuery
    : QUERY subQueryTarget (WHERE condition+)?
    ;

filterByExprBlock
    : '{' (~'}' | filterByExprBlock)* '}'
    ;

filterByExpr
    : ID filterByExprBlock
    ;

filterBy
    : FILTER_BY filterByExpr (',' filterByExpr)*
    ;

namedAsKey
    : ID
    ;

namedAsValue
    : STRING
    ;

namedAs
    : NAMED_AS namedAsValue
    ;

query
    : QUERY queryTargetWithFunction (WHERE condition+)? restrictBy? returnWith? groupBy? orderBy? limit? offset? filterBy? namedAs?
    ;

count
    : COUNT queryTargetWithFunction (WHERE condition+)? restrictBy? groupBy? orderBy? limit? offset? namedAs?
    ;

sumByValue
    : ID
    ;

sumBy
    : 'by' sumByValue
    ;

sum
    : SUM queryTarget sumBy (WHERE condition+)? orderBy? limit? offset? namedAs?
    ;

search
    : SEARCH keyword (FROM index)? restrictBy? limit? offset?
    ;

keyword
    : STRING
    ;

index
    : ID #singleIndex
    | ID (',' ID)+ #multiIndexs
    ;

mathOperator
    : '*' | '/' | '%' | '+' | '-' | '--'
    ;


FILTER_BY: 'filter by';

OFFSET: 'offset';

LIMIT: 'limit';

QUERY: 'query';

GET: 'getapi';

COUNT: 'count';

SUM: 'sum';

SEARCH: 'search';

DISTINCT: 'distinct';

ORDER_BY: 'order by';

GROUP_BY: 'group by';

NAMED_AS: 'named as';

ORDER_BY_VALUE: ASC | DESC;

RESTRICT_BY: 'restrict by';

RETURN_WITH: 'return with';

WHERE: 'where';

FROM: 'from';

AND: 'and';

OR: 'or';

ASC: 'asc';

DESC: 'desc';

INPUT: 'api';

OUTPUT: 'output';

BOOLEAN
    : 'true'
    | 'false'
    ;

INT : '-'? NUMBER;

FLOAT
    : INT '.' NUMBER+
    ;

ID
    : [a-zA-Z0-9_]+
    ;

WS
    : [ \t\r\n]+ -> skip ;

STRING
    : '"' (~'"')* '"'
    | '\'' ('\'\'' |~'\'')* '\''
    ;

fragment CHAR
    : 'a'..'z'+
    | 'A'..'Z'+
    ;

fragment NUMBER
    : '0'..'9'+
    ;