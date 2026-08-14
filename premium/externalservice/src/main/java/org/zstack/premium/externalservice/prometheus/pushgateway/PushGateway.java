package org.zstack.premium.externalservice.prometheus.pushgateway;

import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.externalservice.ExternalService;
import org.zstack.premium.externalservice.prometheus.PrometheusConstant;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface PushGateway extends ExternalService {
    class Data {
        public String metricName;
        public double value;
        public long time;
        public Map<String, String> labels;
    }

    void push(Collection<Data> data);
    
    String BINARY_PATH = Optional
            .ofNullable(PathUtil.findFileOnClassPath(PrometheusConstant.PUSHGATEWAY_BIN_PATH))
            .map(File::getAbsolutePath)
            .orElse(null);
    String DATA_FILE =  PathUtil.join(CoreGlobalProperty.DATA_DIR, "/prometheus/pushgateway/persistence.data");

    String API_PATH = "/metrics/job/zstack";
}
