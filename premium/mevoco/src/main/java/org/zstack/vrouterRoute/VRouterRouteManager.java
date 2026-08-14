package org.zstack.vrouterRoute;

import java.util.List;

/**
 * Created by weiwang on 15/06/2017.
 */
public interface VRouterRouteManager {
    void attachRouterTableToVRouter(String tableUuid, String vrUuid);
    void detachRouterTableFromVRouter(String tableUuid, String vrUuid);
    List<String> getVrUuidsFromTableUuid(String tableUuid);
    List<String> getTableUuidsFromVrUuid(String vrUuid);
}
