package org.zstack.core.cascade;

/**
 * Created by xing5 on 2016/11/7.
 */
public interface CascadeAddOnExtensionPoint {
    CascadeExtensionPoint cascadeAddOn(String resourceName);
}
