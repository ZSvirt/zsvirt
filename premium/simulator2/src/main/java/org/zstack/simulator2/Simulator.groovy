package org.zstack.simulator2

import org.apache.commons.lang.StringUtils
import org.springframework.http.*
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.web.client.AsyncRestTemplate
import org.zstack.appliancevm.ApplianceVmGlobalProperty
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.aspect.UnitTestBypassHelper
import org.zstack.header.rest.RESTConstant
import org.zstack.kvm.KVMGlobalProperty
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty
import org.zstack.premium.externalservice.prometheus.PrometheusFactory
import org.zstack.premium.externalservice.prometheus.pushgateway.PushGatewayFactory
import org.zstack.rest.RestConstants
import org.zstack.rest.RestServletRequestInterceptor
import org.zstack.simulator2.agents.Agent
import org.zstack.simulator2.api.*
import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageGlobalProperty
import org.zstack.storage.backup.sftp.SftpBackupStorageGlobalProperty
import org.zstack.storage.ceph.CephGlobalProperty
import org.zstack.utils.FieldUtils
import org.zstack.utils.HTTP
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger
import org.zstack.zwatch.prometheus.PrometheusCollector
import org.zstack.zwatch.prometheus.PrometheusDatabaseDriver
import org.zstack.zwatch.prometheus.PrometheusStaticConfigManagerImpl

import javax.script.ScriptEngineManager
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by xing5 on 2017/9/15.
 */
class Simulator {
    static CLogger logger = Utils.getLogger(Simulator.class)


    static Map<String, Closure> httpHandlers = [:]
    private ScriptEngineManager scriptMgr = new ScriptEngineManager()
    private static Map<String, Closure> agentHandlers = [:]
    private static ConcurrentHashMap<String, Closure> agentPreHandlers = [:]
    private static ConcurrentHashMap<String, Closure> agentPostHandlers = [:]
    private static AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate()
    private Map<String, Class> resourceTypes = [:]
    static String version
    static String sendCommandUrl

    SQLite sqlite

    void installAgentHandler(String path, Closure c) {
        if (agentHandlers.containsKey(path)) {
            throw new Exception("duplicated agent handler for the path ${path}")
        }

        agentHandlers[path] =  c
    }

    void installAgentPreHandler(String path, Closure c) {
        if (agentPreHandlers.containsKey(path)) {
            throw new Exception("duplicated agent pre handler for the path ${path}")
        }

        agentPreHandlers[path] =  c
    }

    void removeAgentPreHandler(String path) {
        agentPreHandlers.remove(path)
    }

    void installAgentPostHandler(String path, Closure c) {
        if (agentPostHandlers.containsKey(path)) {
            throw new Exception("duplicated agent post handler for the path ${path}")
        }

        agentPostHandlers[path] =  c
    }

    void removeAgentPostHandler(String path) {
        agentPostHandlers.remove(path)
    }

    void removeAllAgentCustomizeHandler() {
        agentPostHandlers.clear()
        agentPreHandlers.clear()
    }

    static String REMOTE_ADDR = "REMOTE-ADDR"

    private static HttpEntity toHttpEntity(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder()
        String line
        while ((line = request.getReader().readLine()) != null) {
            sb.append(line)
        }
        request.getReader().close()

        HttpHeaders header = new HttpHeaders()
        for (Enumeration e = request.getHeaderNames() ; e.hasMoreElements() ;) {
            String name = e.nextElement().toString()
            header.add(name, request.getHeader(name))
        }

        URL url = new URL(request.getRequestURL().toString())

        header.add(REMOTE_ADDR, url.getHost())

        return new HttpEntity<String>(sb.toString(), header)
    }

    private static void replyHttpCall(HttpEntity<String> entity, HttpServletResponse response, Object rsp) {
        String taskUuid = entity.getHeaders().getFirst(RESTConstant.TASK_UUID)
        if (taskUuid == null) {
            response.status = HttpStatus.OK.value()
            response.writer.write(rsp == null ? "" :rsp instanceof String ? rsp : JSONObjectUtil.toJsonString(rsp))
            return
        }

        String callbackUrl = entity.getHeaders().getFirst(RESTConstant.CALLBACK_URL)
        String rspBody = rsp == null ? "" : rsp instanceof String ? rsp : JSONObjectUtil.toJsonString(rsp)
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.setContentLength(rspBody.length())
        headers.set(RESTConstant.TASK_UUID, taskUuid)
        HttpEntity<String> rreq = new HttpEntity<String>(rspBody, headers)
        ListenableFuture<ResponseEntity<String>> f = asyncRestTemplate.exchange(callbackUrl, HttpMethod.POST, rreq, String.class)
        f.addCallback({}, { throw new HTTP.HTTPFailureException(504, "failed to response to ZStack") })
    }

