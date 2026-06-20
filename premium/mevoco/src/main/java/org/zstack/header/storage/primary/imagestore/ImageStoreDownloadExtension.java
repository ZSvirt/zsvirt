package org.zstack.header.storage.primary.imagestore;

import org.zstack.header.core.Completion;
import org.zstack.header.storage.primary.imagestore.ceph.CephImageStoreDownloadContext;

public abstract class ImageStoreDownloadExtension {
    public abstract void afterDownload(ImageStoreDownloadContext context, Completion completion);

    public abstract boolean supports(CephImageStoreDownloadContext context);

    public abstract void download(CephImageStoreDownloadContext context, Completion completion);
}
