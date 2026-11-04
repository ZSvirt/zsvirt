package org.zstack.storage.primary;

public class ImageCacheCleanParam {
    public boolean triggerByApi;
    public boolean includeReadyImage;

    public ImageCacheCleanParam() {

    }

    public ImageCacheCleanParam(boolean triggerByApi, boolean includeReadyImage) {
        this.triggerByApi = triggerByApi;
        this.includeReadyImage = includeReadyImage;
    }
}
