package org.zstack.premium.externalservice.prometheus

import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.HttpClientErrorException
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.zstack.core.Platform
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.externalservice.AbstractLocalExternalService
import org.zstack.core.thread.ThreadFacade
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.rest.RESTFacade
import org.zstack.utils.Bash
import org.zstack.utils.StringDSL
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

import java.util.function.Supplier

/**
 * Created by mingjian.deng on 2019/3/21.*/
abstract class AbstractPrometheusImpl extends AbstractLocalExternalService implements Prometheus {
    CLogger logger = Utils.getLogger(AbstractPrometheusImpl.class)

    protected PrometheusParam param
    protected PrometheusConfig config = new PrometheusConfig()
    protected String baseUrl
    protected Map<String, String> parameters = new HashMap<>()
    protected List<String> binaries = new ArrayList<>()
    @Autowired
    private PluginRegistry pluginRgty
    protected List<PrometheusDriverExtensionPoint> driverExts = new ArrayList<>()

    @Autowired
    protected RESTFacade restf
    @Autowired
    protected ThreadFacade thdf

    private static final String VERSION_FILE_PATH = [Prometheus.VAR_LIB_DIR, "version"].join("/")

    @Override
    abstract protected String[] getCommandLineKeywords();

    @Override
    abstract String getName();

    abstract protected void prometheusUpChecker(boolean abortStarUp);

    abstract protected void setupParameters();

    abstract protected void systemctl();

    abstract protected void systemctl(String ctl);

    protected void extendConfig() {
        driverExts = pluginRgty.getExtensionList(PrometheusDriverExtensionPoint.class)
    }

    private void prepare() {
        File logDir = new File(param.logPath).parentFile
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        File storagePath = new File(param.storagePath)
        if (!storagePath.exists()) {
            storagePath.mkdirs()
        }

        // workaround bug https://github.com/prometheus/prometheus/issues/953
        // for VERSION file missing
        new Bash() {
            @Override
            protected void scripts() {
                int returnCode = sudoRun("[ -f ${VERSION_FILE_PATH} ]")

                // file exists
                if (returnCode == 0) {
                    return
                }

                sudoRun("${param.getBinaryPath()} --version > ${VERSION_FILE_PATH}")
            }
        }.execute()

        new File(param.getConfPath()).write(config.toString())
    }

    private boolean checkIfConfFileChanged() {
        File confFile = new File(param.getConfPath())
        if (!confFile.exists()) {
            return true
        }

        String newConfig = config.toString()
        String oldConfig = confFile.readLines().join("\n")

        if (!StringDSL.stringCompareInLineOrderIndpendent(newConfig, oldConfig)) {
            logger.debug("${confFile.absolutePath} changed, restart prometheus. changed content")
            return true
        }

        File serviceFile = new File(param.servicePath)
        if (!serviceFile.exists()) {
            return true
        }

        String newServiceFile = PrometheusServiceUnitConfig.makeServiceUnitFile(parameters, param.getVersion())
        String oldServiceFile = serviceFile.readLines().join("\n")

        if (!StringDSL.stringCompareInLineOrderIndpendent(newServiceFile, oldServiceFile)) {
            logger.debug("${serviceFile.absolutePath} changed, restart prometheus. changed content")
            return true
        }

        return false
    }

    private boolean checkIfVersionChanged() {
        boolean ret = true
        File vf = new File(VERSION_FILE_PATH)

        new Bash() {
            @Override
            protected void scripts() {
                sudoRun("chown zstack:zstack ${VERSION_FILE_PATH}")

                if (vf.exists()) {
                    String currentVersion = vf.readLines().join("\n")
                    sudoRun("${param.getBinaryPath()} --version")
                    errorOnFailure()
                    String newVersion = stdout()

                    String differ = StringUtils.difference(currentVersion, newVersion)

                    ret = StringUtils.isNotBlank(differ)
                    if (ret) {
                        logger.debug("prometheus version changed. restarts it\nold version:\n${currentVersion}\n\nnew version: \n${newVersion}")
                    }
                }

                sudoRun("${param.getBinaryPath()} --version > ${VERSION_FILE_PATH}")
            }

        }.execute()

        return ret
    }

    private boolean checkIfPluginRequireToReboot() {
        for (Supplier<Boolean> s : config.getRebootJugers()) {
            boolean reboot = s.get()
            if (reboot) {
                logger.debug("plugin[${s.class}] requires to reboot prometheus")
                return true
            }
        }

        return false
    }

