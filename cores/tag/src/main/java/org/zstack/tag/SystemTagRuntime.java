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
import org.zstack.utils.Tags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.PersistenceException;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.inerr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SystemTagRuntime extends PatternedSystemTag {
    private static final CLogger logger = Utils.getLogger(SystemTagRuntime.class);

    @Autowired
    protected DatabaseFacade dbf;

    // TagManager must be explicitly set. use @Autowired will cause circular dependency
    protected TagManagerImpl tagMgr;

    public SystemTagRuntime(String tagFormat, Class resourceClass, boolean hasPattern) {
        super(tagFormat, resourceClass);
        this.hasPattern = hasPattern;
    }

    public SystemTagRuntime(String tagFormat, Class resourceClass) {
        this(tagFormat, resourceClass, true);
    }

    @Override
    public SystemTagRuntime asRuntime() {
        return this;
    }

    @Override
    protected String useTagFormat() {
        return hasPattern ? Tags.tagPatternToSqlPattern(tagFormat) : tagFormat;
    }

    @Override
    protected boolean useLikeOp() {
        return hasPattern;
    }

    private Op useOp() {
        return useLikeOp() ? Op.LIKE : Op.EQ;
    }

    @Override
    public boolean hasTag(String resourceUuid) {
        return hasTag(resourceUuid, resourceClass);
    }

    @Override
    public boolean hasTag(String resourceUuid, Class resourceClass) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.add(SystemTagVO_.resourceType, Op.EQ, resourceClass.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.EQ, resourceUuid);
        q.add(SystemTagVO_.tag, useOp(), useTagFormat());
        return q.isExists();
    }

    @Override
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

    @Override
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

    @Override
    public List<String> getTags(String resourceUuid, Class resourceClass) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.select(SystemTagVO_.tag);
        q.add(SystemTagVO_.resourceType, Op.EQ, resourceClass.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.EQ, resourceUuid);
        q.add(SystemTagVO_.tag, useOp(), useTagFormat());
        return q.listValue();
    }

    @Override
    public List<String> getTags(String resourceUuid) {
        return getTags(resourceUuid, resourceClass);
    }

    @Override
    public String getTag(String resourceUuid, Class resourceClass) {
        List<String> tags = getTags(resourceUuid, resourceClass);
        if (tags.isEmpty()) {
            return null;
        }
        return tags.get(0);
    }

    @Override
    public String getTag(String resourceUuid) {
        return getTag(resourceUuid, resourceClass);
    }

    @Override
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

    @Override
    public Map<String, List<String>> getTags(Collection<String> resourceUuids) {
        DebugUtils.Assert(!resourceUuids.isEmpty(), "how can you pass an empty resourceUuids");
        return getTags(resourceUuids, resourceClass);
    }

    @Override
    public boolean delete(String resourceUuid, Class resourceClass) {
        if (hasPattern) {
            return tagMgr.deleteSystemTagUseLike(useTagFormat(), resourceUuid, resourceClass.getSimpleName(), false);
        }
        return tagMgr.deleteSystemTag(tagFormat, resourceUuid, resourceClass.getSimpleName(), false);
    }

    @Override
    public boolean delete(String resourceUuid, String tagFormat) {
        return tagMgr.deleteSystemTagUseLike(tagFormat, resourceUuid, resourceClass.getSimpleName(), false);
    }

    @Override
    public boolean delete(String resourceUuid) {
        return delete(resourceUuid, resourceClass);
    }

    @Override
    public boolean deleteInherentTag(String resourceUuid) {
        return deleteInherentTag(resourceUuid, resourceClass);
    }

    @Override
    public boolean deleteInherentTag(String resourceUuid, String tagFormat) {
        return tagMgr.deleteSystemTagUseLike(tagFormat, resourceUuid, resourceClass.getSimpleName(), true);
    }

    @Override
    public boolean deleteInherentTag(String resourceUuid, Class resourceClass) {
        if (hasPattern) {
            return tagMgr.deleteSystemTagUseLike(useTagFormat(), resourceUuid, resourceClass.getSimpleName(), true);
        }
        return tagMgr.deleteSystemTag(tagFormat, resourceUuid, resourceClass.getSimpleName(), true);
    }

    private String getTokenByTag(String tag, String tokenName, Class resourceClass) {
        Map<String, String> tokens = getTokensByTag(tag);
        if (tokens == null) {
            return null;
        }
        String value = tokens.get(tokenName);
        if (value == null || tagMgr == null) {
            return value;
        }
        String tagHead = Tags.first(tag);
        return tagMgr.decryptTokenValue(resourceClass.getSimpleName(), tagHead, tokenName, value);
    }

    private Map<String, String> getDecryptedTokensByTag(String tag, Class resourceClass) {
        Map<String, String> tokens = getTokensByTag(tag);
        if (tokens == null || tagMgr == null) {
            return tokens;
        }

        String tagHead = Tags.first(tag);
        tagHead = Objects.equals(tagHead, tag) ? null : tagHead;
        Map<String, String> decrypted = new HashMap<>();
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            String tokenName = entry.getKey();
            String value = entry.getValue();
            decrypted.put(tokenName, value == null ? null :
                    tagMgr.decryptTokenValue(resourceClass.getSimpleName(),
                            tagHead == null ? tokenName : tagHead, tokenName, value));
        }
        return decrypted;
    }

    @Override
    public Map<String, String> getTokensByResourceUuid(String resourceUuid, Class resourceClass) {
        String tag = getTag(resourceUuid, resourceClass);
        if (tag == null) {
            return null;
        }

        return getDecryptedTokensByTag(tag, resourceClass);
    }

    @Override
    public List<Map<String, String>> getTokensOfTagsByResourceUuid(String resourceUuid) {
        return getTokensOfTagsByResourceUuid(resourceUuid, resourceClass);
    }

    @Override
    public List<Map<String, String>> getTokensOfTagsByResourceUuid(String resourceUuid, Class resourceClass) {
        List<Map<String, String>> res = new ArrayList<>();

        List<String> tags = getTags(resourceUuid, resourceClass);
        for (String tag : tags) {
            res.add(getDecryptedTokensByTag(tag, resourceClass));
        }

        return res;
    }

    @Override
    public Map<String, String> getTokensByResourceUuid(String resourceUuid) {
        return getTokensByResourceUuid(resourceUuid, resourceClass);
    }

    @Override
    public String getTokenByResourceUuid(String resourceUuid, Class resourceClass, String tokenName) {
        String tag = getTag(resourceUuid, resourceClass);
        if (tag == null) {
            return null;
        }
        return getTokenByTag(tag, tokenName, resourceClass);
    }

    @Override
    public String getTokenByResourceUuid(String resourceUuid, String tokenName) {
        return getTokenByResourceUuid(resourceUuid, resourceClass, tokenName);
    }

    @Override
    public SystemTagInventory getTagInventory(String resourceUuid) {
        SystemTagVO vo;
        if (hasPattern) {
            vo = Q.New(SystemTagVO.class)
                    .eq(SystemTagVO_.resourceUuid, resourceUuid)
                    .eq(SystemTagVO_.resourceType, getResourceClass().getSimpleName())
                    .like(SystemTagVO_.tag, useTagFormat())
                    .find();
        } else {
            vo = Q.New(SystemTagVO.class)
                    .eq(SystemTagVO_.resourceUuid, resourceUuid)
                    .eq(SystemTagVO_.resourceType, getResourceClass().getSimpleName())
                    .eq(SystemTagVO_.tag, useTagFormat())
                    .find();
        }
        return vo == null ? null : SystemTagInventory.valueOf(vo);
    }

    @Override
    public List<SystemTagInventory> getTagInventories(List<String> resourceUuids) {
        List<SystemTagVO> list;
        if (hasPattern) {
            list = Q.New(SystemTagVO.class)
                    .in(SystemTagVO_.resourceUuid, resourceUuids)
                    .eq(SystemTagVO_.resourceType, getResourceClass().getSimpleName())
                    .like(SystemTagVO_.tag, useTagFormat())
                    .list();
        } else {
            list = Q.New(SystemTagVO.class)
                    .in(SystemTagVO_.resourceUuid, resourceUuids)
                    .eq(SystemTagVO_.resourceType, getResourceClass().getSimpleName())
                    .eq(SystemTagVO_.tag, useTagFormat())
                    .list();
        }
        return SystemTagInventory.valueOf(list);
    }

    @Override
    public List<SystemTagInventory> getTagInventories(String resourceUuid) {
        if (hasPattern) {
            return SystemTagInventory.valueOf(Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceType, getResourceClass().getSimpleName()).
                    eq(SystemTagVO_.resourceUuid, resourceUuid).like(SystemTagVO_.tag, useTagFormat()).list());
        }
        return SystemTagInventory.valueOf(Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceType, getResourceClass().getSimpleName()).
                eq(SystemTagVO_.resourceUuid, resourceUuid).eq(SystemTagVO_.tag, useTagFormat()).list());
    }

    @Override
    public void copyTagInventories(String srcUuid, Class srcResourceClass, String dstUuid, Class dstResourceClass, boolean inherent) {
        if (!isCloneable()) {
            return;
        }

        String rawTag = getTag(srcUuid, srcResourceClass);
        if (rawTag == null) {
            return;
        }

        if (getTag(dstUuid, dstResourceClass) != null) {
            delete(dstUuid, dstResourceClass);
        }

        String dstTag = tagMgr.transformTagForCopy(srcUuid, srcResourceClass.getSimpleName(),
                dstUuid, dstResourceClass.getSimpleName(), rawTag);
        if (dstTag == null) {
            return;
        }

        SystemTagVO svo = new SystemTagVO();
        svo.setUuid(Platform.getUuid());
        svo.setInherent(inherent);
        svo.setResourceUuid(dstUuid);
        svo.setResourceType(dstResourceClass.getSimpleName());
        svo.setTag(dstTag);
        dbf.persistAndRefresh(svo);
    }

    @Override
    public boolean updateTagByToken(String resourceUuid, String tokenName, String newTag) {
        SystemTagVO vo;
        if (hasPattern) {
            vo = Q.New(SystemTagVO.class)
                    .eq(SystemTagVO_.resourceUuid, resourceUuid)
                    .eq(SystemTagVO_.resourceType, getResourceClass().getSimpleName())
                    .like(SystemTagVO_.tag, useTagFormat())
                    .find();
        } else {
            vo = Q.New(SystemTagVO.class)
                    .eq(SystemTagVO_.resourceUuid, resourceUuid)
                    .eq(SystemTagVO_.resourceType, getResourceClass().getSimpleName())
                    .eq(SystemTagVO_.tag, useTagFormat())
                    .find();
        }

        if (vo == null) {
            return false;
        }

        Map<String, String> rawTokens = getTokensByTag(vo.getTag());
        String oldTag = rawTokens == null ? null : rawTokens.get(tokenName);
        if (oldTag == null) {
            return false;
        }

        tagMgr.updateSystemTag(vo.getUuid(), vo.getTag().replace(oldTag, newTag));

        return true;
    }

    @Override
    public SystemTagCreator newSystemTagCreator(String resUuid) {
        if (isEphemeral()) {
            throw new OperationFailureException(inerr("ephemeral tag cannot be persisted")
                    .withOpaque("resource.uuid", resUuid)
                    .withOpaque("tag.format", tagFormat));
        }

        SystemTagRuntime self = this;

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
                    if (useLikeOp()) {
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

    @Override
    public SystemTagInventory updateByTagUuid(String tagUuid, String newTag) {
        return tagMgr.updateSystemTag(tagUuid, newTag);
    }

    @Override
    public SystemTagInventory updateUnique(String resourceUuid, String oldTag, String newTag) {
        String tagUuid = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, resourceUuid).
                eq(SystemTagVO_.resourceType, resourceClass.getSimpleName()).like(SystemTagVO_.tag, oldTag).
                select(SystemTagVO_.uuid).findValue();
        if (tagUuid == null) {
            return null;
        }

        return tagMgr.updateSystemTag(tagUuid, newTag);
    }

    @Override
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
}
