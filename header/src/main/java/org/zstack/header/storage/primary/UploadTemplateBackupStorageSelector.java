package org.zstack.header.storage.primary;

import java.util.Map;

/**
 * Created by xing5 on 2016/4/29.
 */
public interface UploadTemplateBackupStorageSelector {
    // key = backup storage uuid, value = backup storage install path
    Map<String, String> select(long templateActualSize);
}
