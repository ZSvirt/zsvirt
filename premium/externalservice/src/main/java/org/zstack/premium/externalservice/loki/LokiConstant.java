package org.zstack.premium.externalservice.loki;

/**
 * Created by mingjian.deng on 2019/9/6.
 */
public class LokiConstant {
    public static final String LOKI_SERVICE_FILE_PATH = "/lib/systemd/system/loki-server.service";
    public static final String PROMTAIL_SERVICE_FILE_PATH = "/lib/systemd/system/promtail-server.service";
    public static final String LOKI_BIN_PATH ;
    public static final String LOKI_CONFIG_NAME;
    public static final String PROMTAIL_BIN_PATH ;
    public static final String PROMTAIL_BIN_SRC_PATH ;
    public static final String PROMTAIL_CONFIG_NAME = "/var/lib/zstack/loki/promtail-config.yaml";
    public static final String InitialLokiConfig = "grafanaData/loki_030_config";
    public static final String InitialPromtailConfig = "grafanaData/promtail_030_config";

    static {
        LOKI_CONFIG_NAME = "tools/loki-config.yaml";

        if (System.getProperty("os.arch").equals("aarch64")) {
            LOKI_BIN_PATH = "/var/lib/zstack/loki/loki_aarch64";
            PROMTAIL_BIN_SRC_PATH = "promtail_aarch64";
            PROMTAIL_BIN_PATH = "/var/lib/zstack/loki/promtail_aarch64";
        } else {
            LOKI_BIN_PATH = "tools/loki";
            PROMTAIL_BIN_SRC_PATH = "promtail";
            PROMTAIL_BIN_PATH = "/var/lib/zstack/loki/promtail";
        }
    }
}
