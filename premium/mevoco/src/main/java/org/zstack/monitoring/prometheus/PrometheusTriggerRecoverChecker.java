package org.zstack.monitoring.prometheus;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.premium.externalservice.prometheus.PrometheusFactory;
import org.zstack.utils.MapGetter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by xing5 on 2017/6/19.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrometheusTriggerRecoverChecker {
    @Autowired
    private PrometheusFactory factory;

    public boolean check(String query) {
        Prometheus prometheus = factory.getPrometheus();
        LinkedHashMap r = prometheus.apiCall(true, map(e("query", query)), LinkedHashMap.class);
        MapGetter ret = new MapGetter(r);
        List results = ret.get("data.result", List.class);

        if (results.isEmpty()) {
            // no result means the trigger is recovered
            return true;
        }

        Map metric = (Map) results.get(0);
        return "0".equals(((List)metric.get("value")).get(1));
    }
}
