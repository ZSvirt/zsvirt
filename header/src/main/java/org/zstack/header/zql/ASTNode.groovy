package org.zstack.header.zql

class ASTNode {
    private static final Map<Class, List<String>> childrenNames = [:]

    List<ASTNode> getChildren() {
        List<String> childNames
        synchronized (childrenNames) {
            childNames = childrenNames.get(getClass(), metaClass.getProperties().findAll { ASTNode.isAssignableFrom(it.type) }.collect { it.name })
        }

        return childNames.collect { owner[it] as ASTNode }
    }

    Object accept(ASTVisitor visitor) {
        return visitor.visit(this)
    }

    interface Value {
    }

    interface Condition {
    }

    interface ReturnWithExpr {
    }

    interface Function {
    }

    static class Distinct extends ASTNode implements Function {
    }

    static class QueryTarget extends ASTNode {
         String entity
         List<String> fields
    }

    static class QueryTargetWithFunction extends QueryTarget {
        Function function
        QueryTargetWithFunction subTarget

        static QueryTargetWithFunction valueOf(QueryTarget q) {
            return new QueryTargetWithFunction(entity: q.entity, fields: q.fields)
        }

        @Override
        String getEntity() {
            if (subTarget != null) {
                return subTarget.getEntity()
            } else {
                return super.getEntity()
            }
        }

        @Override
        List<String> getFields() {
            if (subTarget != null) {
                return subTarget.getFields()
            } else {
                return super.getFields()
            }
        }
    }

    static class ComplexValue extends ASTNode implements Value {
         SubQuery subQuery
    }

    static class PlainValue extends ASTNode implements Value {
        String text
        transient Class type
        String ctype
    }

    static class ListValue extends ASTNode implements Value {
         List<Value> values
    }

    static class Expr extends ASTNode implements Condition {
         String operator
         List<String> left
         Value right
    }

    static class LogicalOperator extends ASTNode implements Condition {
         String operator
         Condition left
         Condition right
    }

    static class ExprAtom extends ASTNode {
        String text
        List<String> fields = new ArrayList<>()
    }

    static class OrderByExpr extends ASTNode {
        ExprAtom target
        String direction
    }

    static class OrderBy extends ASTNode {
        List<OrderByExpr> exprs
    }

    static class Limit extends ASTNode {
         int limit
    }

    static class Offset extends ASTNode {
         int offset
    }

    static class RestrictExpr extends ASTNode {
         String entity
         String field
         String operator
         Value value
    }

    static class RestrictBy extends ASTNode {
        List<RestrictExpr> exprs
    }

    static class ReturnWithIDExpr extends ASTNode implements ReturnWithExpr {
        List<String> names
    }

    static class ReturnWithBlockExpr extends ASTNode implements ReturnWithExpr {
        String name
        String content
    }

    static class ReturnWith extends ASTNode {
         List<ReturnWithExpr> exprs
    }

    static class Sum extends Query {
        String groupByField
    }

    static class GroupByExpr extends ASTNode {
        List<String> fields
    }

    static class Query extends ASTNode {
        QueryTargetWithFunction target
        List<Condition> conditions
        RestrictBy restrictBy
        ReturnWith returnWith
        FilterBy filterBy
        OrderBy orderBy
        Limit limit
        Offset offset
        String name
        GroupByExpr groupBy

        void addRestrictExpr(RestrictExpr expr) {
            if (restrictBy == null)  {
                restrictBy = new RestrictBy()
            }

            if (restrictBy.exprs == null) {
                restrictBy.exprs = []
            }

            restrictBy.exprs.add(expr)
        }
    }

    static class SubQueryTarget extends ASTNode {
         String entity
         List<String> fields
    }

    static class SubQuery extends ASTNode {
         SubQueryTarget target
         List<Condition> conditions
    }

    static class FilterByExpr extends ASTNode {
         String filterName
         String content
    }

    static class FilterBy extends ASTNode {
         List<FilterByExpr> exprs
    }

    static class Search extends ASTNode {
        Keyword keyword
        Index index
        RestrictBy restrictBy
        Limit limit
        Offset offset
    }

    static class Keyword extends ASTNode {
        String value
    }

    static class Index extends ASTNode {
        List<String> indexs
    }
}
