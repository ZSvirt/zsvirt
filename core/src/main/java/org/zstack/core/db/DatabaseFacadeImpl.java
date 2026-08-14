package org.zstack.core.db;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.TransactionalCallback.Operation;
import org.zstack.header.Component;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIListMessage;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;
import org.zstack.utils.*;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.zstack.utils.CollectionDSL.list;

public class DatabaseFacadeImpl implements DatabaseFacade, Component {
    private static final CLogger logger = CLoggerImpl.getLogger(DatabaseFacadeImpl.class);

    @PersistenceUnit(unitName = "zstack.jpa")
    private EntityManagerFactory entityManagerFactory;
    @PersistenceContext(unitName = "zstack.jpa")
    private EntityManager entityManager;

    @Autowired
    private PluginRegistry pluginRgty;

    private static AtomicLong dberror = new AtomicLong(0);
    private static AtomicLong dbdeadlock = new AtomicLong(0);
    private static AtomicLong dblocktimeout = new AtomicLong(0);

    private DataSource dataSource = null;
    private DataSource extraDataSource = null;
    private List<TransactionalCallback> transactionAsyncCallbacks = null;
    private List<TransactionalSyncCallback> transactionSyncCallbacks = null;
    private Map<Class, List<SoftDeleteEntityExtensionPoint>> softDeleteExtensions = new HashMap<Class, List<SoftDeleteEntityExtensionPoint>>();
    private Map<Class, List<SoftDeleteEntityByEOExtensionPoint>> softDeleteByEOExtensions = new HashMap<Class, List<SoftDeleteEntityByEOExtensionPoint>>();
    private List<SoftDeleteEntityExtensionPoint> softDeleteForAllExtensions = new ArrayList<SoftDeleteEntityExtensionPoint>();
    private Map<Class, List<HardDeleteEntityExtensionPoint>> hardDeleteExtensions = new HashMap<Class, List<HardDeleteEntityExtensionPoint>>();
    private List<HardDeleteEntityExtensionPoint> hardDeleteForAllExtensions = new ArrayList<HardDeleteEntityExtensionPoint>();
    private Map<Class, EntityInfo> entityInfoMap = new HashMap<Class, EntityInfo>();
    private String dbVersion;

    class EntityInfo {
        Field voPrimaryKeyField;
        boolean compositePrimaryKey = false;
        Field eoPrimaryKeyField;
        Field eoSoftDeleteColumn;
        Class eoClass;
        Class voClass;
        Map<EntityEvent, EntityLifeCycleCallback> listeners = new HashMap<EntityEvent, EntityLifeCycleCallback>();

        EntityInfo(Class voClazz) {
            voClass = voClazz;

            if (voClazz.isAnnotationPresent(IdClass.class)) {
                compositePrimaryKey = true;
            }

            voPrimaryKeyField = FieldUtils.getAnnotatedField(Id.class, voClass);
            DebugUtils.Assert(voPrimaryKeyField != null, String.format("%s has no primary key", voClass));
            voPrimaryKeyField.setAccessible(true);

            EO at = (EO) voClazz.getAnnotation(EO.class);
            if (at != null) {
                eoClass = at.EOClazz();
                DebugUtils.Assert(eoClass != null, String.format("cannot find EO entity specified by VO entity[%s]", voClazz.getName()));
                eoPrimaryKeyField = FieldUtils.getAnnotatedField(Id.class, eoClass);
                DebugUtils.Assert(eoPrimaryKeyField != null, String.format("cannot find primary key field(@Id annotated) in EO entity[%s]", eoClass.getName()));
                eoPrimaryKeyField.setAccessible(true);
                eoSoftDeleteColumn = FieldUtils.getField(at.softDeletedColumn(), eoClass);
                DebugUtils.Assert(eoSoftDeleteColumn != null, String.format("cannot find soft delete column[%s] in EO entity[%s]", at.softDeletedColumn(), eoClass.getName()));
                eoSoftDeleteColumn.setAccessible(true);
            }

            buildInheritanceDeletionExtension();
            buildSoftDeletionCascade();
        }

        public boolean hasCompositePrimaryKey() {
            return compositePrimaryKey;
        }

