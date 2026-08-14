package org.zstack.premium.externalservice.prometheus

import groovy.text.GStringTemplateEngine
import org.zstack.core.Platform
import org.zstack.header.errorcode.OperationFailureException

/**
 * Created by kayo on 2018/10/26.
 */
class PrometheusServiceUnitConfig {
    public static final String BINARY_PATH = "BINARY_PATH"
    public static final String PARAMETERS = "PARAMETERS"
    public static final String RESTART_SEC = "RESTART_SEC"
    public static final String MAIN_PID = "MAIN_PID"

    private static GStringTemplateEngine engine = new GStringTemplateEngine()

    private static String getTemplate(String version) {
        switch (version) {
            case "2.x":
                return SERVICE_UNIT_TEMPLATE2
            default:
                throw new OperationFailureException(Platform.inerr("invalid prometheus version: %s", version))
        }
    }

    static String makeServiceUnitFile(Map<String, String> parameters, String version) {
        Map<String, Object> bindings = new HashMap<>()
        bindings.put(BINARY_PATH, parameters.get(BINARY_PATH))
        bindings.put(PARAMETERS, parameters.get(PARAMETERS))
        bindings.put(RESTART_SEC, parameters.get(RESTART_SEC))
        bindings.put(MAIN_PID, "\$MAINPID")

        def tmpl = engine.createTemplate(new StringReader(getTemplate(version))).make(bindings)
        return tmpl.toString()
    }

    static final String SERVICE_UNIT_TEMPLATE2 = '''[Unit]
Description=ZStack Managed Prometheus2 Service
After=network.target
[Service]
Type=simple
User=root
ExecStart=${BINARY_PATH} ${PARAMETERS}
Restart=on-failure
RestartSec=${RESTART_SEC}
LimitNOFILE=32768
[Install]
WantedBy=default.target
'''
}


