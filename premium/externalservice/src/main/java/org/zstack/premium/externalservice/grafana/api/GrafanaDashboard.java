package org.zstack.premium.externalservice.grafana.api;

import org.zstack.core.Platform;
import org.zstack.header.errorcode.OperationFailureException;

/**
 * Created by mingjian.deng on 2019/8/22.
 */
public class GrafanaDashboard {
    public static String getFolderId() {
        GrafanaDBResult r = GrafanaDB.execute(String.format("select id from dashboard where org_id=%s and folder_id=%s and title=\"%s\"", GrafanaDB.orgId, GrafanaDB.folderId, GrafanaDB.folderTitle));
        if (!r.success) {
            throw new OperationFailureException(r.error);
        }
        return r.result;
    }

    public static String getDashboardId(String title) {
        String folderId = getFolderId();
        if (folderId.isEmpty()) {
            throw new OperationFailureException(Platform.operr("cannot find folder: %s in dashboard", GrafanaDB.folderTitle));
        }
        GrafanaDBResult r = GrafanaDB.execute(String.format("select id from dashboard where org_id=%s and folder_id=%s and title=\"%s\"", GrafanaDB.orgId, folderId, title));
        if (!r.success) {
            throw new OperationFailureException(r.error);
        }

        return r.result;
    }

    public static String getDashboardUid(String title) {
        String folderId = getFolderId();
        if (folderId.isEmpty()) {
            throw new OperationFailureException(Platform.operr("cannot find folder: %s in dashboard", GrafanaDB.folderTitle));
        }
        GrafanaDBResult r = GrafanaDB.execute(String.format("select uid from dashboard where org_id=%s and folder_id=%s and title=\"%s\"", GrafanaDB.orgId, folderId, title));
        if (!r.success) {
            throw new OperationFailureException(r.error);
        }

        return r.result;
    }
}
