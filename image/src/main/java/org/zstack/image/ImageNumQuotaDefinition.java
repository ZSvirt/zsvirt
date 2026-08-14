package org.zstack.image;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.image.ImageVO;
import org.zstack.identity.ResourceHelper;

public class ImageNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return ImageQuotaConstant.IMAGE_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return ImageQuotaGlobalConfig.IMAGE_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(ImageVO.class, accountUuid);
    }
}
