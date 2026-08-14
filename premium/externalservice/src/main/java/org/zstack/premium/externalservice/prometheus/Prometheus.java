package org.zstack.premium.externalservice.prometheus;

import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.externalservice.ExternalService;
import org.zstack.utils.path.PathUtil;

import java.util.List;
import java.util.Map;

public interface Prometheus extends ExternalService {
    PrometheusConfig getConfig();

    PrometheusParam getParams();

    void rewriteConfigAndReload();

    void restart();

    void ctlstop();

    /**
     * try to handle known issue such as levelDB crash
     * NOTE: this may cause data lose for failure such as levelDB crash
     */
    void tolerateFailureStart();

    <T> T apiCall(boolean instant, Map params, Class<T> returnType);

    <T> T apiCall(String path, Map params, Class<T> returnType);

    boolean apiDelete(List<String> matches);

    String SERVICE_ID = "prometheus";
    String SERVER2_PATH = PrometheusConstant.PROMETHEUS_2_BIN_PATH;
    String ALERT_URL = "/prometheus/api/v1/alerts";
    String HTTP_API_PATH = "/api/v1/query";
    String HTTP_RANGE_API_PATH = "/api/v1/query_range";
    String HTTP_SERIES_PATH = "/api/v1/series";
    String HTTP_SERIES2_PATH = "/api/v1/admin/tsdb/delete_series";
    String HTTP_REMOTE_READ_PATH = "api/v1/read";
    String HTTP_LABEL_ALL_PATH = "/api/v1/label/";
    String HTTP_LABEL_DETAIL_PATH = HTTP_LABEL_ALL_PATH + "%s" + "/values";

    String VAR_LIB_DIR = PathUtil.join(CoreGlobalProperty.DATA_DIR, "/prometheus");
    String DATA_DIR_2 = PathUtil.join(VAR_LIB_DIR, "data2");

    String PROMETHEUS_BOOT_ERROR = "Prometheus boot error";

    String HOME_DIR = PathUtil.getFolderUnderZStackHomeFolder("prometheus");
    String DISCORVERY_ROOT = PathUtil.join(PathUtil.getFolderUnderZStackHomeFolder("prometheus"), "discovery");
    String LOG_PATH_2 = PathUtil.join(PathUtil.getFolderUnderZStackHomeFolder("prometheus"), "logs/prometheus2.log");
    String RULES_ROOT = PathUtil.join(HOME_DIR, "rules");
    String ACTION_CATEGORY = "prometheus";
    String COLLECTD_EXPORTER_STARTUP_ARGUMENT_0 = " -collectd.listen-address :25826 ";
    String COLLECTD_EXPORTER_PORT_STARTUP_ARGUMENT_FORMAT = " -collectd.listen-address :%s ";
    String COLLECTD_EXPORTER_WEB_PORT_STARTUP_ARGUMENT_FORMAT = " -web.listen-address :%s ";

    class APIReturnStruct {
        public String status;
        public String error;
    }

    class APIRangeQueryStruct extends APIReturnStruct {
        public static class Value {
            public Map<String, String> metric;
            public List<List> values;
        }

        public static class Data {
            public String resultType;
            public List<Value> result;
        }

        public Data data;
    }

    class APIQueryMetadataStruct extends APIReturnStruct {
        public List<Map<String, String>> data;
    }

    class APIQueryLabelValueStruct extends APIReturnStruct {
        public List<String> data;
    }
}
