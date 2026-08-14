package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.AddImageExtensionPoint;
import org.zstack.header.image.CreateImageExtensionPoint;
import org.zstack.header.image.CreateTemplateExtensionPoint;
import org.zstack.header.image.ImageInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.ForEachFunction;

import java.util.List;

/**
 * Created by MaJin on 2021/3/17.
 */
public class ImageExtensionPointEmitter implements Component {
    @Autowired
    private PluginRegistry pluginRgty;

    private List<CreateTemplateExtensionPoint> createTemplateExtensionPoints;
    private List<CreateImageExtensionPoint> createImageExtensionPoints;
    private List<AddImageExtensionPoint> addImageExtensionPoints;

    @Override
    public boolean start() {
        createTemplateExtensionPoints = pluginRgty.getExtensionList(CreateTemplateExtensionPoint.class);
        createImageExtensionPoints = pluginRgty.getExtensionList(CreateImageExtensionPoint.class);
        addImageExtensionPoints = pluginRgty.getExtensionList(AddImageExtensionPoint.class);
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    public void beforeCreateImage(ImageInventory image, String bsUuid, String psUuid) {
        for (CreateImageExtensionPoint ext : createImageExtensionPoints) {
            ext.beforeCreateImage(image, bsUuid, psUuid);
        }
    }

    public void afterCreateImage(ImageInventory image) {
        CollectionUtils.safeForEach(createTemplateExtensionPoints, ext -> ext.afterCreateTemplate(image));
    }

    public void preAddImage(ImageInventory inv) {
        addImageExtensionPoints.forEach(ext -> ext.preAddImage(inv));
    }

    public void beforeAddImage(ImageInventory inv) {
        CollectionUtils.safeForEach(addImageExtensionPoints, ext -> ext.beforeAddImage(inv));
    }

    public void afterAddImage(ImageInventory inv) {
        CollectionUtils.safeForEach(addImageExtensionPoints, ext -> ext.afterAddImage(inv));
    }

    public void failedToAddImage(ImageInventory inv, ErrorCode err) {
        CollectionUtils.safeForEach(addImageExtensionPoints, ext -> ext.failedToAddImage(inv, err));
    }
}
