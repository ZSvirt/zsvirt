package org.zstack.premium.externalservice.prometheus;

public class PrometheusConstant {
    public static final String PROMETHEUS_2_BIN_PATH ;
    public static final String PUSHGATEWAY_BIN_PATH ;
    public static final String COLLECTD_RULE_NAME = "collectd.rule.yml";
    public static final String ZWATCH_RULE_NAME = "zwatch.rule.yml";
    public static final String SERVICE_FILE_PATH = "/lib/systemd/system/prometheus.service";
    public static final String SERVICE2_FILE_PATH = "/lib/systemd/system/prometheus2.service";
    
    static {
        if (System.getProperty("os.arch").equals("aarch64")) {
            PROMETHEUS_2_BIN_PATH = "tools/prometheus2_aarch64";
            PUSHGATEWAY_BIN_PATH = "tools/pushgateway_aarch64";
        } else if (System.getProperty("os.arch").equals("mips64el")){
            PROMETHEUS_2_BIN_PATH = "tools/prometheus2_mips64el";
            PUSHGATEWAY_BIN_PATH = "tools/pushgateway_mips64el";
        } else if (System.getProperty("os.arch").equals("loongarch64")){
            PROMETHEUS_2_BIN_PATH = "tools/prometheus2_loongarch64";
            PUSHGATEWAY_BIN_PATH = "tools/pushgateway_loongarch64";
        } else {
            PROMETHEUS_2_BIN_PATH = "tools/prometheus2";
            PUSHGATEWAY_BIN_PATH = "tools/pushgateway";
        }
    }
}