        private void buildSoftDeletionCascade() {
            SoftDeletionCascades ats = (SoftDeletionCascades) voClass.getAnnotation(SoftDeletionCascades.class);
            if (ats == null) {
                return;
            }

            for (final SoftDeletionCascade at : ats.value()) {
                final Class parent = at.parent();
                if (!parent.isAnnotationPresent(Entity.class)) {
                    throw new CloudRuntimeException(String.format("class[%s] has annotation @SoftDeletionCascade but its parent class[%s] is not annotated by @Entity",
                            voClass, parent));
                }

                if (!parent.isAnnotationPresent(EO.class)) {
                    continue;
                }

                List<SoftDeleteEntityExtensionPoint> exts = softDeleteExtensions.get(parent);
                if (exts == null) {
                    exts = new ArrayList<SoftDeleteEntityExtensionPoint>();
                    softDeleteExtensions.put(parent, exts);
                }

                exts.add(new SoftDeleteEntityExtensionPoint() {
                    @Override
                    public List<Class> getEntityClassForSoftDeleteEntityExtension() {
                        return Arrays.asList(parent);
                    }

                    @Override
                    @Transactional
                    public void postSoftDelete(Collection entityIds, Class entityClass) {
                        String sql = String.format("delete from %s me where me.%s in (:ids)", voClass.getSimpleName(), at.joinColumn());
                        Query q = getEntityManager().createQuery(sql);
                        q.setParameter("ids", entityIds);
                        q.executeUpdate();
                    }
                });
            }
        }

        private void buildInheritanceDeletionExtension() {
            PrimaryKeyJoinColumn at = (PrimaryKeyJoinColumn) voClass.getAnnotation(PrimaryKeyJoinColumn.class);
            if (at == null) {
                return;
            }

            final Class parent = voClass.getSuperclass();
            if (!parent.isAnnotationPresent(Entity.class)) {
                throw new CloudRuntimeException(String.format("class[%s] has annotation @PrimaryKeyJoinColumn but its parent class[%s] is not annotated by @Entity",
                        voClass, parent));
            }

            if (!parent.isAnnotationPresent(EO.class)) {
                return;
            }

            if (!hasEO()) {
                List<SoftDeleteEntityExtensionPoint> exts = softDeleteExtensions.get(parent);
                if (exts == null) {
                    exts = new ArrayList<SoftDeleteEntityExtensionPoint>();
                    softDeleteExtensions.put(parent, exts);
                }

                exts.add(new SoftDeleteEntityExtensionPoint() {
                    @Override
                    public List<Class> getEntityClassForSoftDeleteEntityExtension() {
                        return Arrays.asList(parent);
                    }

                    @Override
                    public void postSoftDelete(Collection entityIds, Class entityClass) {
                        nativeSqlDelete(entityIds);
                    }
                });
            } else {
                List<SoftDeleteEntityByEOExtensionPoint> exts = softDeleteByEOExtensions.get(eoClass);
                if (exts == null) {
                    exts = new ArrayList<SoftDeleteEntityByEOExtensionPoint>();
                    softDeleteByEOExtensions.put(eoClass, exts);
                }

                exts.add(new SoftDeleteEntityByEOExtensionPoint() {
                    @Override
                    public List<Class> getEOClassForSoftDeleteEntityExtension() {
                        return Arrays.asList(eoClass);
                    }

                    @Override
                    public void postSoftDelete(Collection entityIds, Class EOClass) {
                        nativeSqlDelete(entityIds);
                    }
                });
            }
        }

        private void fireSoftDeleteExtension(Collection ids, Class entityClass) {
            List<SoftDeleteEntityExtensionPoint> exts = softDeleteExtensions.get(entityClass);
            if (exts != null) {
                for (SoftDeleteEntityExtensionPoint ext : exts) {
                    ext.postSoftDelete(ids, entityClass);
                }
            }

            for (SoftDeleteEntityExtensionPoint ext : softDeleteForAllExtensions) {
                ext.postSoftDelete(ids, entityClass);
            }
        }

        private void fireSoftDeleteExtensionByEOClass(Collection ids, Class eoClass) {
            List<SoftDeleteEntityByEOExtensionPoint> exts = softDeleteByEOExtensions.get(eoClass);
            if (exts != null) {
                for (SoftDeleteEntityByEOExtensionPoint ext : exts) {
                    ext.postSoftDelete(ids, eoClass);
                }
            }
        }