    static void handleHttp(HttpServletRequest request, HttpServletResponse response) {
        String url = request.getRequestURI()

        url = StringUtils.removeStart(url, SimulatorGlobalProperty.SIMULATOR_AGENT_ROOT_PATH)
        try {
            def entity = toHttpEntity(request)

            Closure handler = httpHandlers[url]

            if (handler == null) {
                handler = agentHandlers[url]
            }

            if (handler == null) {
                response.setStatus(HttpStatus.NOT_FOUND.value())
                response.getWriter().println("no handler found for the path $url")
                return
            }

            logger.debug("[Simulator Received]: ${url}, ${entity.body}")

            Closure preHandler = agentPreHandlers[url]
            if (preHandler != null) {
                preHandler(entity)
            }

            def rspBody = handler(entity)

            Closure postHandler = agentPostHandlers[url]
            if (postHandler != null) {
                if (postHandler.maximumNumberOfParameters <= 1) {
                    rspBody = postHandler(rspBody)
                } else if (postHandler.maximumNumberOfParameters == 2) {
                    rspBody = postHandler(rspBody, entity)
                }
            }

            replyHttpCall(entity, response, rspBody == null ? [:] : rspBody)
        } catch (Throwable t) {
            logger.warn("error happened when handling $url", t)
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
            response.getWriter().println(t.message)
        }
    }

    static void registerHttpHandler(String path, Closure c) {
        httpHandlers[path] = c
    }

    Simulator() {
        installHttpHandlers()

        UnitTestBypassHelper.registerBypassJudger("${PrometheusFactory.class.getName()}") {
            return false
        }
        UnitTestBypassHelper.registerBypassJudger("${PrometheusDatabaseDriver.class.getName()}") {
            return false
        }
        UnitTestBypassHelper.registerBypassJudger("${PrometheusCollector.class.getName()}") {
            return false
        }
        UnitTestBypassHelper.registerBypassJudger("${PushGatewayFactory.class.getName()}") {
            return false
        }
        UnitTestBypassHelper.registerBypassJudger("${PrometheusStaticConfigManagerImpl.class.getName()}") {
            return false
        }

        CoreGlobalProperty.UNIT_TEST_ON = true
        KVMGlobalProperty.AGENT_URL_ROOT_PATH = SimulatorGlobalProperty.SIMULATOR_AGENT_ROOT_PATH
        VirtualRouterGlobalProperty.AGENT_URL_ROOT_PATH = SimulatorGlobalProperty.SIMULATOR_AGENT_ROOT_PATH
        ApplianceVmGlobalProperty.AGENT_URL_ROOT_PATH = SimulatorGlobalProperty.SIMULATOR_AGENT_ROOT_PATH
        SftpBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH = SimulatorGlobalProperty.SIMULATOR_AGENT_ROOT_PATH
        CephGlobalProperty.PRIMARY_STORAGE_AGENT_URL_ROOT_PATH = SimulatorGlobalProperty.SIMULATOR_AGENT_ROOT_PATH
        CephGlobalProperty.BACKUP_STORAGE_AGENT_URL_ROOT_PATH = SimulatorGlobalProperty.SIMULATOR_AGENT_ROOT_PATH
        ImageStoreBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH = SimulatorGlobalProperty.SIMULATOR_AGENT_ROOT_PATH

        setupAgentHandlers()

        Platform.reflections.getSubTypesOf(VO.class).findAll {
            return !Modifier.isAbstract(it.modifiers)
        }.each {
            resourceTypes[it.simpleName] = it
        }

        def dbFile = new File(System.getProperty("java.io.tmpdir") + "/zstack-simulator.db")
        if (!dbFile.exists()) {
            SimulatorGlobalProperty.REDEPLOY_DB = true
        }

        if (SimulatorGlobalProperty.REDEPLOY_DB) {
            dbFile.delete()
        }

        sqlite = new SQLite(dbFile.absolutePath)
        if (SimulatorGlobalProperty.REDEPLOY_DB) {
            sqlite.createTables()
        }
    }

    private void setupAgentHandlers() {
        def agents = Platform.reflections.getSubTypesOf(Agent.class)
        agents.each {
            def agent = it.newInstance(this)
            agent.setupAgentHandler()
        }
    }

