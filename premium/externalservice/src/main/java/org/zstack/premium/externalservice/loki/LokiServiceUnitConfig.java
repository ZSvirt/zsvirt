package org.zstack.premium.externalservice.loki;

import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import org.zstack.core.Platform;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mingjian.deng on 2019/9/6.
 */
public class LokiServiceUnitConfig {
    public enum templateParameters {
        BINARY_PATH,
        PARAMETERS,
        LOKI_SERVICE_PORT,
        LOKI_GRPC_PORT,
        PROMTAIL_PORT,
        LOCAL_IP,
        ZSTACK_HOME,
        LOKI_API_URL,
    }
    private static GStringTemplateEngine engine = new GStringTemplateEngine();

    protected static Map<String, Object> getBindings(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> bindings = new HashMap<>();
        for (templateParameters e: templateParameters.values()) {
            bindings.put(e.toString(), parameters.get(e.toString()));
        }
        return bindings;
    }

    public static String makeConfigFile(Map<String, String> parameters, String initialConfigFilePath) {
        File config = PathUtil.findFileOnClassPath(initialConfigFilePath);
        if (config == null) {
            throw new RuntimeException(String.format("cannot find %s in classpath", initialConfigFilePath));
        }

        try {
            Writable tmpl = engine.createTemplate(new FileReader(config)).make(getBindings(parameters));
            return tmpl.toString();
        } catch (Exception e) {
            throw new OperationFailureException(Platform.inerr("create config from %s failed: %s", initialConfigFilePath, e.getMessage()));
        }
    }

    public static String makeServiceUnitFile(Map<String, String> parameters, String template) {
        try {
            Writable tmpl = engine.createTemplate(new StringReader(template)).make(getBindings(parameters));
            return tmpl.toString();
        } catch (Exception e) {
            throw new OperationFailureException(Platform.inerr("create loki service failed: %s", e.getMessage()));
        }
    }

    public static final String LOKI_SERVICE_UNIT_TEMPLATE = "[Unit]\n" +
    "Description=ZStack Managed Loki Service\n" +
    "After=network.target\n" +
    "[Service]\n" +
    "Type=simple\n" +
    "User=zstack\n" +
    "ExecStart=${BINARY_PATH} ${PARAMETERS}\n" +
    "Restart=on-failure\n" +
    "LimitNOFILE=32768\n" +
    "[Install]\n" +
    "WantedBy=default.target";

    public static final String PROMTAIL_UNIT_TEMPLATE = "[Unit]\n" +
    "Description=ZStack Managed Promtail Service\n" +
    "After=network.target\n" +
    "[Service]\n" +
    "Type=simple\n" +
    "User=root\n" +
    "ExecStart=${BINARY_PATH} ${PARAMETERS}\n" +
    "Restart=on-failure\n" +
    "LimitNOFILE=32768\n" +
    "[Install]\n" +
    "WantedBy=default.target";
}
