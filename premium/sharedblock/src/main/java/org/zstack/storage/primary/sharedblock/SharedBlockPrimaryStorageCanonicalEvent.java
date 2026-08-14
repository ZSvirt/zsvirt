package org.zstack.storage.primary.sharedblock;


public class SharedBlockPrimaryStorageCanonicalEvent {
    public static final String UPDATE_ACTIVATE_VOLUME_GC_PATH = "/sblkprimaryStorage/reload/activate/volume/gc";

    public static class UpdateActivateVolumeGC {
        private String action;
        private String gcUuid;
        private String context;

        public String getGcUuid() {
            return gcUuid;
        }

        public void setGcUuid(String gcUuid) {
            this.gcUuid = gcUuid;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }
    }
}
