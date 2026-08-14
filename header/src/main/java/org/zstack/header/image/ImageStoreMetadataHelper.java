package org.zstack.header.image;

import org.apache.commons.lang.StringUtils;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * author:kaicai.hu
 * Date:2020/2/19
 */
public class ImageStoreMetadataHelper {
    private String exportMd5Sum;
    private String exportUrl;

    public String getExportMd5Sum() {
        return exportMd5Sum;
    }

    public void setExportMd5Sum(String exportMd5Sum) {
        this.exportMd5Sum = exportMd5Sum;
    }

    public String getExportUrl() {
        return exportUrl;
    }

    public void setExportUrl(String exportUrl) {
        this.exportUrl = exportUrl;
    }

    public static ImageInventory buildImageInventoryFromMetadata(String metadata) {
        ImageInventory imageInventory = JSONObjectUtil.toObject(metadata, ImageInventory.class);
        ImageStoreMetadataHelper imageStoreMetadataHelper = JSONObjectUtil.toObject(metadata, ImageStoreMetadataHelper.class);
        if (StringUtils.isNotEmpty(imageStoreMetadataHelper.getExportMd5Sum())) {
            imageInventory.getBackupStorageRefs().forEach(ref -> ref.setExportMd5Sum(imageStoreMetadataHelper.getExportMd5Sum()));
        }
        if (StringUtils.isNotEmpty(imageStoreMetadataHelper.getExportUrl())) {
            imageInventory.getBackupStorageRefs().forEach(ref -> ref.setExportUrl(imageStoreMetadataHelper.getExportUrl()));
        }

        if (imageInventory.getArchitecture() == null) {
            imageInventory.setArchitecture(ImageArchitecture.defaultArch());
        }

        ImageHelper.updateImageIfVirtioIsNull(imageInventory);

        return imageInventory;
    }
}
