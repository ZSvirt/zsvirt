package org.zstack.premium.externalservice.prometheus.pushgateway

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.zstack.core.Platform
import org.zstack.core.externalservice.AbstractLocalExternalService
import org.zstack.header.core.external.service.ExternalServiceCapabilities
import org.zstack.core.externalservice.ExternalServiceCapabilitiesBuilder
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.rest.RESTFacade
import org.zstack.premium.externalservice.prometheus.PrometheusConfig
import org.zstack.utils.Bash

class PushGatewayImpl extends AbstractLocalExternalService implements PushGateway {
    @Autowired
    RESTFacade restf

    ExternalServiceCapabilities capabilities = ExternalServiceCapabilitiesBuilder
            .build()
            .reloadConfig(false)

    @Override
    protected String[] getCommandLineKeywords() {
        return ["pushgateway", "zstack", "WEB-INF"]
    }

    @Override
    String getName() {
        return "prometheus-pushgateway-${Platform.managementServerIp}"
    }

    private static String getListenAddress() {
        String listenAddr = PushGatewayGlobalProperty.LISTEN_ADDRESS
        if (listenAddr == "AUTO") {
            listenAddr = "${Platform.managementServerIp}:9091"
        }

        return listenAddr
    }

    PushGatewayImpl() {
        if (BINARY_PATH == null) {
            throw new CloudRuntimeException("pushgateway is not found in class path")
        }
    }

    @Override
    void start() {
        if (isAlive()) {
            return
        }

        String dataFile = PushGatewayGlobalProperty.PERSISTENCE_FILE
        if (dataFile == "AUTO") {
            dataFile = DATA_FILE
        }

        String listenAddr = getListenAddress()

        new Bash() {
            @Override
            protected void scripts() {
                setE()
                mkdirs(dirname(dataFile))

                def cmd = []
                cmd.add("nohup ${BINARY_PATH}")
                cmd.add("--web.listen-address ${listenAddr}")
                cmd.add("--persistence.file ${dataFile}")
                cmd.add("--persistence.interval ${PushGatewayGlobalProperty.PERSISTENCE_INTERVAL}")
                cmd.add(">/dev/null 2>&1 < /dev/null &")

                def cmdstr = """chmod +x ${BINARY_PATH}
${cmd.join(" ")}
echo \$!
disown
"""
                sudoRun(cmdstr)
            }
        }.execute()

        doActionIfServicePIDNotShowUpInTwoMinutes {
            new Bash() {
                @Override
                protected void scripts() {
                    abortManagementNode(stdout())
                }
            }.execute()
        }
    }

    @Override
    boolean isAlive() {
        return getPID() != null
    }

    @Override
    ExternalServiceCapabilities getExternalServiceCapabilities() {
        return capabilities
    }

    @Override
    void reload() {
        // do nothing
    }

    @Override
    void push(Collection<PushGateway.Data> data) {
        List<String> samples = []
        data.each { d ->
            List<String> labels = []
            if (d.labels != null) {
                d.labels.each {k, v -> labels.add("${k}=\"${v}\"")}
            }

            samples.add("${d.metricName}{${labels.join(',')}} ${d.value}")
        }

        if (!samples.isEmpty()) {
            String url = "http://${getListenAddress()}${API_PATH}"
            ResponseEntity rsp = restf.getRESTTemplate().postForEntity(url, samples.join("\n")+"\n", String.class)
            if (!rsp.getStatusCode().is2xxSuccessful()) {
                throw new OperationFailureException(Platform.operr("unable to push metrics, ${rsp.getBody()}"))
            }
        }
    }

    static void preparePrometheusConfig(PrometheusConfig config) {
        config.scrapeConfig {
            job_name = "custom-metrics-pushgateway"
            honor_labels = true
            staticConfig {
                targets = [getListenAddress()]
            }
        }
    }
}
