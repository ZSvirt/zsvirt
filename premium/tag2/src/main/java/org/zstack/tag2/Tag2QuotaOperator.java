package org.zstack.tag2;

import org.springframework.transaction.annotation.Transactional;
import org.zstack.header.identity.APIChangeResourceOwnerMsg;
import org.zstack.header.identity.Quota;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.tag.TagPatternVO;
import org.zstack.identity.QuotaUtil;
import org.zstack.identity.ResourceHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Tag2QuotaOperator implements Quota.QuotaOperator {
    @Override
    public void checkQuota(APIMessage msg, Map<String, Quota.QuotaPair> pairs) {
        if (msg instanceof APIChangeResourceOwnerMsg) {
            check((APIChangeResourceOwnerMsg) msg, pairs);
        } else if (msg instanceof APICreateTagMsg) {
            check((APICreateTagMsg) msg, pairs);
        }
    }

    @Override
    public void checkQuota(NeedQuotaCheckMessage msg, Map<String, Quota.QuotaPair> pairs) {

    }

    @Override
    public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
        Quota.QuotaUsage usage = new Quota.QuotaUsage();
        usage.setName(Tag2QuotaConstant.TAG_PATTERN_TOTAL_NUM);
        usage.setUsed(getUsedTagPattern(accountUuid));
        return Collections.singletonList(usage);
    }

    private void check(APIChangeResourceOwnerMsg msg, Map<String, Quota.QuotaPair> pairs) {
        String resourceTargetOwnerAccountUuid = msg.getAccountUuid();
        if (new QuotaUtil().isAdminAccount(resourceTargetOwnerAccountUuid)) {
            return;
        }

        String resourceType = new QuotaUtil().getResourceType(msg.getResourceUuid());

        if (TagPatternVO.class.getSimpleName().equals(resourceType)) {
            checkTag(resourceTargetOwnerAccountUuid, pairs);
        }
    }

    private void check(APICreateTagMsg msg, Map<String, Quota.QuotaPair> pairs) {
        checkTag(msg.getSession().getAccountUuid(), pairs);
    }

    private void checkTag(String accountUuid, Map<String, Quota.QuotaPair> pairs) {
        long tagNumQuota = pairs.get(Tag2QuotaConstant.TAG_PATTERN_TOTAL_NUM).getValue();
        long askedTagNum = 1;

        QuotaUtil.QuotaCompareInfo info = new QuotaUtil.QuotaCompareInfo();
        info.resourceTargetOwnerAccountUuid = accountUuid;
        info.quotaName = Tag2QuotaConstant.TAG_PATTERN_TOTAL_NUM;
        info.quotaValue = tagNumQuota;
        info.currentUsed = getUsedTagPattern(accountUuid);
        info.request = askedTagNum;
        new QuotaUtil().CheckQuota(info);
    }

    @Transactional(readOnly = true)
    private long getUsedTagPattern(String accountUuid){
        return ResourceHelper.countOwnResources(TagPatternVO.class, accountUuid);
    }
}
