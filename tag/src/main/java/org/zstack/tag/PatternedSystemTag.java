package org.zstack.tag;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.TagConstant;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.StringDSL.s;

/**
 */
public class PatternedSystemTag extends SystemTag {
    private static final CLogger logger = Utils.getLogger(SystemTag.class);

    public SensitiveTag annotation;

    public PatternedSystemTag(String tagFormat, Class resourceClass) {
        super(tagFormat, resourceClass);
    }

    public static PatternedSystemTag makeEphemeralTag(String tagFormatWithoutPrefix) {
        String tagFormat = String.format("%s::%s", TagConstant.EPHEMERAL_TAG_PREFIX, tagFormatWithoutPrefix);
        PatternedSystemTag tag = new PatternedSystemTag(tagFormat, SystemTagVO.class);
        tag.markAsEphemeral();
        return tag;
    }

    @Override
    protected String useTagFormat() {
        return TagUtils.tagPatternToSqlPattern(tagFormat);
    }

    @Override
    protected Op useOp() {
        return Op.LIKE;
    }

    @Override
    public boolean isMatch(String tag) {
        return TagUtils.isMatch(tagFormat, tag);
    }

    @Override
    public boolean delete(String resourceUuid, Class resourceClass) {
        return tagMgr.deleteSystemTagUseLike(useTagFormat(), resourceUuid, resourceClass.getSimpleName(), false);
    }

    public boolean delete(String resourceUuid, String tagFormat) {
        return tagMgr.deleteSystemTagUseLike(tagFormat, resourceUuid, resourceClass.getSimpleName(), false);
    }

    @Override
    public boolean delete(String resourceUuid) {
        return tagMgr.deleteSystemTagUseLike(useTagFormat(), resourceUuid, resourceClass.getSimpleName(), false);
    }

    @Override
    public boolean deleteInherentTag(String resourceUuid) {
        return tagMgr.deleteSystemTagUseLike(useTagFormat(), resourceUuid, resourceClass.getSimpleName(), true);
    }

    public boolean deleteInherentTag(String resourceUuid, String tagFormat) {
        return tagMgr.deleteSystemTagUseLike(tagFormat, resourceUuid, resourceClass.getSimpleName(), true);
    }

    @Override
    public boolean deleteInherentTag(String resourceUuid, Class resourceClass) {
        return tagMgr.deleteSystemTagUseLike(useTagFormat(), resourceUuid, resourceClass.getSimpleName(), true);
    }

    public Map<String, String> getTokensByTag(String tag) {
        return TagUtils.parseIfMatch(tagFormat, tag);
    }

    public String getTokenByTag(String tag, String tokenName) {
        return getTokenByTag(tag, tokenName, resourceClass);
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
        int idx = tag.indexOf("::");
        String tagHead = idx > 0 ? tag.substring(0, idx) : tokenName;
        return tagMgr.decryptTokenValue(resourceClass.getSimpleName(), tagHead, tokenName, value);
    }

    private Map<String, String> getDecryptedTokensByTag(String tag, Class resourceClass) {
        Map<String, String> tokens = getTokensByTag(tag);
        if (tokens == null || tagMgr == null) {
            return tokens;
        }

        int idx = tag.indexOf("::");
        String tagHead = idx > 0 ? tag.substring(0, idx) : null;
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

    public Map<String, String> getTokensByResourceUuid(String resourceUuid, Class resourceClass) {
        String tag = getTag(resourceUuid, resourceClass);
        if (tag == null) {
            return null;
        }

        return getDecryptedTokensByTag(tag, resourceClass);
    }

    public List<Map<String, String>> getTokensOfTagsByResourceUuid(String resourceUuid) {
        return getTokensOfTagsByResourceUuid(resourceUuid, resourceClass);
    }

    public List<Map<String, String>> getTokensOfTagsByResourceUuid(String resourceUuid, Class resourceClass) {
        List<Map<String, String>> res = new ArrayList<>();

        List<String> tags = getTags(resourceUuid, resourceClass);
        for (String tag : tags) {
            res.add(getDecryptedTokensByTag(tag, resourceClass));
        }

        return res;
    }

    public Map<String, String> getTokensByResourceUuid(String resourceUuid) {
        return getTokensByResourceUuid(resourceUuid, resourceClass);
    }

    public String getTokenByResourceUuid(String resourceUuid, Class resourceClass, String tokenName) {
        String tag = getTag(resourceUuid, resourceClass);
        if (tag == null) {
            return null;
        }
        return getTokenByTag(tag, tokenName, resourceClass);
    }

    public String getTokenByResourceUuid(String resourceUuid, String tokenName) {
        return getTokenByResourceUuid(resourceUuid, resourceClass, tokenName);
    }

    public String hideSensitiveInfo(String tag) {
        Class<? extends SensitiveTagOutputHandler> clz = this.annotation.customizeOutput();
        String result = tag;
        try {
            SensitiveTagOutputHandler sensitiveOutputHandler = clz.newInstance();
            result = sensitiveOutputHandler.desensitizeTag(this, tag);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.warn("exception happened :", e);
        }
        return result;
    }

    public String instantiateTag(Map tokens) {
        return s(tagFormat).formatByMap(tokens);
    }

    public SystemTagInventory getTagInventory(String resourceUuid) {
        SystemTagVO vo = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, resourceUuid)
                .eq(SystemTagVO_.resourceType, getResourceClass().getSimpleName())
                .like(SystemTagVO_.tag, useTagFormat())
                .find();
        return  vo == null ? null : SystemTagInventory.valueOf(vo);
    }

    public List<SystemTagInventory> getTagInventories(List<String> resourceUuids) {
        List<SystemTagVO> list = Q.New(SystemTagVO.class)
                .in(SystemTagVO_.resourceUuid, resourceUuids)
                .eq(SystemTagVO_.resourceType, getResourceClass().getSimpleName())
                .like(SystemTagVO_.tag, useTagFormat())
                .list();
        return SystemTagInventory.valueOf(list);
    }

    public List<SystemTagInventory> getTagInventories(String resourceUuid) {
        return SystemTagInventory.valueOf(Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceType, getResourceClass().getSimpleName()).
                eq(SystemTagVO_.resourceUuid, resourceUuid).like(SystemTagVO_.tag, useTagFormat()).list());
    }

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

    public boolean updateTagByToken(String resourceUuid, String tokenName, String newTag) {
        SystemTagVO vo = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, resourceUuid)
                .eq(SystemTagVO_.resourceType, getResourceClass().getSimpleName())
                .like(SystemTagVO_.tag, useTagFormat())
                .find();

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
}
