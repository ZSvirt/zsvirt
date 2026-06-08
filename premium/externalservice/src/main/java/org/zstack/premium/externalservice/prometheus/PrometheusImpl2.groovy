package org.zstack.premium.externalservice.prometheus

import com.google.common.base.Strings
import groovy.json.JsonSlurper
import org.springframework.web.client.HttpClientErrorException
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.externalservice.ExternalServiceCapabilitiesBuilder
import org.zstack.header.core.external.service.ExternalServiceCapabilities
import org.zstack.header.core.external.service.ExternalServiceConfigurationInventory
import org.zstack.header.core.external.service.ExternalServiceConfigurationVO
import org.zstack.header.core.external.service.ExternalServiceConfigurationVO_
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.utils.Bash
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import static org.zstack.core.Platform.operr
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
    String getServiceType() {
        return "Prometheus2"
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

        applyRemoteWriteConfig()
    }

    @Override
    void externalConfig(String serviceType) {
        if (serviceType != getServiceType() || !getExternalServiceCapabilities().isReloadConfig()) {
            logger.info(String.format("requested external service type %s not equal to current service type %s or current service can not reload config, skip", serviceType, getServiceType()))
            return
        }

        applyRemoteWriteConfig()
        rewriteConfigAndReload()
    }

    private void applyRemoteWriteConfig() {
        List<PrometheusConfig.RemoteWrite> remoteWrites = queryRemoteWrite(getServiceType())
        if (remoteWrites != null) {
            config.remoteWrite(remoteWrites)
        } else if (!Strings.isNullOrEmpty(PrometheusGlobalProperty.REMOTE_WRITE_URL)) {
            config.remoteWrite(PrometheusGlobalProperty.REMOTE_WRITE_URL)
        } else {
            config.remoteWrite([])
        }
    }

    List<PrometheusConfig.RemoteWrite> queryRemoteWrite(String serviceType) {
        List<ExternalServiceConfigurationVO> vos = Q.New(ExternalServiceConfigurationVO.class)
                .eq(ExternalServiceConfigurationVO_.serviceType, serviceType)
                .list()
        if (vos.isEmpty()) {
            return null
        }
        if (vos.size() > 1) {
            throw new OperationFailureException(operr("found multiple external service configurations for type [%s]", serviceType))
        }

        List<ExternalServiceConfigurationInventory> invs = ExternalServiceConfigurationInventory.valueOf(vos, false)
        return parseRemoteWrite(invs)
    }

    List<PrometheusConfig.RemoteWrite> parseRemoteWrite(Collection invs) {
        def slurper = new JsonSlurper()
        List<PrometheusConfig.RemoteWrite> result = []

        invs.each { inv ->
            if (inv.configuration == null || inv.configuration == "") {
                return
            }

            Map m
            try {
                def parsed = slurper.parseText(inv.configuration)
                if (!(parsed instanceof Map)) {
                    throw new OperationFailureException(operr("external configuration[uuid:%s][type:%s] must be a JSON object",
                            inv.getUuid(), inv.getServiceType()))
                }
                m = parsed as Map
            } catch (Exception e) {
                if (e instanceof OperationFailureException) {
                    throw e
                }
                throw new OperationFailureException(operr("external configuration[uuid:%s][type:%s] content failed to parse: %s",
                        inv.getUuid(), inv.getServiceType(), e.getMessage()))
            }

            def remoteWriteList = m?.remote_write
            if (remoteWriteList == null) {
                logger.info(String.format("external configuration[uuid:%s][type:%s] not contains remote write, skip", inv.getUuid(), inv.getServiceType()))
                return
            }

            if (!(remoteWriteList instanceof List)) {
                throw new OperationFailureException(operr("external configuration[uuid:%s][type:%s] remote_write must be a JSON array",
                        inv.getUuid(), inv.getServiceType()))
            }

            remoteWriteList.each { remoteWrite ->
                if (!(remoteWrite instanceof Map)) {
                    throw new OperationFailureException(operr("external configuration[uuid:%s][type:%s] remote_write item must be a JSON object",
                            inv.getUuid(), inv.getServiceType()))
                }

                def url = remoteWrite.url?.toString()?.trim()
                if (!url) {
                    throw new OperationFailureException(operr("external configuration[uuid:%s][type:%s] remote_write url can not be empty",
                            inv.getUuid(), inv.getServiceType()))
                }
                if (!isValidRemoteWriteUrl(url)) {
                    throw new OperationFailureException(operr("external configuration[uuid:%s][type:%s] remote_write url[%s] is invalid",
                            inv.getUuid(), inv.getServiceType(), url))
                }

                def rw = new PrometheusConfig.RemoteWrite()
                rw.url = url

                if (remoteWrite.basic_auth != null) {
                    if (!(remoteWrite.basic_auth instanceof Map)) {
                        throw new OperationFailureException(operr("external configuration[uuid:%s][type:%s] basic_auth must be a JSON object",
                                inv.getUuid(), inv.getServiceType()))
                    }

                    def u = remoteWrite.basic_auth.username?.toString()?.trim()
                    def p = remoteWrite.basic_auth.password?.toString()?.trim()
                    if (u || p) {
                        def ba = new PrometheusConfig.BasicAuth()
                        ba.username = u
                        ba.password = p
                        rw.basic_auth = ba
                    }
                }

                if (remoteWrite.write_relabel_configs != null) {
                    if (!(remoteWrite.write_relabel_configs instanceof Collection)) {
                        throw new OperationFailureException(operr("external configuration[uuid:%s][type:%s] write_relabel_configs must be a JSON array",
                                inv.getUuid(), inv.getServiceType()))
                    }

                    def relabels = []

                    remoteWrite.write_relabel_configs.eachWithIndex { r, idx ->
                        if (!(r instanceof Map)) {
                            throw new OperationFailureException(operr("external configuration[uuid:%s][type:%s] write_relabel_configs[%s] must be a JSON object",
                                    inv.getUuid(), inv.getServiceType(), idx))
                        }

                        def rc = new PrometheusConfig.RelabelConfig()
                        if (r.source_labels != null) {
                            if (!(r.source_labels instanceof Collection)) {
                                throw new OperationFailureException(operr("external configuration[uuid:%s][type:%s] write_relabel_configs[%s].source_labels must be a JSON array",
                                        inv.getUuid(), inv.getServiceType(), idx))
                            }

                            r.source_labels.each {
                                def v = it?.toString()?.trim()
                                if (v) {
                                    rc.sourceLabel(v)
                                }
                            }
                        }

                        rc.target_label = r.target_label?.toString()?.trim()
                        rc.regex = r.regex?.toString()?.trim() ?: rc.regex
                        rc.replacement = r.replacement?.toString()?.trim() ?: rc.replacement
                        rc.action = r.action?.toString()?.trim() ?: rc.action

                        if (rc.action == "replace" && !rc.target_label) {
                            throw new OperationFailureException(operr("external configuration[uuid:%s][type:%s] write_relabel_configs[%s] target_label can not be empty when action=replace",
                                    inv.getUuid(), inv.getServiceType(), idx))
                        }

                        relabels << rc
                    }

                    if (!relabels.isEmpty()) {
                        rw.write_relabel_configs = relabels
                    }
                }

                result << rw
            }
        }

        return result
    }

    private static boolean isValidRemoteWriteUrl(String url) {
        try {
            URI uri = new URI(url)
            String scheme = uri.getScheme()
            return scheme != null &&
                    ["http", "https"].contains(scheme.toLowerCase()) &&
                    !Strings.isNullOrEmpty(uri.getHost())
        } catch (Exception ignored) {
            return false
        }
    }
}
