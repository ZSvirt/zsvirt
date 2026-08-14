package org.zstack.monitoring.prometheus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.rest.RESTFacade;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2017/6/10.
 */
@Controller
public class AlertManagerController {
    private static CLogger logger = Utils.getLogger(AlertManagerController.class);

    @Autowired
    private PrometheusMonitorProviderFactory ppf;
    @Autowired
    private RESTFacade restf;

    @RequestMapping(
            value = Prometheus.ALERT_URL,
            method = {RequestMethod.POST}
    )
    @ExceptionSafe
    public void handleAlerts(HttpServletRequest request, HttpServletResponse response) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        HttpEntity<String> e = restf.httpServletRequestToHttpEntity(request);

        try {
            List<PrometheusAlert> alerts = JSONObjectUtil.toCollection(e.getBody(), ArrayList.class, PrometheusAlert.class);

            for (PrometheusAlert alert : alerts) {
                ppf.handleAlert(alert);
            }
        } catch (Throwable t) {
            logger.warn(String.format("unhandled exception for the alert:%s", e.getBody()), t);
        } finally {
            response.setStatus(HttpStatus.OK.value());
            response.getWriter().write("");
        }
    }
}
