package org.zstack.zwatch.namespace.event;

import org.zstack.header.message.Message;
import org.zstack.header.rest.RestAPIExtensionPoint;
import org.zstack.zwatch.prometheus.RestPrometheusNamespace;

/**
 * Created by mingjian.deng on 2020/3/19.
 */
public class RestEvent implements RestAPIExtensionPoint {
    public RestEvent() {
    }

    @Override
    public void afterAPIRequest(Message method) {
        RestPrometheusNamespace.afterAPIRequest(method);
    }

    @Override
    public void beforeAPIResponse(Message msg) {
        RestPrometheusNamespace.beforeAPIResponse(msg);
    }

    @Override
    public void beforeRestResponse(String method, int statusCode) {
        RestPrometheusNamespace.beforeRestResponse(method, statusCode);
    }

    @Override
    public void afterRestRequest(String method) {
        RestPrometheusNamespace.afterRestRequest(method);
    }
}
