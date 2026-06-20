package org.zstack.storage.primary.imagestore;

import org.zstack.header.core.Completion;
import org.zstack.header.storage.primary.imagestore.ImageStoreDownloadContext;
import org.zstack.header.storage.primary.imagestore.ImageStoreDownloadExtension;
import org.zstack.header.storage.primary.imagestore.ceph.CephImageStoreDownloadContext;

public class ImageStoreDownloadExtensions {
    private ImageStoreDownloadExtensions() {
    }

    public static void afterDownload(ImageStoreDownloadExtension extension,
                                     ImageStoreDownloadContext context,
                                     Completion completion) {
        if (extension == null) {
            completion.success();
            return;
        }
        extension.afterDownload(context, completion);
    }

    public static boolean supports(ImageStoreDownloadExtension extension, CephImageStoreDownloadContext context) {
        return extension != null && extension.supports(context);
    }
}
