package org.zstack.core.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.header.message.APIListMessage;

import javax.persistence.Entity;
import javax.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.util.List;

public class DbEntityListerImpl implements DbEntityLister {
    @Autowired
    private DatabaseFacade dbf;

    private String getEntityName(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException(String.format("%s is not a JPA entity, @Entity is not presented on class declaration", clazz.getName()));
        }
        return clazz.getSimpleName();
    }

    @Override
    public <T> List<T> listByApiMessage(APIListMessage msg, Class<T> clazz) {
        return listByUuids(msg.getUuids(), msg.getOffset(), msg.getLength(), clazz);
    }
    
    @Override
    public <T> List<T> listAll(Class<T> clazz) {
        return listAll(0, Integer.MAX_VALUE, clazz);
    }

    @Override
    public <T> List<T> listAll(int offset, int length, Class<T> clazz) {
        return listByUuids(null, offset, length, clazz);
    }

    @Override
    public <T> List<T> listByUuids(List<String> uuids, Class<T> clazz) {
        return listByUuids(uuids, 0, Integer.MAX_VALUE, clazz);
    }

    private void checkIfHasUuid(Class<?> c) {
        Class<?> clazz = c;
        do {
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                if (f.getName().equals("uuid")) {
                    return;
                }
            }

            clazz = clazz.getSuperclass();
        } while (clazz != null && clazz != Object.class);

        throw new IllegalArgumentException(String.format("Entity %s doesn't have field 'uuid'", clazz.getName()));
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public <T> List<T> listByUuids(List<String> uuids, int offset, int length, Class<T> clazz) {
        String ename = getEntityName(clazz);
        String sql = null;
        TypedQuery<T> query = null;
        if (uuids == null || uuids.isEmpty()) {
            sql = String.format("select e from %s e", ename);
            query = dbf.getEntityManager().createQuery(sql, clazz);
        } else {
            checkIfHasUuid(clazz);
            sql = String.format("select e from %s e where e.uuid in :uuids", ename);
            query = dbf.getEntityManager().createQuery(sql, clazz);
            query.setParameter("uuids", uuids);
        }
        query.setFirstResult(offset);
        query.setMaxResults(length);
        return query.getResultList();
    }
}
