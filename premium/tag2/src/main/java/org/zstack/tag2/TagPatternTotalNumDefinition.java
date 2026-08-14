package org.zstack.tag2;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.tag.TagPatternVO;
import org.zstack.identity.ResourceHelper;

public class TagPatternTotalNumDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return Tag2QuotaConstant.TAG_PATTERN_TOTAL_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return Tag2QuotaGlobalConfig.TAG_PATTERN_TOTAL_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(TagPatternVO.class, accountUuid);
    }
}
