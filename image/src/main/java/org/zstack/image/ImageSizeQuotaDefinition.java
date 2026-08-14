package org.zstack.image;

import org.zstack.core.db.Q;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;

public class ImageSizeQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return ImageQuotaConstant.IMAGE_SIZE;
    }

    @Override
    public Long getDefaultValue() {
        return ImageQuotaGlobalConfig.IMAGE_SIZE.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        Long imageSize = Q.New(ImageVO.class, AccountResourceRefVO.class)
                .table0()
                    .eq(ImageVO_.uuid).table1(AccountResourceRefVO_.resourceUuid)
                    .selectSum(ImageVO_.actualSize)
                .table1()
                    .eq(AccountResourceRefVO_.accountUuid, accountUuid)
                    .eq(AccountResourceRefVO_.resourceType, ImageVO.class.getSimpleName())
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .find();
        return imageSize == null ? 0L : imageSize;
    }
}
