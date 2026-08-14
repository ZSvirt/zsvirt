package org.zstack.header.image;

/**
 * Created by kayo on 2018/9/12.
 */
public interface CreateTemplateExtensionPoint {
    void afterCreateTemplate(ImageInventory inv);
}
