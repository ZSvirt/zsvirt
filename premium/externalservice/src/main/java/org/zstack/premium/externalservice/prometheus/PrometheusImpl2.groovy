package org.zstack.premium.externalservice.prometheus

import com.google.common.base.Strings
import org.springframework.web.client.HttpClientErrorException
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.zstack.core.Platform
import org.zstack.core.externalservice.ExternalServiceCapabilitiesBuilder
import org.zstack.header.core.external.service.ExternalServiceCapabilities
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.utils.Bash
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger
/**
 * Created by mingjian.deng on 2019/3/21.*/
class PrometheusImpl2 extends AbstractPrometheusImpl implements Prometheus {
    CLogger logger = Utils.getLogger(PrometheusImpl2.class)

    ExternalServiceCapabilities capabilities = ExternalServiceCapabilitiesBuilder
            .build()
            .reloadConfig(true)

    PrometheusImpl2(PrometheusParam param) {
        this.param = param
        param.checkParams()
        baseUrl = "http://${param.getIp()}:${param.getPort()}/"
    }

    @Override
    void setupParameters() {
        def params = new ArrayList<>()
        params.add("--web.enable-admin-api")   // allow admin api
        params.add("--web.listen-address ${param.getIp()}:${param.getPort()}")
        params.add("--storage.tsdb.path ${param.getStoragePath()}")
        params.add("--storage.tsdb.retention.time ${param.getRetention()}")
        params.add("--storage.tsdb.retention.size ${param.getRetentionSize()}")
        params.add("--config.file ${param.getConfPath()}")
        params.add("--storage.tsdb.min-block-duration ${param.getWalBlockMinRetention()}")
        if (PrometheusGlobalProperty.SUPPORT_THANOS) {
            params.add("--storage.tsdb.max-block-duration ${param.getWalBlockMinRetention()}")  // max = min to use thanos
        }
        if (param.getParameters() != null) {
            params.addAll(param.getParameters())
        }

        parameters.put(PrometheusServiceUnitConfig.PARAMETERS, params.join(" "))
        parameters.put(PrometheusServiceUnitConfig.BINARY_PATH, param.getBinaryPath())
        parameters.put(PrometheusServiceUnitConfig.RESTART_SEC, String.valueOf(param.getStartUpTimeout()))
    }

    @Override
    protected String[] getCommandLineKeywords() {
        return ["prometheus2", "zstack", "--config.file", "storage.tsdb.path"] as String[]
    }

    @Override
    String getName() {
        return "prometheus2-on-management-node-${Platform.getManagementServerIp()}"
    }

    @Override
    ExternalServiceCapabilities getExternalServiceCapabilities() {
        return capabilities
    }

    @Override
    protected void systemctl() {
        String serviceUnitFile = PrometheusServiceUnitConfig.makeServiceUnitFile(parameters, "2.x")

        new Bash() {
            @Override
            protected void scripts() {
                setE()
                sudoRunScripts("touch", param.servicePath)
                sudoRunScripts("chmod", "777", param.servicePath)
                sudoRunFormat("echo '${serviceUnitFile}' > ${param.servicePath}")
                sudoRunScripts("chmod", "644", param.servicePath)

                run("for i in 1 2 3; do sudo systemctl daemon-reload && break || sleep 5; done")
                sudoRunScripts("systemctl", "enable", "prometheus2")
                sudoRunScripts("systemctl", "restart", "prometheus2")
            }
        }.execute()
    }

    @Override
    protected void prometheusUpChecker(boolean abortStartUp) {
        new Bash() {
            @Override
            protected void scripts() {
                int return_code = sudoRun("systemctl show -p ActiveEnterTimestamp prometheus2 | awk '{print \$2,\$3}'")

                if (return_code == 0) {
                    String timeStamp = stdout().replace("\n", "")
                    sudoRun("journalctl --unit=prometheus2 --since '${timeStamp}'")
                } else {
                    sudoRun("journalctl --unit=prometheus2 --since '${param.getStartUpTimeout()} seconds ago'")
                }

                if (stdout().contains("leveldb/storage: corrupted or incomplete meta file")) {
                    logger.warn("WE ARE SORRY YOU LOST YOUR MONITORING DATA. The prometheus data has crashed" +
                            " because the levelDB corrupted. The prometheus community has promised to replace the levelDB to fix" +
                            " this issue. Before that, we have to tolerate it. Logs:\n ${stdout()}")

                    if (!new File(param.storagePath).isDirectory()) {
                        throw new Exception("why ${param.storagePath} is not a directory???")

                    }

                    sudoRun("/bin/rm -rf ${param.storagePath}")
                    restart()
                } else if (stdout().contains("Error opening memory series storage") && stdout().contains("VERSION")) {
                    sudoRun("/bin/rm -f ${param.storagePath}/VERSION")
                    restart()
                } else if (stdout().contains("Restart Prometheus ASAP to initiate recovery")) {
                    restart()
                }

                if (abortStartUp) {
                    abortManagementNode(PROMETHEUS_BOOT_ERROR + stdout() + stderr())
                }

                logger.debug("No specific error match in prometheus log")
            }
        }.execute()
    }

    @Override
    protected void systemctl(String ctl) {
        new Bash() {
            @Override
            protected void scripts() {
                sudoRunScripts("systemctl", ctl, "prometheus2")
            }
        }.execute()
    }

    @Override
    boolean apiDelete(List<String> matches) {
        String url = baseUrl + HTTP_SERIES2_PATH

        List<String> pairs = []
        matches?.each { match ->
            pairs.add("match[]=${URLEncoder.encode(match, "UTF-8")}")
        }

        if (pairs != null && !pairs.isEmpty()) {
            url += "?${pairs.join('&')}"
        }
        def uri = URI.create(url).normalize()
        if (logger.isTraceEnabled()) {
            logger.trace("call prometheus: ${uri.toString()}")
        }

        try {
            def entity = new HttpEntity<String>(null, buildPrometheusHeaders())
            restf.getRESTTemplate().exchange(uri, HttpMethod.POST, entity, String.class)
            return true
        } catch (HttpClientErrorException e) {
            throw new OperationFailureException(Platform.operr("Prometheus API failure, status code: ${e.getStatusCode().toString()}, body: ${e.getResponseBodyAsString()}"))
        }
    }

    @Override
    void ctlstop() {
        new Bash() {
            @Override
            protected void scripts() {
                unsetE()
                sudoRunScripts("systemctl", "stop", "prometheus2")
            }
        }.execute()
    }

    @Override
    protected void extendConfig() {
        super.extendConfig()

        if (PrometheusGlobalProperty.ENABLE_REMOTE_READ) {
            String remoteReadUrl = String.format("http://%s:%s/%s",
                    param.getIp(), PrometheusGlobalProperty.READONLY_PORT, HTTP_REMOTE_READ_PATH)
            config.remoteRead(remoteReadUrl)
        }

        if (!Strings.isNullOrEmpty(PrometheusGlobalProperty.REMOTE_WRITE_URL)) {
            config.remoteWrite(PrometheusGlobalProperty.REMOTE_WRITE_URL)
        }
    }
}
