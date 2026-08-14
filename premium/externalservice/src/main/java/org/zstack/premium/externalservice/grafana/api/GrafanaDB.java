package org.zstack.premium.externalservice.grafana.api;

import org.zstack.core.Platform;
import org.zstack.premium.externalservice.grafana.GrafanaGlobalProperty;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;

/**
 * Created by mingjian.deng on 2019/8/22.
 */
public class GrafanaDB {
    public static String orgId = "1";
    public static String folderTitle = "zstack";
    public static String folderId = "0";
    public static String MysqlDataSourceName = "MySQL-ZStack";
    public static String PrometheusDatasourceName = "Prometheus-ZStack";


    public static GrafanaDBResult execute(String sql) {
        ShellResult r = ShellUtils.runAndReturn(String.format("/usr/bin/sqlite3 %s '%s'", GrafanaGlobalProperty.GRAFANA_DATA_PATH, sql));
        if (r.isReturnCode(0)) {
            return new GrafanaDBResult(r.getStdout().trim());
        } else {
            return new GrafanaDBResult(Platform.operr("sqlite3 execute failed, because: %s", r.getStderr()));
        }
    }
}
