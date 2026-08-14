package org.zstack.storage.primary.local;

import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.image.ImageInventory;

public class LocalStorageCanonicalEvents {
    public static final String IMAGE_DISTRIBUTED_TO_IMAGE_CACHE = "/localstorage/image/distributed";

    public static class LocalStorageImageDistributedToImageCacheEvent {
        public String hostUuid;
        public String localStorageUuid;
        public ImageInventory image;

        public void setImage(ImageInventory image) {
            this.image = image;
        }

        public ImageInventory getImage() {
            return this.image;
        }

        public void fire() {
            EventFacade evtf = Platform.getComponentLoader().getComponent(EventFacade.class);
            evtf.fire(IMAGE_DISTRIBUTED_TO_IMAGE_CACHE, this);
        }
    }
}
