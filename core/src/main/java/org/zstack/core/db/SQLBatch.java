package org.zstack.core.db;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.header.exception.CloudRuntimeException;

import java.lang.reflect.Field;

/**
 * Created by xing5 on 2017/3/4.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class SQLBatch {
    @Autowired
    protected DatabaseFacade databaseFacade;

    protected <K> K persist(K k) {
        databaseFacade.getEntityManager().persist(k);
        return k;
    }

    protected <K> K merge(K k) {
        return databaseFacade.getEntityManager().merge(k);
    }

    protected void remove(Object k) {
        Field f = EntityMetadata.getPrimaryKeyField(k.getClass());
        try {
            f.setAccessible(true);
            sql(String.format("DELETE FROM %s vo WHERE vo.%s = :value", k.getClass().getSimpleName(), f.getName()))
                    .param("value", f.get(k)).execute();
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException(e);
        }
    }

    protected void flush() {
        databaseFacade.getEntityManager().flush();
    }

    protected <K> K findByUuid(String uuid, Class<K> clz) {
        return databaseFacade.getEntityManager().find(clz, uuid);
    }

    protected <K> K reload(K k) {
        flush();
        databaseFacade.getEntityManager().refresh(k);
        return k;
    }

    protected abstract void scripts();

    protected SQL sql(String text) {
        return SQL.New(text);
    }

    protected UpdateQuery sql(Class clz) {
        return SQL.New(clz);
    }

    protected SQL sql(String text, Class clz) {
        return SQL.New(text, clz);
    }

    protected Q q(Class clz) {
        return Q.New(clz);
    }

    protected QueryMore q(Class<?> first, Class<?>... more) {
        return Q.New(first, more);
    }

    @Transactional
    private void _execute() {
        scripts();
    }

    @DeadlockAutoRestart
    public void execute() {
        _execute();
    }
}
