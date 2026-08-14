package org.zstack.billing;

/**
 * Created by lining on 2019/4/17.
 */
public interface ResourceCreateUsageExtensionPoint {
    Usage makeUsage(Usage usage);
}
