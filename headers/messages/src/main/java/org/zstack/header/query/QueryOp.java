package org.zstack.header.query;

import org.zstack.header.configuration.PythonClass;

import java.util.HashMap;
import java.util.Map;

@PythonClass
public class QueryOp {
    private static Map<String, QueryOp> allOps = new HashMap<String, QueryOp>();

    private String op;

    private QueryOp(String op) {
        this.op = op;
        allOps.put(op, this);
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof QueryOp) {
            QueryOp qop = (QueryOp) obj;
            return qop.op.equals(this.op);
        } else if (obj instanceof String) {
            return obj.equals(this.op);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return op.hashCode();
    }

    @Override
    public String toString() {
        return op;
    }

    public static QueryOp valueOf(String op) {
        QueryOp qop = allOps.get(op);
        if (qop == null) {
            throw new IllegalArgumentException(String.format("unknown QueryOp type[%s]", op));
        }
        return qop;
    }

    public static QueryOp EQ = new QueryOp("=");
    public static QueryOp NOT_EQ = new QueryOp("!=");
    public static QueryOp GT = new QueryOp(">");
    public static QueryOp GT_AND_EQ = new QueryOp(">=");
    public static QueryOp LT = new QueryOp("<");
    public static QueryOp LT_AND_EQ = new QueryOp("<=");
    public static QueryOp IN = new QueryOp("in");
    public static QueryOp NOT_IN = new QueryOp("not in");
    public static QueryOp IS_NULL = new QueryOp("is null");
    public static QueryOp NOT_NULL = new QueryOp("is not null");
    public static QueryOp LIKE = new QueryOp("like");
    public static QueryOp NOT_LIKE = new QueryOp("not like");
}
