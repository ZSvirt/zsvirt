package org.zstack.premium.externalservice.grafana.api;

/**
 * Created by mingjian.deng on 2019/8/22.
 */
public class GrafanaAPICommands {
    public class GrafanaLookupUserResult extends GrafanaAPIResult {
        public int id;
        public String login;
        public int orgId;
        public boolean isGrafanaAdmin;
    }

    public class GrafanaResetPasswordResult extends GrafanaAPIResult {
        public String message;
    }

    public class DataSourceAPICmd {
        public int id;
        public int orgId;
        public String name;
        public String type;
        public String access;
        public String url;
        public String password;
        public String user;
        public String database;
        public boolean basicAuth;
        public String basicAuthUser;
        public String basicAuthPassword;
        public boolean isDefault;
        public Object jsonData;
    }

    public class DataSourceAPIResult extends GrafanaAPIResult {
        public int id;
        public int orgId;
        public String name;
        public String type;
        public String access;
        public String url;
        public String password;
        public String user;
        public String database;
        public boolean basicAuth;
        public String basicAuthUser;
        public String basicAuthPassword;
        public boolean isDefault;
        public Object jsonData;
    }

    public class DataSourceNameAPIResult extends GrafanaAPIResult {
        public int id;
    }
}
