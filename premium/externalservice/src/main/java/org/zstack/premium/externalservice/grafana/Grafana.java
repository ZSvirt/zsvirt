package org.zstack.premium.externalservice.grafana;

import org.zstack.core.externalservice.ExternalService;

/**
 * Created by mingjian.deng on 2019/8/21.
 */
public interface Grafana extends ExternalService {
    boolean resetPassword(String oldPassword, String password);
    boolean resetMysqlDataSource();
    boolean resetPrometheusDataSource();
}
