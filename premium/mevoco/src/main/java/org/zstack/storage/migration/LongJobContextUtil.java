package org.zstack.storage.migration;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.utils.gson.JSONObjectUtil;

public class LongJobContextUtil {
    public static int saveContext(String jobUuid, Object ctxObj) {
        String jstr = JSONObjectUtil.toJsonString(ctxObj);
        return SQL.New(LongJobVO.class)
                .eq(LongJobVO_.uuid, jobUuid)
                .set(LongJobVO_.jobResult, jstr)
                .update();
    }

    public static <T> T loadContext(String jobUuid, Class<T> clazz) {
        String jstr = Q.New(LongJobVO.class)
                .eq(LongJobVO_.uuid, jobUuid)
                .select(LongJobVO_.jobResult)
                .findValue();
        if (jstr == null) {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new CloudRuntimeException(ex);
            }
        }

        return JSONObjectUtil.toObject(jstr, clazz);
    }
}
