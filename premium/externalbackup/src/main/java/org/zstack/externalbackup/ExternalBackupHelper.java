package org.zstack.externalbackup;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.jsonlabel.JsonLabelInventory;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

public class ExternalBackupHelper {
    public static void deleteBackupLabel() {
        new JsonLabel().delete(ExternalBackupConstants.EXTERNAL_BACKUP_LABEL);
    }

    public static void markBackupLabel(String backupUuid) {
        new JsonLabel().create(ExternalBackupConstants.EXTERNAL_BACKUP_LABEL, backupUuid);
    }

    public static String getLabelBackupUuid() {
        return Optional.ofNullable(new JsonLabel().get(ExternalBackupConstants.EXTERNAL_BACKUP_LABEL))
                .map(JsonLabelInventory::getLabelValue)
                .orElse(null);
    }
    public static String getSQLToMarkBackupLabel(String backupUuid) {
        return String.format("INSERT INTO `zstack`.`JsonLabelVO` (`labelKey`, `labelValue`, `lastOpDate`, `createDate`) " +
                "VALUES ('externalBackup', '%s', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());", backupUuid);
    }
}
