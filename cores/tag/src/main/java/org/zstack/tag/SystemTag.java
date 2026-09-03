package org.zstack.tag;

import java.sql.SQLIntegrityConstraintViolationException;
import org.hibernate.TransactionException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.tag.*;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.PersistenceException;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.inerr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SystemTag {
    private static final CLogger logger = Utils.getLogger(SystemTag.class);

    @Autowired
    protected DatabaseFacade dbf;

    // TagManager must be explicitly set. use @Autowired will cause circular dependency
    protected TagManagerImpl tagMgr;

    protected String tagFormat;
    protected Class resourceClass;
    protected List<SystemTagValidator> validators = new ArrayList<>();
    protected List<SystemTagLifeCycleListener> lifeCycleListeners = new ArrayList<>();
    protected List<SystemTagOperationJudger> judgers = new ArrayList<>();

    private boolean ephemeral = false;
    private boolean adminOnly = false;
    private boolean cloneable = true;
    private boolean sensitive = false;

    public SystemTag(String tagFormat, Class resourceClass) {
        this.tagFormat = tagFormat;
        this.resourceClass = resourceClass;
    }

    public static SystemTag makeEphemeralTag(String tagScript) {
        String tagFormat = String.format("%s::%s", TagConstant.EPHEMERAL_TAG_PREFIX, tagScript);
        return new SystemTag(tagFormat, SystemTagVO.class).markAsEphemeral();
    }

    public enum SystemTagOperation {
        CREATE,
        UPDATE,
        DELETE
    }

    public SystemTag installLifeCycleListener(SystemTagLifeCycleListener listener) {
        lifeCycleListeners.add(listener);
        return this;
    }

    public SystemTag installJudger(SystemTagOperationJudger judger) {
        judgers.add(judger);
        return this;
    }

    void callCreatedJudger(SystemTagInventory tag) {
        for (SystemTagOperationJudger j : judgers) {
            j.tagPreCreated(tag);
        }
    }

    void callTagCreatedListener(SystemTagInventory tag) {
        for (SystemTagLifeCycleListener ext : lifeCycleListeners) {
            ext.tagCreated(tag);
        }
    }

    void callDeletedJudger(SystemTagInventory tag) {
        for (SystemTagOperationJudger j : judgers) {
            j.tagPreDeleted(tag);
        }
    }

    void callTagDeletedListener(SystemTagInventory tag) {
        for (SystemTagLifeCycleListener ext : lifeCycleListeners) {
            ext.tagDeleted(tag);
        }
    }

    void callUpdatedJudger(SystemTagInventory old, SystemTagInventory newTag) {
        for (SystemTagOperationJudger j : judgers) {
            j.tagPreUpdated(old, newTag);
        }
    }

    void callTagUpdatedListener(SystemTagInventory old, SystemTagInventory newTag) {
        for (SystemTagLifeCycleListener ext : lifeCycleListeners) {
            ext.tagUpdated(old, newTag);
        }
    }

    protected String useTagFormat() {
        return tagFormat;
    }

    protected Op useOp() {
        return Op.EQ;
    }

    public boolean hasTag(String resourceUuid) {
        return hasTag(resourceUuid, resourceClass);
    }

    public boolean hasTag(String resourceUuid, Class resourceClass) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.add(SystemTagVO_.resourceType, Op.EQ, resourceClass.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.EQ, resourceUuid);
        q.add(SystemTagVO_.tag, useOp(), useTagFormat());
        return q.isExists();
    }

    public List<String> filterResourceHasTag(Collection<String> resourceUuids) {
        if (resourceUuids.isEmpty()) {
            return new ArrayList<>();
        }

        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.add(SystemTagVO_.resourceType, Op.EQ, resourceClass.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.IN, resourceUuids);
        q.add(SystemTagVO_.tag, useOp(), useTagFormat());
        q.select(SystemTagVO_.resourceUuid);
        return q.listValue();
    }

    public void copy(String srcUuid, Class srcClass, String dstUuid, Class dstClass) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.add(SystemTagVO_.resourceType, Op.EQ, srcClass.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.EQ, srcUuid);
        q.add(SystemTagVO_.tag, useOp(), useTagFormat());
        List<SystemTagVO> tags = q.list();
        for (SystemTagVO tag : tags) {
            if (tag.isInherent()) {
                deleteInherentTag(dstUuid, dstClass);
                tagMgr.createInherentSystemTag(dstUuid, tag.getTag(), dstClass.getSimpleName());
            } else {
                delete(dstUuid, dstClass);
                tagMgr.createNonInherentSystemTag(dstUuid, tag.getTag(), dstClass.getSimpleName());
            }
        }
    }

    public List<String> getTags(String resourceUuid, Class resourceClass) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.select(SystemTagVO_.tag);
        q.add(SystemTagVO_.resourceType, Op.EQ, resourceClass.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.EQ, resourceUuid);
        q.add(SystemTagVO_.tag, useOp(), useTagFormat());
        return q.listValue();
    }

    public List<String> getTags(String resourceUuid) {
        return getTags(resourceUuid, resourceClass);
    }

    public String getTag(String resourceUuid, Class resourceClass) {
        List<String> tags = getTags(resourceUuid, resourceClass);
        if (tags.isEmpty()) {
            return null;
        }
        return tags.get(0);
    }

    public String getTag(String resourceUuid) {
        return getTag(resourceUuid, resourceClass);
    }

    public Map<String, List<String>> getTags(Collection<String> resourceUuids, Class resourceClass) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.select(SystemTagVO_.tag, SystemTagVO_.resourceUuid);
        q.add(SystemTagVO_.resourceType, Op.EQ, resourceClass.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.IN, resourceUuids);
        q.add(SystemTagVO_.tag, useOp(), useTagFormat());
        List<Tuple> ts = q.listTuple();
        Map<String, List<String>> ret = new HashMap<>();
        for (Tuple t : ts) {
            String uuid = t.get(1, String.class);
            List<String> tags = ret.get(uuid);
            if (tags == null) {
                tags = new ArrayList<>();
                ret.put(uuid, tags);
            }
            tags.add(t.get(0, String.class));
        }
        return ret;
    }

    public Map<String, List<String>> getTags(Collection<String> resourceUuids) {
        DebugUtils.Assert(!resourceUuids.isEmpty(), "how can you pass an empty resourceUuids");
        return getTags(resourceUuids, resourceClass);
    }

    public String getTagFormat() {
        return tagFormat;
    }

    public Class getResourceClass() {
        return resourceClass;
    }

    /**
     * @return true if any tags have been deleted
     */
    public boolean delete(String resourceUuid, Class resourceClass) {
        return tagMgr.deleteSystemTag(tagFormat, resourceUuid, resourceClass.getSimpleName(), false);
    }

    /**
     * @return true if any tags have been deleted
     */
    public boolean delete(String resourceUuid) {
        return tagMgr.deleteSystemTag(tagFormat, resourceUuid, resourceClass.getSimpleName(), false);
    }

    /**
     * @return true if any tags have been deleted
     */
    public boolean deleteInherentTag(String resourceUuid) {
        return tagMgr.deleteSystemTag(tagFormat, resourceUuid, resourceClass.getSimpleName(), true);
    }

    /**
     * @return true if any tags have been deleted
     */
    public boolean deleteInherentTag(String resourceUuid, Class resourceClass) {
        return tagMgr.deleteSystemTag(tagFormat, resourceUuid, resourceClass.getSimpleName(), true);
    }

    public String instantiateTag(Map tokens) {
        return tagFormat;
    }

    public SystemTagCreator newSystemTagCreator(String resUuid) {
        if (ephemeral) {
            throw new OperationFailureException(inerr("ephemeral tag cannot be persisted")
                    .withOpaque("resource.uuid", resUuid)
                    .withOpaque("tag.format", tagFormat));
        }

        SystemTag self = this;

        return new SystemTagCreator() {
            private boolean exceptionCanBeIgnored;

            @Override
            public SystemTagInventory create() {
                try {
                    return doCreate();
                } catch (TransactionSystemException e) {
                    if (exceptionCanBeIgnored) {
                        return null;
                    } else {
                        throw e;
                    }
                } catch (JpaSystemException e) {
                    if (e.getCause() != null && e.getCause() instanceof TransactionException) {
                        if (exceptionCanBeIgnored) {
                            return null;
                        }
                    }

                    throw e;
                } catch (UnexpectedRollbackException e) {
                    if (exceptionCanBeIgnored) {
                        return null;
                    }

                    throw e;
                }
            }

            @Override
            public void setTagByTokens(Map tokens) {
                tag = instantiateTag(tokens);
            }

            @Transactional
            private SystemTagInventory doCreate() {
                build();

                tagMgr.validateSystemTag(resourceUuid, resourceClass.getSimpleName(), tag);

                tag = tagMgr.applyBeforePersist(resourceUuid, resourceClass.getSimpleName(), tag);

                SystemTagVO keepSameTagVO = null;
                if (recreate) {
                    String sql;
                    if (useOp() == Op.LIKE) {
                        sql = "select t from SystemTagVO t where t.tag like :tag" +
                                " and t.resourceUuid = :resUuid and t.inherent = :inherent" +
                                " and t.resourceType = :resType";
                    } else {
                        sql = "select t from SystemTagVO t where t.tag = :tag" +
                                " and t.resourceUuid = :resUuid and t.inherent = :inherent" +
                                " and t.resourceType = :resType";
                    }

                    TypedQuery<SystemTagVO> q = dbf.getEntityManager().createQuery(sql, SystemTagVO.class);
                    q.setParameter("resUuid", resourceUuid);
                    q.setParameter("tag", useTagFormat());
                    q.setParameter("inherent", inherent);
                    q.setParameter("resType", resourceClass.getSimpleName());
                    List<SystemTagVO> vos = q.getResultList();

                    List<SystemTagInventory> invs = SystemTagInventory.valueOf(vos);
                    for (SystemTagInventory inv : invs) {
                        tagMgr.preTagDeleted(inv);
                    }

                    for (SystemTagVO vo : vos) {
                        if (quickRecreate && vo.getTag().equals(this.tag) && keepSameTagVO == null) {
                            keepSameTagVO = vo;
                            continue;
                        }

                        dbf.getEntityManager().remove(vo);
                    }

                    dbf.getEntityManager().flush();

                    tagMgr.fireTagDeleted(invs);
                }

                if (quickRecreate && keepSameTagVO != null) {
                    return SystemTagInventory.valueOf(keepSameTagVO);
                }

                String uuid;
                if (unique) {
                    String key = String.format("%s-%s-%s", resourceUuid, resourceClass.getSimpleName(), tag);
                    uuid = UUID.nameUUIDFromBytes(key.getBytes()).toString().replaceAll("-", "");
                } else {
                    uuid = Platform.getUuid();
                }

                /**
                 * fix issues 1970
                 *
                 * Check whether the data exists, if there is no longer insert (another recreate for true will not touch this logic, it will not change the original code behavior),
                 * This reduces the vast majority of Hibernate redundant error logs.
                 *
                 * Background: If we insert duplicate data, hibernate will produce an error log, resulting in a large number of redundant error log.
                 */
                String sql = "select count(t) from SystemTagVO t where t.uuid = :uuid";
                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("uuid", uuid);
                if(q.getSingleResult() > 0){ // tag exists
                    if (!ignoreIfExisting) {
                        throw new CloudRuntimeException(String.format("duplicate system tag[uuid: %s]", uuid));
                    } else {
                        exceptionCanBeIgnored = true;
                        return null;
                    }
                }

                SystemTagVO vo = new SystemTagVO();
                vo.setResourceType(resourceClass.getSimpleName());
                vo.setUuid(uuid);
                vo.setResourceUuid(resourceUuid);
                vo.setInherent(inherent);
                vo.setTag(tag);
                vo.setType(TagType.System);

                tagMgr.preTagCreated(SystemTagInventory.valueOf(vo));

                try {
                    // If execution fails, hibernate will generate an error log
                    dbf.getEntityManager().persist(vo);
                    dbf.getEntityManager().flush();
                    dbf.getEntityManager().refresh(vo);
                } catch (PersistenceException e) {
                    if (ExceptionDSL.isCausedBy(e, SQLIntegrityConstraintViolationException.class, "Duplicate entry")) {
                        // tag exists
                        if (!ignoreIfExisting) {
                            throw new CloudRuntimeException(String.format("duplicate system tag[resourceUuid: %s," +
                                    "resourceType: %s, tag: %s", resourceUuid, resourceClass.getSimpleName(), tag), e);
                        } else {
                            logger.debug(String.format("please ignore this error log: Duplicate entry '%s' for key 'PRIMARY'", uuid));
                            exceptionCanBeIgnored = true;
                            return null;
                        }
                    }
                }

                SystemTagInventory inv = SystemTagInventory.valueOf(vo);
                tagMgr.fireTagCreated(asList(inv));

                return inv;
            }

            private void build() {
                resourceUuid = resUuid;
                if (resourceClass == null) {
                    resourceClass = self.resourceClass;
                }

                if (tag == null) {
                    tag = getTagFormat();
                }
            }
        };
    }

    public SystemTag installValidator(SystemTagValidator validator) {
        validators.add(validator);
        return this;
    }

    public boolean isMatch(String tag) {
        return tagFormat.equals(tag);
    }

    void validate(String resourceUuid, Class resourceType, String tag) {
        for (SystemTagValidator validator : validators) {
            validator.validateSystemTag(resourceUuid, resourceType, tag);
        }
    }

    public SystemTagInventory updateByTagUuid(String tagUuid, String newTag) {
        return tagMgr.updateSystemTag(tagUuid, newTag);
    }

    public SystemTagInventory updateUnique(String resourceUuid, String oldTag, String newTag) {
        String tagUuid = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, resourceUuid).
                eq(SystemTagVO_.resourceType, resourceClass.getSimpleName()).like(SystemTagVO_.tag, oldTag).
                select(SystemTagVO_.uuid).findValue();
        if (tagUuid == null) {
            return null;
        }

        return tagMgr.updateSystemTag(tagUuid, newTag);
    }

    public SystemTagInventory update(String resourceUuid, String newTag) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.select(SystemTagVO_.uuid);
        q.add(SystemTagVO_.resourceType, Op.EQ, resourceClass.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.EQ, resourceUuid);
        q.add(SystemTagVO_.tag, useOp(), useTagFormat());
        String tagUuid = q.findValue();
        if (tagUuid == null) {
            return null;
        }

        return tagMgr.updateSystemTag(tagUuid, newTag);
    }

    public void setTagMgr(TagManager tagMgr) {
        this.tagMgr = (TagManagerImpl) tagMgr;
    }

    public List<SystemTagValidator> getValidators() {
        return validators;
    }

    public void setValidators(List<SystemTagValidator> validators) {
        this.validators = validators;
    }

    public SystemTag markAsEphemeral() {
        this.ephemeral = true;
        return this;
    }

    public SystemTag markAsAdminOnly() {
        this.adminOnly = true;
        return this;
    }

    public SystemTag disableClone() {
        this.cloneable = false;
        return this;
    }

    public SystemTag markAsSensitive() {
        this.sensitive = true;
        return this;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public boolean isAdminOnly() {
        return adminOnly;
    }

    public boolean isCloneable() {
        return cloneable;
    }

    public boolean isSensitive() {
        return sensitive;
    }
}
