package org.zstack.tag;

import org.zstack.header.tag.SystemTagInventoryDefinition;
import org.zstack.header.tag.SystemTagLifeCycleListener;
import org.zstack.header.tag.SystemTagOperationJudger;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vo.ResourceVODefinition;
import org.zstack.utils.Tags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.StringDSL.s;

/**
 * Definition-layer system tag. Persistence / DB APIs are implemented by {@code SystemTagRuntime} in cores/tag.
 */
public class SystemTag {
    private static final CLogger logger = Utils.getLogger(SystemTag.class);

    public static final String EPHEMERAL_TAG_PREFIX = "ephemeral";

    public final String tagFormat;
    public final Class<?> resourceClass;
    public final boolean hasPattern;
    protected List<SystemTagValidator> validators = new ArrayList<>();
    protected List<SystemTagLifeCycleListener> lifeCycleListeners = new ArrayList<>();
    protected List<SystemTagOperationJudger> judgers = new ArrayList<>();

    /**
     * may use in SystemTag[hasPattern == true]
     */
    public SensitiveTag annotation;

    private boolean ephemeral = false;
    private boolean adminOnly = false;
    private boolean cloneable = true;
    private boolean sensitive = false;

    /**
     * TODO: resourceClass should be a class of ResourceVODefinition
     * but there is invalid usage in BillingSystemTags
     */
    public SystemTag(String tagFormat, Class<?> resourceClass) {
        this.tagFormat = tagFormat;
        this.resourceClass = resourceClass;
        this.hasPattern = checkIfHasPattern(tagFormat);
    }

    private boolean checkIfHasPattern(String tagFormat) {
        return Arrays.stream(Tags.split(tagFormat))
                .anyMatch(fmtKey -> fmtKey.startsWith("{") && fmtKey.endsWith("}"));
    }

    public static Class<?> resourceTypeForEphemeralTag() {
        return ResourceVODefinition.NopeResource.class;
    }

    public static SystemTag makeEphemeralTag(String tagScript) {
        String format = Tags.prefix(EPHEMERAL_TAG_PREFIX) + tagScript;
        return new SystemTag(format, resourceTypeForEphemeralTag()).markAsEphemeral();
    }

    public enum SystemTagOperation {
        CREATE,
        UPDATE,
        DELETE
    }

    public boolean hasPattern() {
        return hasPattern;
    }

    public SystemTag installLifeCycleListener(SystemTagLifeCycleListener listener) {
        lifeCycleListeners.add(listener);
        return this;
    }

    public SystemTag installJudger(SystemTagOperationJudger judger) {
        judgers.add(judger);
        return this;
    }

    void callCreatedJudger(SystemTagInventoryDefinition tag) {
        for (SystemTagOperationJudger j : judgers) {
            j.tagPreCreated(tag);
        }
    }

    void callTagCreatedListener(SystemTagInventoryDefinition tag) {
        for (SystemTagLifeCycleListener ext : lifeCycleListeners) {
            ext.tagCreated(tag);
        }
    }

    void callDeletedJudger(SystemTagInventoryDefinition tag) {
        for (SystemTagOperationJudger j : judgers) {
            j.tagPreDeleted(tag);
        }
    }

    void callTagDeletedListener(SystemTagInventoryDefinition tag) {
        for (SystemTagLifeCycleListener ext : lifeCycleListeners) {
            ext.tagDeleted(tag);
        }
    }

    void callUpdatedJudger(SystemTagInventoryDefinition old, SystemTagInventoryDefinition newTag) {
        for (SystemTagOperationJudger j : judgers) {
            j.tagPreUpdated(old, newTag);
        }
    }

    void callTagUpdatedListener(SystemTagInventoryDefinition old, SystemTagInventoryDefinition newTag) {
        for (SystemTagLifeCycleListener ext : lifeCycleListeners) {
            ext.tagUpdated(old, newTag);
        }
    }

    protected String useTagFormat() {
        return hasPattern ? Tags.tagPatternToSqlPattern(tagFormat) : tagFormat;
    }

    /**
     * Whether DB match uses SQL LIKE (pattern tags) or EQ (exact tags).
     */
    protected boolean useLikeOp() {
        return hasPattern;
    }

    public String getTagFormat() {
        return tagFormat;
    }

    public Class<?> getResourceClass() {
        return resourceClass;
    }

    public String instantiateTag(Map tokens) {
        if (hasPattern) {
            return s(tagFormat).formatByMap(tokens);
        }
        return tagFormat;
    }

    public Map<String, String> getTokensByTag(String tag) {
        if (!hasPattern) {
            return null;
        }
        return Tags.parseIfMatch(tagFormat, tag);
    }

    public String getTokenByTag(String tag, String tokenName) {
        Map<String, String> tokens = getTokensByTag(tag);
        if (tokens == null) {
            return null;
        }
        return tokens.get(tokenName);
    }

    public String hideSensitiveInfo(String tag) {
        if (!hasPattern || annotation == null) {
            return tag;
        }
        Class<? extends SensitiveTagOutputHandler> clz = annotation.customizeOutput();
        String result = tag;
        try {
            SensitiveTagOutputHandler sensitiveOutputHandler = clz.newInstance();
            result = sensitiveOutputHandler.desensitizeTag(this, tag);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.warn("exception happened:", e);
        }
        return result;
    }

