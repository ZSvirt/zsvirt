package org.zstack.identity;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.QuotaVO;

import java.util.Set;

/**
 * Created by Wenhao.Zhang on 21/12/20
 */
public interface QuotaUpdateChecker {
    /**
     * return identity types supported by Quota
     * example: [AccountVO.class.getSimpleName()]
     */
    Set<String> type();

    /**
     * validate the quota and usage before update
     */
    ErrorCode check(QuotaVO quota, long updatedValue);
}