    private void callApiHookers(HttpServletRequest req) {
        def it = restApiHookers.values().iterator()
        while (it.hasNext()) {
            RestApiHooker hooker = ++it

            if (req.getRequestURI() != hooker.path) {
                return
            }

            HttpEntity e = toHttpEntity(req)

            if (hooker.hookMethod == RestApiHooker.HookMethod.byJobUuid) {
                String jobUuid = e.getHeaders().get(RestConstants.HEADER_JOB_UUID)
                if (jobUuid == hooker.jobUuid) {
                    it.remove()
                    throw new RestServletRequestInterceptor.RestServletRequestInterceptorException(hooker.errorCode, hooker.errorMessage)
                }
            } else if (hooker.hookMethod == RestApiHooker.HookMethod.byScript) {
                def script = scriptMgr.getEngineByName(hooker.scriptType.toString())

                def binding = [
                        "PATH": hooker.path,
                        "HEADERS": e.getHeaders().toSingleValueMap(),
                        "BODY": JSONObjectUtil.toObject(e.body, LinkedHashMap.class),
                        "RET": [:]
                ]

                script.eval(hooker.script, binding as Binding)

                Map ret = binding["RET"]

                if (ret.remove == true)  {
                    it.remove()
                }

                if (ret.errorCode != null) {
                    throw new RestServletRequestInterceptor.RestServletRequestInterceptorException(ret.errorCode, ret.errorMessage == null ? "" : ret.errorMessage)
                }
            }
        }
    }

    Simulator(String confPath) {
        this()
    }

    static String UPDATE_PATH = "/simulators/update"
    static String DELETE_PATH = "/simulators/delete"
    static String CREATE_PATH = "/simulators/create"
    static String BATCH_CREATE_PATH = "/simulators/batch-create"
    static String DUMP_PATH = "/simulators/dump"
    static String READ_PATH = "/simulators/query"
    static String CREATE_API_HOOK_PATH = "/simulators/api-hookers/create"
    static String DELETE_API_HOOK_PATH = "/simulators/api-hookers/delete"
    static String ADD_AGENT_PRE_HANDLER_PATH = "/simulators/agent-pre-handlers/add"
    static String REMOVE_AGENT_PRE_HANDLER_PATH = "/simulators/agent-pre-handlers/remove"
    static String ADD_AGENT_POST_HANDLER_PATH = "/simulators/agent-post-handlers/add"
    static String REMOVE_AGENT_POST_HANDLER_PATH = "/simulators/agent-post-handlers/remove"
    static String REMOVE_ALL_AGENT_CUSTOMIZE_HANDLER_PATH = "/simulators/agent-customize-handlers/removeAll"
    static String Get_ALL_AGENT_CUSTOMIZE_HANDLER_PATH = "/simulators/agent-customize-handlers/get"

    Map<String, RestApiHooker> restApiHookers = [:]

    static class CreateApiHookerCmd {
        String id
        RestApiHooker hooker
    }

    static class DumpInfo {
        String type
        Map data = [:]
    }

    static class CustomizeHandleInfo{
        String path
        String type
    }