    public SystemTagCreator newSystemTagCreator(String resUuid) {
        throw notRuntime("newSystemTagCreator");
    }

    public SystemTag installValidator(SystemTagValidator validator) {
        validators.add(validator);
        return this;
    }

    public boolean isMatch(String tag) {
        if (hasPattern) {
            return Tags.isMatch(tagFormat, tag);
        }
        return tagFormat.equals(tag);
    }

    void validate(String resourceUuid, Class resourceType, String tag) {
        for (SystemTagValidator validator : validators) {
            validator.validateSystemTag(resourceUuid, resourceType, tag);
        }
    }

    public SystemTag asRuntime() {
        throw notRuntime("asRuntime");
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

    // ---- runtime stubs (implemented by SystemTagRuntime) ----

    public boolean hasTag(String resourceUuid) {
        throw notRuntime("hasTag");
    }

    public boolean hasTag(String resourceUuid, Class resourceClass) {
        throw notRuntime("hasTag");
    }

    public List<String> filterResourceHasTag(Collection<String> resourceUuids) {
        throw notRuntime("filterResourceHasTag");
    }

    public void copy(String srcUuid, Class srcClass, String dstUuid, Class dstClass) {
        throw notRuntime("copy");
    }

    public List<String> getTags(String resourceUuid, Class resourceClass) {
        throw notRuntime("getTags");
    }

    public List<String> getTags(String resourceUuid) {
        throw notRuntime("getTags");
    }

    public String getTag(String resourceUuid, Class resourceClass) {
        throw notRuntime("getTag");
    }

    public String getTag(String resourceUuid) {
        throw notRuntime("getTag");
    }

    public Map<String, List<String>> getTags(Collection<String> resourceUuids, Class resourceClass) {
        throw notRuntime("getTags");
    }

    public Map<String, List<String>> getTags(Collection<String> resourceUuids) {
        throw notRuntime("getTags");
    }

    public boolean delete(String resourceUuid, Class resourceClass) {
        throw notRuntime("delete");
    }

    public boolean delete(String resourceUuid) {
        throw notRuntime("delete");
    }

    public boolean deleteInherentTag(String resourceUuid) {
        throw notRuntime("deleteInherentTag");
    }

    public boolean deleteInherentTag(String resourceUuid, Class resourceClass) {
        throw notRuntime("deleteInherentTag");
    }

    public SystemTagInventoryDefinition updateByTagUuid(String tagUuid, String newTag) {
        throw notRuntime("updateByTagUuid");
    }

    public SystemTagInventoryDefinition updateUnique(String resourceUuid, String oldTag, String newTag) {
        throw notRuntime("updateUnique");
    }

    public SystemTagInventoryDefinition update(String resourceUuid, String newTag) {
        throw notRuntime("update");
    }

    public Map<String, String> getTokensByResourceUuid(String resourceUuid, Class resourceClass) {
        throw notRuntime("getTokensByResourceUuid");
    }

    public Map<String, String> getTokensByResourceUuid(String resourceUuid) {
        throw notRuntime("getTokensByResourceUuid");
    }

    public List<Map<String, String>> getTokensOfTagsByResourceUuid(String resourceUuid) {
        throw notRuntime("getTokensOfTagsByResourceUuid");
    }

    public List<Map<String, String>> getTokensOfTagsByResourceUuid(String resourceUuid, Class resourceClass) {
        throw notRuntime("getTokensOfTagsByResourceUuid");
    }

    public String getTokenByResourceUuid(String resourceUuid, Class resourceClass, String tokenName) {
        throw notRuntime("getTokenByResourceUuid");
    }

    public String getTokenByResourceUuid(String resourceUuid, String tokenName) {
        throw notRuntime("getTokenByResourceUuid");
    }

    public SystemTagInventoryDefinition getTagInventory(String resourceUuid) {
        throw notRuntime("getTagInventory");
    }

    public List<? extends SystemTagInventoryDefinition> getTagInventories(List<String> resourceUuids) {
        throw notRuntime("getTagInventories");
    }

    public List<? extends SystemTagInventoryDefinition> getTagInventories(String resourceUuid) {
        throw notRuntime("getTagInventories");
    }

    public void copyTagInventories(String srcUuid, Class srcResourceClass, String dstUuid, Class dstResourceClass, boolean inherent) {
        throw notRuntime("copyTagInventories");
    }

    public boolean updateTagByToken(String resourceUuid, String tokenName, String newTag) {
        throw notRuntime("updateTagByToken");
    }

    public boolean delete(String resourceUuid, String tagFormat) {
        throw notRuntime("delete");
    }

    public boolean deleteInherentTag(String resourceUuid, String tagFormat) {
        throw notRuntime("deleteInherentTag");
    }

    protected static IllegalStateException notRuntime(String method) {
        return new IllegalStateException(String.format(
                "SystemTag.%s requires TagManager init (SystemTagRuntime); definition-only instance cannot persist/query",
                method));
    }
}
