package org.zstack.core.db;

import org.zstack.core.db.SimpleQuery.Op;

import javax.persistence.metamodel.SingularAttribute;
import java.util.Collection;

/**
 * Created by xing5 on 2016/6/29.
 */
public interface UpdateQuery {
    UpdateQuery set(SingularAttribute attr, Object val);

    UpdateQuery condAnd(SingularAttribute attr, Op op, Object val);

    UpdateQuery eq(SingularAttribute attr, Object val);

    UpdateQuery notEq(SingularAttribute attr, Object val);

    UpdateQuery in(SingularAttribute attr, Collection val);

    UpdateQuery notIn(SingularAttribute attr, Collection val);

    UpdateQuery isNull(SingularAttribute attr);

    UpdateQuery notNull(SingularAttribute attr);

    UpdateQuery gt(SingularAttribute attr, Object val);

    UpdateQuery gte(SingularAttribute attr, Object val);

    UpdateQuery lt(SingularAttribute attr, Object val);

    UpdateQuery lte(SingularAttribute attr, Object val);

    UpdateQuery like(SingularAttribute attr, Object val);

    UpdateQuery notLike(SingularAttribute attr, Object val);

    void delete();

    int hardDelete();

    int update();

    static UpdateQuery New(Class entityClass) {
        return new UpdateQueryImpl().entity(entityClass);
    }
}