    private void installHttpHandlers() {
        registerHttpHandler(DUMP_PATH) {
            List ret = []
            resourceTypes.each { type, clz ->
                def info = new DumpInfo(type: type)
                FieldUtils.getAllFields(clz).findAll { it.isAnnotationPresent(Col.class) }.each { f ->
                    Col at = f.getAnnotation(Col.class)

                    def instance = clz.newInstance()
                    List name = ["${f.name} "]
                    name.add("(")
                    def defaultValue = instance.getAt(f.name)
                    if (defaultValue == null) {
                        name.add("mandatory field")
                    } else {
                        name.add("optional field with default value[${defaultValue}]")
                    }

                    if (at.parent() != Object.class) {
                        name.add( ", foreign key to ${at.parent().simpleName}" )
                    }
                    name.add(")")

                    info.data[(name.join(""))] = f.type.simpleName
                }
                ret.add(info)
            }

            return ret
        }

        registerHttpHandler(UPDATE_PATH) { HttpEntity entity ->
            UpdateResourceCmd cmd = JSONObjectUtil.toObject(entity.body, UpdateResourceCmd.class)
            assert cmd.id != null : "id cannot be null"
            assert cmd.type != null : "type cannot be null"
            def clz = resourceTypes[cmd.type]
            assert clz != null : "cannot find resource with type[${cmd.type}]"

            sqlite.updateById(cmd.id, clz, cmd.values)

            return null
        }

        registerHttpHandler(DELETE_PATH) { HttpEntity entity ->
            DeleteResourceCmd cmd = JSONObjectUtil.toObject(entity.body, DeleteResourceCmd.class)

            assert cmd.id != null : "id cannot be null"
            assert cmd.type != null : "type cannot be null"
            def clz = resourceTypes[cmd.type]
            assert clz != null : "cannot find resource with type[${cmd.type}]"

            sqlite.delteById(cmd.id, clz)

            return null
        }

        registerHttpHandler(BATCH_CREATE_PATH) { HttpEntity entity ->
            BatchCreateResourceCmd cmd = JSONObjectUtil.toObject(entity.body, BatchCreateResourceCmd.class)

            cmd.resources.each {
                assert it.type != null : "type cannot be null"
                def clz = resourceTypes[it.type]
                assert clz != null : "cannot find resource with type[${it.type}]"

                def obj = JSONObjectUtil.rehashObject(it.data, clz)
                sqlite.persist(obj)
            }

            return null
        }

        registerHttpHandler(CREATE_PATH) { HttpEntity entity ->
            CreateResourceCmd cmd = JSONObjectUtil.toObject(entity.body, CreateResourceCmd.class)

            assert cmd.type != null : "type cannot be null"
            def clz = resourceTypes[cmd.type]
            assert clz != null : "cannot find resource with type[${cmd.type}]"

            def obj = JSONObjectUtil.rehashObject(cmd.data, clz)
            sqlite.persist(obj)

            return null
        }

        registerHttpHandler(READ_PATH) { HttpEntity entity ->
            QueryResourceCmd cmd = JSONObjectUtil.toObject(entity.body, QueryResourceCmd.class)

            assert cmd.type != null : "type cannot be null"
            def clz = resourceTypes[cmd.type]
            assert clz != null : "cannot find resource with type[${cmd.type}]"

            List ret = sqlite.query(cmd.sql, clz)

            return ret
        }

        registerHttpHandler(CREATE_API_HOOK_PATH) { HttpEntity entity ->
            CreateApiHookerCmd cmd = JSONObjectUtil.toObject(entity.body, CreateApiHookerCmd.class)

            String id = cmd.id
            if (id == null) {
                throw new Exception("the CreateApiHookerCmd command does not have the id field\n ${entity.body}")
            }

            if (cmd.hooker == null) {
                throw new Exception("the CreateApiHookerCmd command does not have the hooker field\n ${entity.body}")
            }

            restApiHookers[id] = cmd.hooker
            return null
        }

        registerHttpHandler(DELETE_API_HOOK_PATH) { HttpEntity entity ->
            Map cmd = JSONObjectUtil.toObject(entity.body, LinkedHashMap.class)
            String id = cmd.id
            if (id == null) {
                throw new Exception("the DeleteApiHookerCmd command does not have the id field\n ${entity.body}")
            }

            restApiHookers.remove(id)
            return null
        }

        registerHttpHandler(ADD_AGENT_PRE_HANDLER_PATH) { HttpEntity entity ->
            AddAgentPreHandlerCmd cmd = JSONObjectUtil.toObject(entity.body, AddAgentPreHandlerCmd.class)
            assert cmd.urlPath != null : "urlPath cannot be null"
            assert cmd.script != null : "script cannot be null"

            def sh = new GroovyShell()
            def closure = sh.evaluate(cmd.script)
            installAgentPreHandler(cmd.urlPath, closure)

            return null
        }

        registerHttpHandler(REMOVE_AGENT_PRE_HANDLER_PATH) { HttpEntity entity ->
            String urlPath = entity.body
            assert urlPath != null : "urlPath cannot be null"

            removeAgentPreHandler(urlPath)
            return null
        }

        registerHttpHandler(ADD_AGENT_POST_HANDLER_PATH) { HttpEntity entity ->
            AddAgentPostHandlerCmd cmd = JSONObjectUtil.toObject(entity.body, AddAgentPostHandlerCmd.class)
            assert cmd.urlPath != null : "urlPath cannot be null"
            assert cmd.script != null : "script cannot be null"

            def sh = new GroovyShell()
            def closure = sh.evaluate(cmd.script)
            installAgentPostHandler(cmd.urlPath, closure)

            return null
        }

        registerHttpHandler(REMOVE_AGENT_POST_HANDLER_PATH) { HttpEntity entity ->
            String urlPath = entity.body
            assert urlPath != null : "urlPath cannot be null"

            removeAgentPostHandler(urlPath)
            return null
        }

        registerHttpHandler(REMOVE_ALL_AGENT_CUSTOMIZE_HANDLER_PATH) { HttpEntity entity ->
            removeAllAgentCustomizeHandler()
            return null
        }

        registerHttpHandler(Get_ALL_AGENT_CUSTOMIZE_HANDLER_PATH) { HttpEntity entity ->
            List<CustomizeHandleInfo> customizeHandleInfos = new ArrayList<>()
            Enumeration<String> preHandlersPath = agentPreHandlers.keys()
            Enumeration<String> postHandlersPath = agentPostHandlers.keys()
            while(preHandlersPath.hasMoreElements()){
                CustomizeHandleInfo preInfo = new CustomizeHandleInfo()
                preInfo.type = "pre"
                preInfo.path = preHandlersPath.nextElement()
                customizeHandleInfos.add(preInfo)
            }
            while(postHandlersPath.hasMoreElements()){
                CustomizeHandleInfo postInfo = new CustomizeHandleInfo()
                postInfo.type = "post"
                postInfo.path = postHandlersPath.nextElement()
                customizeHandleInfos.add(postInfo)
            }
            return customizeHandleInfos
        }
    }
}
