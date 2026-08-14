package org.zstack.externalbackup;

import org.zstack.core.db.SQL;
import org.zstack.externalbackup.CreateExternalBackupMsg;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by MaJin on 2019/11/29.
 */
public class ExternalBackupContextReloader {
    public static void saveContext(CreateExternalBackupMsg msg) {
        if (msg.getLongJobUuid() != null) {
            SQL.New(LongJobVO.class).eq(LongJobVO_.uuid, msg.getLongJobUuid())
                    .set(LongJobVO_.jobData, JSONObjectUtil.toJsonString(msg))
                    .update();
        }
    }
}