    @Override
    void start() {
        setupParameters()

        new Bash() {
            @Override
            protected void scripts() {
                sudoRun("chmod +x ${param.getBinaryPath()}")
            }
        }.execute()

        extendConfig()

        if (checkIfConfFileChanged() || checkIfVersionChanged() || checkIfPluginRequireToReboot()) {
            // stop all the old process...
            binaries.each {
                new Bash() {
                    @Override
                    protected void scripts() {
                        unsetE()
                        sudoRunScripts("systemctl", "stop", it)
                    }
                }.execute()
            }
            stop()
        } else {
            if (!isAlive()) {
                systemctl("start")
            }

            return
        }

        prepare()
        systemctl()
    }

    @Override
    boolean isAlive() {
        return getPID() != null
    }

    @Override
    PrometheusConfig getConfig() {
        return config
    }

    @Override
    PrometheusParam getParams() {
        return param
    }

    @Override
    void rewriteConfigAndReload() {
        new File(param.getConfPath()).write(config.toString())
        reload()
    }

    @Override
    void restart() {
        stop()
        start()
    }

    @Override
    void reload() {
        if (!isAlive()) {
            start()
            return
        }

        new Bash() {
            @Override
            protected void scripts() {
                setE()

                sudoRun("kill -1 ${getPID()}")
            }
        }.execute()
    }

    @Override
    void tolerateFailureStart() {
        start()

        def action = new AbstractLocalExternalService.ActionIfServiceNotUp()
        action.timeout = param.getStartUpTimeout()
        action.upChecker = { return getPID() != null }
        action.action = {
            prometheusUpChecker(true)
        }

        // Also check prometheus error when pid exists
        action.actionAfterUp = {
            prometheusUpChecker(false)
        }
        action.run()
    }

    protected HttpHeaders buildPrometheusHeaders() {
        def headers = new HttpHeaders()
        if (PrometheusGlobalConfig.ENABLE_BASIC_AUTH.value(Boolean.class)) {
            String credentials = PrometheusGlobalConfig.BASIC_AUTH_USERNAME.value() + ":" + PrometheusGlobalConfig.BASIC_AUTH_PASSWORD.value()
            headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes("UTF-8")))
        }
        return headers
    }

    private <T> T httpGet(String url, List<String> params, Class retClz) {
        if (params != null) {
            url = "${url}?${params.join('&')}"
        }

        def uri = URI.create(url).normalize()

        if (logger.isTraceEnabled()) {
            logger.trace("call prometheus: ${uri.toString()}")
        }

        try {
            def entity = new HttpEntity<String>(buildPrometheusHeaders())
            def rsp = restf.getRESTTemplate().exchange(uri, HttpMethod.GET, entity, String.class)
            if (!rsp.getStatusCode().is2xxSuccessful()) {
                throw new OperationFailureException(Platform.operr("Prometheus API failure, status code: ${rsp.getStatusCode()}, body: ${rsp.getBody()}"))
            }

            return JSONObjectUtil.toObject(rsp.getBody(), retClz)
        } catch (HttpClientErrorException e) {
            throw new OperationFailureException(Platform.operr("Prometheus API failure, status code: ${e.getStatusCode().toString()}, body: ${e.getResponseBodyAsString()}"))
        }
    }

    private <T> T doApiCall(boolean instant, String path, Map<String, Object> params, Class<T> returnType) {
        String url = baseUrl

        if (path == null) {
            if (instant) {
                url += HTTP_API_PATH
            } else {
                url += HTTP_RANGE_API_PATH
            }
        } else {
            url += path
        }

        List<String> ps = []
        params?.each { k, v ->
            if (v instanceof String) {
                ps.add("${k}=${URLEncoder.encode(v, "UTF-8")}")
            } else if (v instanceof Collection) {
                ps.addAll(v.collect { vv -> "${k}=${URLEncoder.encode(vv.toString(), "UTF-8")}" })
            } else {
                throw new Exception("unknown value type[${v.class}], key = ${k}")
            }
        }
        driverExts.each {
            it.beforePrometheusApicall(param.ip, url)
        }
        def ret = httpGet(url, ps, returnType)
        if ("error" == ret["status"]) {
            throw new OperationFailureException(Platform.operr("Prometheus API error: %s", ret["error"]))
        }

        return ret as T
    }

    @Override
    <T> T apiCall(boolean instant, Map params, Class<T> returnType) {
        return doApiCall(instant, null, params, returnType)

    }

    @Override
    <T> T apiCall(String path, Map params, Class<T> returnType) {
        return doApiCall(false, path, params, returnType)
    }

    @Override
    void stop() {
        new Bash() {
            @Override
            protected void scripts() {
                setE()
                systemctl("stop")
                sudoRunScripts("rm", "-f", param.servicePath)
                run("for i in 1 2 3; do sudo systemctl daemon-reload && break || sleep 5; done")
            }
        }.execute()
        super.stop()
    }
}
