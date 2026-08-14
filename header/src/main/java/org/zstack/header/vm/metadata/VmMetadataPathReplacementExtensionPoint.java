package org.zstack.header.vm.metadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface VmMetadataPathReplacementExtensionPoint {
    String getPrimaryStorageType();
    PathReplacementResult calculatePathReplacements(String targetPsUuid, List<String> allOldPaths);
    class PathReplacementResult {
        private Map<String, String> metadataToCurrentPathMap = new HashMap<>();
        private String oldPrefix;
        private String newPrefix;

        public PathReplacementResult() {
        }

        public Map<String, String> getMetadataToCurrentPathMap() {
            return metadataToCurrentPathMap;
        }

        public void setMetadataToCurrentPathMap(Map<String, String> metadataToCurrentPathMap) {
            this.metadataToCurrentPathMap = metadataToCurrentPathMap;
        }

        public String getOldPrefix() {
            return oldPrefix;
        }

        public void setOldPrefix(String oldPrefix) {
            this.oldPrefix = oldPrefix;
        }

        public String getNewPrefix() {
            return newPrefix;
        }

        public void setNewPrefix(String newPrefix) {
            this.newPrefix = newPrefix;
        }
    }
}
