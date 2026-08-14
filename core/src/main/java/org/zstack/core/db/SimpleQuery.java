package org.zstack.core.db;

import javax.persistence.Tuple;
import javax.persistence.metamodel.SingularAttribute;
import java.util.Collection;
import java.util.List;


public interface SimpleQuery<T> {
    SimpleQuery<T> select(SingularAttribute...attrs);

    SimpleQuery<T> add(SingularAttribute attr, Op op, Collection vals);

    SimpleQuery<T> add(SingularAttribute attr, Op op, Object...val);
    
    SimpleQuery<T> isSoftDeleted(SingularAttribute attr);
    
    SimpleQuery<T> orderBy(SingularAttribute attr, Od order);
    
    SimpleQuery<T> groupBy(SingularAttribute attr);
    
    SimpleQuery<T> setLimit(int limit);
    
    SimpleQuery<T> setStart(int start);
    
    T find();
    
    <T> List<T> list();
    
    <K> K findValue();
    
    <K> List<K> listValue();
    
    Tuple findTuple();
    
    List<Tuple> listTuple();
    
    Long count();

    boolean isExists();
    
    enum Op {
        EQ("="),
        NOT_EQ("!="),
        NOT_NULL("is not null"),
        NULL("is null"),
        IN("in"),
        NOT_IN("not in"),
        GT(">"),
        LT("<"),
        GTE(">="),
        LTE("<="),
        LIKE("like"),
        NOT_LIKE("not like");

        private String name;

        Op(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    
    enum Od {
       DESC,
       ASC,
    }
}
