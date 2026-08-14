package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

public class DownloadVolumeTemplateToPrimaryStorageReply extends MessageReply {
    private ImageCacheInventory imageCache;

    public ImageCacheInventory getImageCache() {
        return imageCache;
    }

    public void setImageCache(ImageCacheInventory imageCache) {
        this.imageCache = imageCache;
    }
}
