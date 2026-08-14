package org.zstack.premium.externalservice.grafana.api;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 2019/8/22.
 */
public class GrafanaDatasource {
    static CLogger logger = Utils.getLogger(GrafanaDatasource.class);

    public static String getPrometheusDatasourceId() {
        GrafanaDBResult r = GrafanaDB.execute(String.format("select id from data_source where org_id=%s and name=\"%s\"", GrafanaDB.orgId, GrafanaDB.PrometheusDatasourceName));
        if (!r.success) {
            throw new OperationFailureException(r.error);
        }

        return r.result;
    }

    public static boolean setPrometheusDatasourceUrl(String newUrl) {
        String id = getPrometheusDatasourceId();
        if (id.isEmpty()) {
            logger.warn(String.format("cannot find '%s' in sqlite data_source", GrafanaDB.PrometheusDatasourceName));
            return false;
        }
        GrafanaDBResult r = GrafanaDB.execute(String.format("update data_source set url = \"%s\" where id=%s", newUrl, id));
        if (!r.success) {
            throw new OperationFailureException(r.error);
        }
        return true;
    }
}
