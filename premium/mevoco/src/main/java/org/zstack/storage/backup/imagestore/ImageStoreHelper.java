package org.zstack.storage.backup.imagestore;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.image.ImageHelper;

import static org.zstack.header.image.ImageConstant.EXPORTED_IMAGE_PREFIX;

public class ImageStoreHelper {
    public static class ImageStoreExportUrl extends ImageHelper.ExportUrl {
        @Override
        public String removeNameFromExportUrl(String exportUrl) {
            String image = StringUtils.substringAfterLast(exportUrl, "/");
            return exportUrl.replace(image, String.format("%s%s",
                    EXPORTED_IMAGE_PREFIX, StringUtils.substringAfterLast(exportUrl, EXPORTED_IMAGE_PREFIX)));
        }
    }
}