        boolean hasEO() {
            return eoClass != null;
        }

        private Object getEOPrimaryKeyValue(Object entity) {
            try {
                return eoPrimaryKeyField.get(entity);
            } catch (IllegalAccessException e) {
                throw new CloudRuntimeException(e);
            }
        }

        private Object getVOPrimaryKeyValue(Object entity) {
            try {
                return voPrimaryKeyField.get(entity);
            } catch (IllegalAccessException e) {
                throw new CloudRuntimeException(e);
            }
        }

        private void updateEO(Object entity, RuntimeException de) {
            Throwable rootCause = NestedExceptionUtils.getRootCause(de);
            if (rootCause == null
                    || !SQLIntegrityConstraintViolationException.class.isAssignableFrom(rootCause.getClass())) {
                throw de;
            }

            SQLIntegrityConstraintViolationException me = (SQLIntegrityConstraintViolationException) rootCause;
            if (!(me.getErrorCode() == 1062 && "23000".equals(me.getSQLState()) && me.getMessage().contains("PRIMARY"))) {
                throw de;
            }

            if (!hasEO()) {
                throw de;
            }

            // at this point, the error is caused by a update tried on VO entity which has been soft deleted. This is mostly
            // caused by a deletion cascade(e.g deleting host will cause vm running on it to be deleted, and deleting vm is trying to return capacity
            // to host which has been soft deleted, because vm deletion is executed in async manner). In this case, we make the update to EO table

            Object idval = getEOPrimaryKeyValue(entity);
            Object eo = getEntityManager().find(eoClass, idval);
            final Object deo = ObjectUtils.copy(eo, entity);
            new Runnable() {
                @Override
                @Transactional(propagation = Propagation.REQUIRES_NEW)
                public void run() {
                    getEntityManager().merge(deo);
                }
            }.run();
            logger.debug(String.format("A EO[%s] update has been made", eoClass.getName()));
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        private Object update(Object e, boolean refresh) {
            try {
                e = getEntityManager().merge(e);
                if (refresh) {
                    getEntityManager().flush();
                    getEntityManager().refresh(e);
                }
                return e;
            } catch (DataIntegrityViolationException | ConstraintViolationException exception) {
                updateEO(e, exception);
            }

            return e;
        }

        @DeadlockAutoRestart
        void update(Object e) {
            update(e, false);
        }

        @DeadlockAutoRestart
        Object updateAndRefresh(Object e) {
            return update(e, true);
        }

        private void hardDelete(Object entity) {
            entity = getEntityManager().merge(entity);
            getEntityManager().remove(entity);
            Object idval = getVOPrimaryKeyValue(entity);
            fireHardDeleteExtension(list(idval));
        }

        private void softDelete(Object entity) {
            try {
                Object idval = getEOPrimaryKeyValue(entity);
                if (idval == null) {
                    // the entity is physically deleted
                    return;
                }

                Object eo = getEntityManager().find(eoClass, idval);
                eoSoftDeleteColumn.set(eo, new Timestamp(new Date().getTime()).toString());
                getEntityManager().merge(eo);
                fireSoftDeleteExtension(Arrays.asList(idval), voClass);
                fireSoftDeleteExtensionByEOClass(Arrays.asList(idval), eoClass);
            } catch (CloudRuntimeException ce) {
                throw ce;
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        }

        private void softDelete(Collection ids) {
            String sql = String.format("update %s eo set eo.%s = (:date) where eo.%s in (:ids)",
                    eoClass.getSimpleName(), eoSoftDeleteColumn.getName(), eoPrimaryKeyField.getName());
            Query q = getEntityManager().createQuery(sql);
            q.setParameter("ids", ids);
            q.setParameter("date", new Timestamp(new Date().getTime()).toString());
            q.executeUpdate();

            fireSoftDeleteExtension(ids, voClass);
            fireSoftDeleteExtensionByEOClass(ids, eoClass);
        }

        private void fireHardDeleteExtension(Collection ids) {
            List<HardDeleteEntityExtensionPoint> exts = hardDeleteExtensions.get(voClass);
            if (exts != null) {
                for (HardDeleteEntityExtensionPoint ext : exts) {
                    ext.postHardDelete(ids, voClass);
                }
            }
            for (HardDeleteEntityExtensionPoint ext : hardDeleteForAllExtensions) {
                ext.postHardDelete(ids, voClass);
            }
        }

        private void hardDelete(Collection ids) {
            String tblName = hasEO() ? eoClass.getSimpleName() : voClass.getSimpleName();
            String sql = String.format("delete from %s eo where eo.%s in (:ids)", tblName, voPrimaryKeyField.getName());
            Query q = getEntityManager().createQuery(sql);
            q.setParameter("ids", ids);
            q.executeUpdate();
            logger.debug(String.format("hard delete %s records from %s", ids.size(), tblName));

            fireHardDeleteExtension(ids);
        }

        @Transactional
        private void nativeSqlDelete(Collection ids) {
            // native sql can avoid JPA cascades a deletion to parent entity when deleting a child entity
            String sql = String.format("delete from %s where %s in (:ids)", voClass.getSimpleName(), voPrimaryKeyField.getName());
            Query q = getEntityManager().createNativeQuery(sql);
            q.setParameter("ids", ids);
            q.executeUpdate();
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        void remove(Object entity) {
            if (!hasEO()) {
                hardDelete(entity);
            } else {
                softDelete(entity);
            }
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        void removeByPrimaryKey(Object id) {
            if (hasEO()) {
                softDelete(list(id));
            } else {
                hardDelete(list(id));
            }
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        void removeByPrimaryKeys(Collection ids) {
            if (hasEO()) {
                softDelete(ids);
            } else {
                hardDelete(ids);
            }
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        void removeCollection(Collection entities) {
            for (Object entity : entities) {
                if (!entity.getClass().isAnnotationPresent(EO.class)) {
                    hardDelete(entity);
                } else {
                    softDelete(entity);
                }
            }
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        Object reload(Object entity) {
            return getEntityManager().find(entity.getClass(), getVOPrimaryKeyValue(entity));
        }

        @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
        List listByPrimaryKeys(Collection ids, int offset, int length) {
            String sql = null;
            Query query = null;
            if (ids == null || ids.isEmpty()) {
                sql = String.format("select e from %s e", voClass.getSimpleName());
                query = getEntityManager().createQuery(sql, voClass);
            } else {
                sql = String.format("select e from %s e where e.%s in (:ids)", voClass.getSimpleName(), voPrimaryKeyField.getName());
                query = getEntityManager().createQuery(sql, voClass);
                query.setParameter("ids", ids);
            }
            query.setFirstResult(offset);
            query.setMaxResults(length);
            return query.getResultList();
        }

        @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
        boolean isExist(Object id) {
            String sql = String.format("select count(*) from %s ref where ref.%s = :id", voClass.getSimpleName(), voPrimaryKeyField.getName());
            TypedQuery<Long> q = getEntityManager().createQuery(sql, Long.class);
            q.setParameter("id", id);
            q.setMaxResults(1);
            Long count = q.getSingleResult();
            return count > 0;
        }

        void installLifeCycleCallback(EntityEvent evt, EntityLifeCycleCallback l) {
            listeners.put(evt, l);
        }

        void fireLifeCycleEvent(EntityEvent evt, Object o) {
            EntityLifeCycleCallback cb = listeners.get(evt);
            if (cb != null) {
                cb.entityLifeCycleEvent(evt, o);
            }
        }
    }

    @Transactional(readOnly = true)
    private void getDbVersionOnInit() {
        String sql = "select version from schema_version";
        Query q = getEntityManager().createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<String> versions = q.getResultList();
        dbVersion = versions.stream()
                .map(VersionComparator::new)
                .max(VersionComparator::compare)
                .map(VersionComparator::toString)
                .orElseThrow(() -> new CloudRuntimeException("cannot get db version."));
    }

    void init() {
        buildEntityInfo();
        getDbVersionOnInit();
    }

    @Override
    public <T> T persist(T entity) {
        return persist(entity, false);
    }

    EntityInfo getEntityInfo(Class clz) {
        EntityInfo info = entityInfoMap.get(clz);
        DebugUtils.Assert(info != null, String.format("cannot find entity info for %s", clz.getName()));
        return info;
    }

    @Override
    public <T> void update(T entity) {
        getEntityInfo(entity.getClass()).update(entity);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return entityManagerFactory.getCriteriaBuilder();
    }

    @Override
    public <T> SimpleQuery<T> createQuery(Class<T> entityClass) {
        assert entityClass.isAnnotationPresent(Entity.class) : entityClass.getName() + " is not annotated by JPA @Entity";
        return new SimpleQueryImpl<T>(entityClass);
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public <T> T findById(long id, Class<T> entityClass) {
        return getEntityManager().find(entityClass, id);
    }

    @Override
    @DeadlockAutoRestart
    public void remove(Object entity) {
        getEntityInfo(entity.getClass()).remove(entity);
    }

    @Override
    @DeadlockAutoRestart
    public void removeCollection(Collection entities, Class entityClass) {
        if (entities.isEmpty()) {
            return;
        }

        getEntityInfo(entityClass).removeCollection(entities);
    }

    @Override
    @DeadlockAutoRestart
    public void removeByPrimaryKeys(Collection priKeys, Class entityClazz) {
        if (priKeys.isEmpty()) {
            return;
        }
        getEntityInfo(entityClazz).removeByPrimaryKeys(priKeys);
    }


    @Override
    public <T> T updateAndRefresh(T entity) {
        return (T) getEntityInfo(entity.getClass()).updateAndRefresh(entity);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public <T> T findByUuid(String uuid, Class<T> entityClass) {
        return this.getEntityManager().find(entityClass, uuid);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public <T> T find(Query q) {
        List<T> ret = q.getResultList();
        if (ret.size() > 1) {
            throw new CloudRuntimeException("more than one result found");
        }
        return ret.isEmpty() ? null : ret.get(0);
    }

    @Override
    @DeadlockAutoRestart
    public void removeByPrimaryKey(Object primaryKey, Class<?> entityClass) {
        getEntityInfo(entityClass).removeByPrimaryKey(primaryKey);
    }

    @Override
    @Transactional
    @ExceptionSafe
    public void hardDeleteCollectionSelectedBySQL(String sql, Class entityClass) {
        EntityInfo info = getEntityInfo(entityClass);
        Query q = getEntityManager().createQuery(sql);
        List ids = q.getResultList();
        if (ids.isEmpty()) {
            return;
        }

        info.hardDelete(ids);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private <T> T doPersist(T entity, boolean isRefresh) {
        this.entityForTranscationCallback(Operation.PERSIST, entity.getClass());
        getEntityManager().persist(entity);

        if (isRefresh) {
            getEntityManager().flush();
            getEntityManager().refresh(entity);
        }
        return entity;
    }

    @DeadlockAutoRestart
    private <T> T persist(T entity, boolean isRefresh) {
        return doPersist(entity, isRefresh);
    }

    @Override
    public <T> T persistAndRefresh(T entity) {
        return persist(entity, true);
    }

    @Override
    public long count(Class<?> entityClass) {
        SimpleQuery<?> query = this.createQuery(entityClass);
        return query.count();
    }

    private List<TransactionalCallback> getTransactionAsyncCallbacks() {
        if (transactionAsyncCallbacks == null) {
            transactionAsyncCallbacks = new ArrayList<TransactionalCallback>();
            PluginRegistry pluginRgty = Platform.getComponentLoader().getComponent(PluginRegistry.class);
            transactionAsyncCallbacks = pluginRgty.getExtensionList(TransactionalCallback.class);
        }
        return transactionAsyncCallbacks;
    }

    private List<TransactionalSyncCallback> getTransactionSyncCallbacks() {
        if (transactionSyncCallbacks == null) {
            transactionSyncCallbacks = new ArrayList<TransactionalSyncCallback>();
            PluginRegistry pluginRgty = Platform.getComponentLoader().getComponent(PluginRegistry.class);
            transactionSyncCallbacks = pluginRgty.getExtensionList(TransactionalSyncCallback.class);
        }
        return transactionSyncCallbacks;
    }

    @Override
    public void entityForTranscationCallback(Operation op, Class<?>... entityClass) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            for (TransactionalSyncCallback cb : getTransactionSyncCallbacks()) {
                TransactionSynchronizationSyncImpl tsi = new TransactionSynchronizationSyncImpl(cb, op, entityClass);
                TransactionSynchronizationManager.registerSynchronization(tsi);
            }

            for (TransactionalCallback cb : getTransactionAsyncCallbacks()) {
                TransactionSynchronizationAsyncImpl tsi = new TransactionSynchronizationAsyncImpl(cb, op, entityClass);
                TransactionSynchronizationManager.registerSynchronization(tsi);
            }
        } else {
            StringBuilder sb = new StringBuilder();
            for (Class<?> c : entityClass) {
                sb.append(c.getName()).append(",");
            }

            String err = String.format("entityForTranscationCallback is called but transcation is not active. Did you forget adding @Transactional to method??? [operation: %s, entity classes: %s]", op, sb.toString());
            logger.warn(err);
        }
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> T reload(T entity) {
        return (T) getEntityInfo(entity.getClass()).reload(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void doUpdateCollection(Collection entities) {
        for (Object e : entities) {
            getEntityManager().merge(e);
        }
    }

    @Override
    @DeadlockAutoRestart
    public void updateCollection(Collection entities) {
        doUpdateCollection(entities);
    }

    @Override
    public long generateSequenceNumber(Class<?> seqTable) {
        try {
            Field id = seqTable.getDeclaredField("id");
            if (id == null) {
                throw new CloudRuntimeException(String.format("sequence VO[%s] must have 'id' field", seqTable.getName()));
            }
            Object vo = seqTable.newInstance();
            vo = persistAndRefresh(vo);
            id.setAccessible(true);
            return (Long) id.get(vo);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public <T> List<T> listByApiMessage(APIListMessage msg, Class<T> clazz) {
        return listByPrimaryKeys(msg.getUuids(), msg.getOffset(), msg.getLength(), clazz);
    }

    @Override
    public <T> List<T> listAll(Class<T> clazz) {
        return listAll(0, Integer.MAX_VALUE, clazz);
    }

    @Override
    public <T> List<T> listAll(int offset, int length, Class<T> clazz) {
        return listByPrimaryKeys(null, offset, length, clazz);
    }

    @Override
    public <T> List<T> listByPrimaryKeys(Collection ids, Class<T> clazz) {
        return listByPrimaryKeys(ids, 0, Integer.MAX_VALUE, clazz);
    }

    @Override
    public <T> List<T> listByPrimaryKeys(Collection ids, int offset, int length, Class<T> clazz) {
        return getEntityInfo(clazz).listByPrimaryKeys(ids, offset, length);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistCollection(Collection entities) {
        for (Object e : entities) {
            this.entityForTranscationCallback(Operation.PERSIST, e.getClass());
            this.getEntityManager().persist(e);
        }
    }

    @Override
    public boolean isExist(Object id, Class<?> clazz) {
        return getEntityInfo(clazz).isExist(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void _eoCleanup(Class VOClazz) {
        EntityInfo info = getEntityInfo(VOClazz);

        String deleted = info.eoSoftDeleteColumn.getName();
        String sql = String.format("select eo.%s from %s eo where eo.%s is not null", info.voPrimaryKeyField.getName(),
                info.eoClass.getSimpleName(), deleted);
        Query q = getEntityManager().createQuery(sql);
        List ids = q.getResultList();
        if (ids.isEmpty()) {
            return;
        }

        info.hardDelete(ids);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void _eoCleanup(Class VOClazz, Object id) {
        EntityInfo info = getEntityInfo(VOClazz);
        if (!info.hasEO()) {
            logger.warn(String.format("Class[%s] doesn't has EO.", VOClazz));
            return;
        }

        String deleted = info.eoSoftDeleteColumn.getName();
        String sql = String.format("select eo from %s eo where eo.%s is not null and eo.%s = :id",
                info.eoClass.getSimpleName(), deleted, info.voPrimaryKeyField.getName());
        Query q = getEntityManager().createQuery(sql, info.eoClass);
        q.setParameter("id", id);
        List result = q.getResultList();
        if (result.isEmpty()) {
            return;
        }

        info.hardDelete(result.get(0));
    }

    @Override
    @DeadlockAutoRestart
    public void eoCleanup(Class VOClazz) {
        EntityInfo info = getEntityInfo(VOClazz);
        if (!info.hasEO()) {
            logger.warn(String.format("Class[%s] doesn't has EO.", VOClazz));
            return;
        }

        _eoCleanup(VOClazz);
    }

    @Override
    @DeadlockAutoRestart
    public void eoCleanup(Class VOClazz, Object id) {
        if(id == null) {
            throw new RuntimeException(String.format("Cleanup %s EO  fail, id is null", VOClazz.getSimpleName()));
        }

        _eoCleanup(VOClazz, id);
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    public void setExtraDataSource(DataSource extraDataSource) {
        this.extraDataSource = extraDataSource;
    }

    @Override
    public DataSource getExtraDataSource() {
        return extraDataSource;
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void buildEntityInfo() {
        BeanUtils.reflections.getTypesAnnotatedWith(Entity.class).forEach(clz-> {
            entityInfoMap.put(clz, new EntityInfo(clz));
        });
    }

    private void populateExtensions() {
        for (SoftDeleteEntityExtensionPoint ext : pluginRgty.getExtensionList(SoftDeleteEntityExtensionPoint.class)) {
            if (ext.getEntityClassForSoftDeleteEntityExtension() == null) {
                softDeleteForAllExtensions.add(ext);
                continue;
            }

            for (Class eclazz : ext.getEntityClassForSoftDeleteEntityExtension()) {
                List<SoftDeleteEntityExtensionPoint> exts = softDeleteExtensions.get(eclazz);
                if (exts == null) {
                    exts = new ArrayList<SoftDeleteEntityExtensionPoint>();
                    softDeleteExtensions.put(eclazz, exts);
                }
                exts.add(ext);
            }
        }

        for (SoftDeleteEntityByEOExtensionPoint ext : pluginRgty.getExtensionList(SoftDeleteEntityByEOExtensionPoint.class)) {
            for (Class eoClass : ext.getEOClassForSoftDeleteEntityExtension()) {
                List<SoftDeleteEntityByEOExtensionPoint> exts = softDeleteByEOExtensions.get(eoClass);
                if (exts == null) {
                    exts = new ArrayList<SoftDeleteEntityByEOExtensionPoint>();
                    softDeleteByEOExtensions.put(eoClass, exts);
                }
                exts.add(ext);
            }
        }

        for (HardDeleteEntityExtensionPoint ext : pluginRgty.getExtensionList(HardDeleteEntityExtensionPoint.class)) {
            if (ext.getEntityClassForHardDeleteEntityExtension() == null) {
                hardDeleteForAllExtensions.add(ext);
                continue;
            }

            for (Class clazz : ext.getEntityClassForHardDeleteEntityExtension()) {
                List<HardDeleteEntityExtensionPoint> exts = hardDeleteExtensions.get(clazz);
                if (exts == null) {
                    exts = new ArrayList<HardDeleteEntityExtensionPoint>();
                    hardDeleteExtensions.put(clazz, exts);
                }
                exts.add(ext);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Timestamp getCurrentSqlTime() {
        Query query = getEntityManager().createNativeQuery("select current_timestamp()");
        return (Timestamp) query.getSingleResult();
    }

    @Override
    public String getDbVersion() {
        return dbVersion;
    }

    @Override
    public void installEntityLifeCycleCallback(Class clz, EntityEvent evt, EntityLifeCycleCallback cb) {
        if (clz != null) {
            EntityInfo info = entityInfoMap.get(clz);
            DebugUtils.Assert(info != null, String.format("cannot find EntityInfo for the class[%s]", clz));
            info.installLifeCycleCallback(evt, cb);
        } else {
            for (EntityInfo info : entityInfoMap.values()) {
                info.installLifeCycleCallback(evt, cb);
            }
        }
    }

    @Override
    public boolean stop() {
        return true;
    }

    void entityEvent(EntityEvent evt, Object entity) {
        EntityInfo info = entityInfoMap.get(entity.getClass());
        if (info == null) {
            logger.warn(String.format("cannot find EntityInfo for the class[%s], not entity events will be fired", entity.getClass()));
            return;
        }

        info.fireLifeCycleEvent(evt, entity);
    }

    public static AtomicLong getDberror() {
        return dberror;
    }

    public static long increaseDberror() {
        return dberror.incrementAndGet();
    }

    public static long increaseDeadlock() {
        return dbdeadlock.incrementAndGet();
    }

    public static long increaseLocktimeout() {
        return dblocktimeout.incrementAndGet();
    }

    public static AtomicLong getDbdeadlock() {
        return dbdeadlock;
    }

    public static AtomicLong getDblocktimeout() {
        return dblocktimeout;
    }
}
