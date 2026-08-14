package org.zstack.premium.externalservice.grafana;

import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseGlobalProperty;
import org.zstack.core.externalservice.AbstractLocalExternalService;
import org.zstack.header.core.external.service.ExternalServiceCapabilities;
import org.zstack.core.externalservice.ExternalServiceCapabilitiesBuilder;
import org.zstack.premium.externalservice.grafana.api.*;
import org.zstack.premium.externalservice.prometheus.PrometheusGlobalProperty;
import org.zstack.utils.Bash;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by mingjian.deng on 2019/8/21.
 */
public class GrafanaImpl6 extends AbstractLocalExternalService implements Grafana {
    ExternalServiceCapabilities capabilities = ExternalServiceCapabilitiesBuilder.build()
            .reloadConfig(false);

    @Override
    public String getName() {
        return String.format("grafana6-on-management-node-%s", Platform.getManagementServerIp());
    }

    @Override
    public void start() {
        File dash = PathUtil.findFileOnClassPath(GrafanaConstant.InitialGrafanaData);
        if (dash == null) {
            throw new RuntimeException(String.format("cannot find %s in classpath", GrafanaConstant.InitialGrafanaData));
        }

        if (isAlive()) {
            // update grafana initial panel data
            GrafanaDB.execute(String.format(".read %s", dash.getAbsolutePath()));
            return;
        }

        sysctl("restart");
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            logger.warn(e.getMessage());
            Thread.currentThread().interrupt();
        }
        GrafanaDB.execute(String.format(".read %s", dash.getAbsolutePath()));
    }

    @Override
    public boolean resetMysqlDataSource() {
        String url = getUrl(DatabaseGlobalProperty.DbUrl);
        return resetMysqlDatasource(url, DatabaseGlobalProperty.DbUser, DatabaseGlobalProperty.DbPassword);
    }

    @Override
    public boolean resetPrometheusDataSource() {
        String url = String.format("http://%s:%s", Platform.getManagementServerIp(), PrometheusGlobalProperty.PORT);
        return GrafanaDatasource.setPrometheusDatasourceUrl(url);
    }

    @Override
    public void stop() {
        if (!isAlive()) {
            return;
        }
        sysctl("stop");
    }

    @Override
    public void restart() {
        sysctl("restart");
    }

    private void sysctl(String ctl) {
        new Bash() {
            @Override
            protected void scripts() {
                setE();
                sudoRun("systemctl %s grafana-server", ctl);
            }
        }.execute();
    }

    @Override
    public boolean isAlive() {
        return getPID() != null;
    }

    @Override
    public ExternalServiceCapabilities getExternalServiceCapabilities() {
        return capabilities;
    }

    @Override
    public void reload() {
        // do nothing
    }

    @Override
    protected String[] getCommandLineKeywords() {
        return new String[]{"grafana-server", "--config=", "--pidfile"};
    }

    @Override
    public boolean resetPassword(String oldPassword, String password) {
        GrafanaAPI api = new GrafanaAPI();
        api.setPassword(oldPassword);
        Map<String, Object> params = new HashMap<>();
        params.put("loginOrEmail", "admin");
        GrafanaAPICommands.GrafanaLookupUserResult user = api.call("/users/lookup", "GET", params,
                GrafanaAPICommands.GrafanaLookupUserResult.class);

        if (user.error != null) {
            return false;
        }

        params = new HashMap<>();
        params.put("password", password);
        GrafanaAPICommands.GrafanaResetPasswordResult pass = api.call(String.format("/admin/users/%d/password", user.id), "PUT", params,
                GrafanaAPICommands.GrafanaResetPasswordResult.class);
        if (pass.error != null) {
            return false;
        }

        logger.debug(String.format("reset grafana password to: %s", password));
        return true;
    }

    private boolean resetMysqlDatasource(String url, String username, String password) {
        GrafanaAPI api = new GrafanaAPI();
        api.setPassword(GrafanaGlobalConfig.GRAFANA_ADMIN_PASSWORD.value());

        // search the id by name
        GrafanaAPICommands.DataSourceNameAPIResult datasourceId = api.call(String.format("/datasources/id/%s", GrafanaDB.MysqlDataSourceName), "GET", null,
                GrafanaAPICommands.DataSourceNameAPIResult.class);

        if (datasourceId.error != null || datasourceId.id == 0) {
            return false;
        }

        // get the datasource
        GrafanaAPICommands.DataSourceAPIResult datasource = api.call(String.format("/datasources/%d", datasourceId.id), "GET", null,
                GrafanaAPICommands.DataSourceAPIResult.class);

        if (datasource.error != null || datasource.id == 0) {
            return false;
        }

        // update url / username / password
        datasource.url = url;
        datasource.user = username;
        datasource.password = password;

        GrafanaAPICommands.DataSourceAPICmd param = JSONObjectUtil.rehashObject(datasource, GrafanaAPICommands.DataSourceAPICmd.class);
        GrafanaAPIResult reset = api.call(String.format("/datasources/%d", datasourceId.id), "PUT", JSONObjectUtil.rehashObject(param, Map.class),
                GrafanaAPIResult.class);
        if (reset.error != null) {
            return false;
        }

        return true;
    }

    private String getUrl(String url) {
        String prefix = "jdbc:mysql://";
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        } else {
            return url;
        }
    }
}
