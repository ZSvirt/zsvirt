package org.zstack.cloudformation;

import org.zstack.header.cloudformation.ResourceStackInventory;

import java.util.Map;

/**
 * Created by mingjian.deng on 2019/6/4.
 */
public interface ClousFormationTemplateExtensionPoint {
    String getPreParameters(ResourceStackInventory stack);

    void afterCreateResourceStack(ResourceStackInventory stack, String source);

    void afterGetResourceStackFromResource(Map<String, String> stack);

    void afterDeleteResourceStack(ResourceStackInventory stack);
}
