package org.zstack.header.image;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by MaJin on 2020/1/15.
 */
public class ImageCanonicalEvents {
    public static String IMAGE_TRACK_RESULT_PATH = "/image/track/result";

    public static class ImageTrackData {
        private boolean success = true;
        private ErrorCode error;
        private String uuid;
        private ImageInventory inventory;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public ErrorCode getError() {
            return error;
        }

        public void setError(ErrorCode error) {
            this.error = error;
            if (error != null) {
                this.success = false;
            }
        }

        public ImageInventory getInventory() {
            return inventory;
        }

        public void setInventory(ImageInventory inventory) {
            this.inventory = inventory;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }
}
