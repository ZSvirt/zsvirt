package org.zstack.storage.ceph.backup;

import org.zstack.header.image.ImageHelper;

public class CephBackStorageHelper {

    public static class CephBackStorageExportUrl extends ImageHelper.ExportUrl {
        @Override
        public String removeNameFromExportUrl(String exportUrl) {
            return exportUrl;
        }
    }
}
