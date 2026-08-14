package org.zstack.header.identity;

import java.util.Map;
import java.util.Set;

/**
 * Created by miao on 1/9/2016.
 */
public interface RegisterQuotaCheckerExtensionPoint {
    Map<String, Set<Quota.QuotaValidator>> registerQuotaValidator();
}
