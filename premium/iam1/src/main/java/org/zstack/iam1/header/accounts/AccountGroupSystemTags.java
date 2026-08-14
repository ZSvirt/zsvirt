package org.zstack.iam1.header.accounts;

import org.zstack.header.tag.TagDefinition;
import org.zstack.iam1.entity.accounts.AccountGroupVO;
import org.zstack.tag.SystemTag;

@TagDefinition
public class AccountGroupSystemTags {
    public static SystemTag GROUP_FOR_NEW_CREATE_ACCOUNT =
            new SystemTag("groupForNewCreateAccount", AccountGroupVO.class);
}
