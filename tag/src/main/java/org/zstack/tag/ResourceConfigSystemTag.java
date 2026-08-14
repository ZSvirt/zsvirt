package org.zstack.tag;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.TagConstant;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.TagUtils;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ResourceConfigSystemTag extends PatternedSystemTag {
    @Autowired
    private ResourceConfigFacade rcf;

    public ResourceConfigSystemTag() {
        super(String.format("%s::{%s}::{%s}::{%s}",
                TagConstant.RESOURCE_CONFIG_TAG_PREFIX,
                TagConstant.RESOURCE_CONFIG_CATEGORY_TOKEN,
                TagConstant.RESOURCE_CONFIG_NAME_TOKEN,
                TagConstant.RESOURCE_CONFIG_VALUE_TOKEN), SystemTagVO.class);
        this.markAsEphemeral();
    }

    @Override
    public SystemTagCreator newSystemTagCreator(String resourceUuid) {
        throw new CloudRuntimeException("ResourceConfigSystemTag do not support SystemTagCreator");
    }

    @Override
    public boolean isMatch(String tag) {
        return TagUtils.isMatch(tagFormat, tag) && tag.startsWith(TagConstant.RESOURCE_CONFIG_TAG_PREFIX);
    }

    public void validateResourceConfig(String tag) {
        Map<String, String> tokens = this.getTokensByTag(tag);
        ResourceConfig resourceConfig = rcf.getResourceConfig(String.format("%s.%s",
                tokens.get(TagConstant.RESOURCE_CONFIG_CATEGORY_TOKEN),
                tokens.get(TagConstant.RESOURCE_CONFIG_NAME_TOKEN)));

        resourceConfig.validateOnly(tokens.get(TagConstant.RESOURCE_CONFIG_VALUE_TOKEN));
    }

    public void newResourceConfig(String resourceUuid, String tag) {
        Map<String, String> tokens = this.getTokensByTag(tag);
        ResourceConfig resourceConfig = rcf.getResourceConfig(String.format("%s.%s",
                tokens.get(TagConstant.RESOURCE_CONFIG_CATEGORY_TOKEN),
                tokens.get(TagConstant.RESOURCE_CONFIG_NAME_TOKEN)));
        resourceConfig.updateValue(resourceUuid, tokens.get(TagConstant.RESOURCE_CONFIG_VALUE_TOKEN));
    }

    public static String buildResourceConfig(String category, String name, String value) {
        return String.format("%s::%s::%s::%s", TagConstant.RESOURCE_CONFIG_TAG_PREFIX, category, name, value);
    }
}
