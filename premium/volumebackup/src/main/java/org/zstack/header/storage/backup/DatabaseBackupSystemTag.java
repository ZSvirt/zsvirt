package org.zstack.header.storage.backup;

import org.zstack.header.storage.database.backup.DatabaseBackupVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Created by MaJin on 2019/3/13.
 */
@TagDefinition
public class DatabaseBackupSystemTag {

    public static final String API_ID_TOKEN = "apiId";
    public static SystemTag MANUAL_CREATE_RESOURCE = new SystemTag(
            String.format("manualCreate::api::{%s}", API_ID_TOKEN),
            DatabaseBackupVO.class
    );
}
