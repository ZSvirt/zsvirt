package org.zstack.testlib

import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import okhttp3.OkHttpClient
import org.apache.commons.lang.StringUtils
import org.zstack.core.Platform
import org.zstack.core.StartMode
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusImpl2
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.AbstractService
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.identity.AccountConstant
import org.zstack.header.message.APIMessage
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor
import org.zstack.header.message.AbstractBeforeSendMessageInterceptor
import org.zstack.header.message.Event
import org.zstack.header.message.Message
import org.zstack.sdk.ErrorCode
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.ZQLQueryReturn
import org.zstack.sdk.ZSClient
import org.zstack.sdk.identity.role.RoleInventory
import org.zstack.testlib.collectstrategy.SubCaseCollectionStrategy
import org.zstack.testlib.collectstrategy.SubCaseCollectionStrategyFactory
import org.zstack.testlib.util.Retry
import org.zstack.utils.ShellUtils
import org.zstack.utils.Utils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger
import org.zstack.utils.path.PathUtil

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.logging.Level
import java.util.logging.Logger
/**
 * Created by xing5 on 2017/2/12.
 */
abstract class Test extends ApiHelper implements Retry {
    final CLogger logger = Utils.getLogger(this.getClass())
    static final String targetSubCaseParamKey = "cases"
    static final String subCaseExecutionTimesKey = "times"

    static Object deployer
    static Map<String, String> apiPaths = new ConcurrentHashMap<>()

    private final static long DEFAULT_MESSAGE_TIMEOUT_SECS = TimeUnit.SECONDS.toMillis(25)
    static Map<Class, Closure> functionForMockTestObjectFactory = new ConcurrentHashMap<>()

    protected List<Closure> methodsOnClean = []

    protected List zqlQuery(String text) {
        return zQLQuery { zql = text }.results[0].inventories
    }

    protected ZQLQueryReturn zqlQueryFull(String text) {
        return zQLQuery { zql = text }.results[0]
    }

    static {
        Platform.functionForMockTestObject = { supplier ->
            def obj = supplier.get()
            if (obj == null) {
                return obj
            }

            def entry = functionForMockTestObjectFactory.find {
                return it.key.isAssignableFrom(obj.getClass())
            }

            if (entry == null) {
                return obj
            }

            return entry.value(obj)
        }
    }

    static long getMessageTimeoutMillsConfig(){
        String msgTimeoutStr = System.getProperty("msgTimeoutMins")

        if(System.getProperty("maven.surefire.debug") != null && msgTimeoutStr == null){
            return TimeUnit.MINUTES.toMillis(30)
        }

        if(msgTimeoutStr == null || msgTimeoutStr.isEmpty()){
            return DEFAULT_MESSAGE_TIMEOUT_SECS
        }

        long msgTimeout = Long.parseLong(msgTimeoutStr)
        return TimeUnit.MINUTES.toMillis(msgTimeout)
    }

    private final int PHASE_NONE = 0
    private final int PHASE_SETUP = 1
    private final int PHASE_ENV = 2
    private final int PHASE_TEST = 3
    private final int PHASE_CLEAN = 4

    // these are global variables
    static EnvSpec currentEnvSpec
    static ComponentLoader componentLoader
    static long envCreateTime
    static long testRunTime
    static long cleanEnvTime

    private int phase = PHASE_NONE
    protected BeanConstructor beanConstructor
    protected SpringSpec _springSpec

    protected void setSpringSpec() {
        _springSpec = new SpringSpec()
    }

    Test() {
        setSpringSpec()
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE)
    }

    static SpringSpec makeSpring() {
        return makeSpring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()
            vyos()
            flatNetwork()
            ceph()
            lb()
            nfsPrimaryStorage()
            externalPrimaryStorage()
            zbs()
            eip()
            portForwarding()
            smp()
            console()

            include("accountImport.xml")
            include("LdapManagerImpl.xml")
            include("captcha.xml")
            include("CloudBusAopProxy.xml")
            include("ZoneManager.xml")
            include("webhook.xml")
            include("Progress.xml")
            include("vip.xml")
            include("vxlan.xml")
            include("mediateApiValidator.xml")
            include("LongJobManager.xml")
            include("log.xml")
            include("HostAllocateExtension.xml")
            include("HostNetworkManager.xml")
            include("telemetry.xml")
        }
    }


    static EnvSpec makeEnv(@DelegatesTo(strategy=Closure.DELEGATE_FIRST, value=EnvSpec.class) Closure c) {
        def spec = new EnvSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return spec
    }

    static EnvSpec makeEnv(EnvSpec parent, @DelegatesTo(strategy=Closure.DELEGATE_FIRST, value=EnvSpec.class) Closure c) {
        def spec = parent
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return spec
    }

    static SpringSpec makeSpring(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SpringSpec.class) Closure c) {
        def spec = new SpringSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return spec
    }

    protected void withSession(SessionInventory s, Closure c)  {
        SessionInventory backup = currentEnvSpec.session
        currentEnvSpec.session = s
        c()
        currentEnvSpec.session = backup
        logOut { sessionUuid = s.uuid }
    }

    protected void withAccountSession(String accountName, String password, Closure c) {
        def session = logInByAccount {
            delegate.accountName = accountName
            delegate.password = password
        } as SessionInventory
        withSession(session, c)
    }

    protected void onCleanExecute(Closure c) {
        methodsOnClean.add(c)
    }

    protected EnvSpec env(@DelegatesTo(strategy=Closure.DELEGATE_FIRST, value=EnvSpec.class) Closure c) {
        def spec = new EnvSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return spec
    }

    protected SimpleEnvSpec senv(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SimpleEnvSpec.class) Closure c) {
        def spec = new SimpleEnvSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return spec
    }

    protected void spring(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SpringSpec.class) Closure c) {
        c.delegate = _springSpec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
    }

    abstract void setup()
    abstract void environment()
    abstract void test()

    protected boolean DEPLOY_DB = true
    protected boolean NEED_WEB_SERVER = true
    protected boolean API_PORTAL = true
    protected boolean INCLUDE_CORE_SERVICES = true

    protected String getDeployDBScriptBaseDir() {
        String home = System.getProperty("user.dir")
        return  [home, "../"].join("/")
    }

    private Map<String, String> getHostAndPort(String jdbcUrl) {
        def result = new HashMap()
        if (jdbcUrl == null) {
            return null
        }
        if (!jdbcUrl.trim().startsWith("jdbc:mysql://")) {
            return null
        }
        String sub = jdbcUrl.trim().substring(13)
        def pair = sub.split(":") as List<String>
        if (pair == null || pair.size() < 2) {
            return null
        }
        result.put("host", pair[0])
        if (pair[1].indexOf("/") == -1) {
            result.put("port", pair[1])
        } else {
            result.put("port", pair[1].substring(0, pair[1].indexOf("/")))
        }
        return result
    }

    private void deployDB() {
        logger.info("Deploying database ...")
        String baseDir = getDeployDBScriptBaseDir()
        Properties prop = new Properties()

        try {
            prop.load(this.getClass().getClassLoader().getResourceAsStream("zstack.properties"))

            String user = System.getProperty("DB.user")
            if (user == null) {
                user = prop.getProperty("DB.user")
                if (user == null) {
                    user = prop.getProperty("DbFacadeDataSource.user")
                }
                if (user == null) {
                    throw new CloudRuntimeException("cannot find DB user in zstack.properties, please set either DB.user or DbFacadeDataSource.user")
                }
            }

            String password = System.getProperty("DB.password")
            if (password == null) {
                password = prop.getProperty("DB.password")
                if (password == null) {
                    password = prop.getProperty("DbFacadeDataSource.password")
                }
                if (password == null) {
                    throw new CloudRuntimeException("cannot find DB user in zstack.properties, please set either DB.password or DbFacadeDataSource.password")
                }
            }

            Map<String, String> hostAndPort = getHostAndPort(System.getProperty("DB.url"))
            if (hostAndPort == null) {
                hostAndPort = getHostAndPort(prop.getProperty("DB.url"))
                if (hostAndPort == null) {
                    hostAndPort = getHostAndPort(prop.getProperty("DbFacadeDataSource.jdbcUrl"))
                }
            }

            logger.debug("host, port: ${hostAndPort.toString()}")

            if (hostAndPort == null || (hostAndPort["host"] == "localhost" && hostAndPort["port"] == "3306")) {
                ShellUtils.run("build/deploydb.sh $user $password", baseDir, false)
            } else {
                def host = hostAndPort["host"]
                if (host == "localhost" && hostAndPort["port"] != "3306") {
                    host = "127.0.0.1"
                }
                ShellUtils.run("build/deploydb.sh $user $password $host ${hostAndPort["port"]}", baseDir, false)
            }

            logger.info("Deploying database successfully")
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to deploy zstack database for testing", e)
        }
    }

    private void nextPhase() {
        phase ++
    }

    private void hijackService() {
        CloudBus bus = bean(CloudBus.class)
        if(bus instanceof CloudBusImpl2){
            logger.info(String.format("CloudBus message timeout: %s mills", getMessageTimeoutMillsConfig()))
            ((CloudBusImpl2)bus).setDEFAULT_MESSAGE_TIMEOUT(getMessageTimeoutMillsConfig())
        }

        def serviceId = "test.hijack.service"
        def service = new AbstractService() {
            @Override
            void handleMessage(Message msg) {
                if (currentEnvSpec == null) {
                    return
                }

                try {
                    def all = currentEnvSpec.messageHandlers.findAll { k, _ -> k.isAssignableFrom(msg.getClass()) }

                    boolean handled = false
                    boolean conded = false
                    all.values().each { tuples ->
                        // if there is a closure with condition satisfied, skip the mock without conditions
                        tuples.each {
                            Closure cond = it[0]
                            if (cond != null && cond(msg)) {
                                conded = true
                            }
                        }
                        tuples.each {
                            Closure cond = it[0]
                            Closure handler = it[1]

                            if (cond == null && conded) {
                                // skip (cond == null)
                                return
                            }

                            if (cond != null && !cond(msg)) {
                                return
                            }

                            handled = true
                            if (handler.maximumNumberOfParameters <= 1) {
                                handler(msg)
                            } else {
                                handler(msg, bus)
                            }
                        }
                    }

                    if (!handled) {
                        bus.replyErrorByMessageType(msg, "a test case installed message handler for this message, however," +
                                " its condition closure decides not to handle this message. Check your test case")
                    } else {
                        currentEnvSpec.messageHandlerCounters.putIfAbsent(msg.getClass(), new AtomicInteger(0))
                        currentEnvSpec.messageHandlerCounters.get(msg.getClass()).incrementAndGet()
                    }
                } catch (Exception ex) {
                    bus.replyErrorByMessageType(msg, ex)
                }
            }

            @Override
            String getId() {
                return bus.makeLocalServiceId(serviceId)
            }

            @Override
            boolean start() {
                return true
            }

            @Override
            boolean stop() {
                return true
            }
        }

        bus.registerService(service)

        bus.installBeforeSendMessageInterceptor(new AbstractBeforeSendMessageInterceptor() {
            @Override
            void beforeSendMessage(Message msg) {
                if (Event.class.isAssignableFrom(msg.class) || currentEnvSpec == null) {
                    return
                }

                def handler = currentEnvSpec.messageHandlers.find { k, _ -> k.isAssignableFrom(msg.getClass()) }
                if(handler != null && !handler.getValue().isEmpty()){
                    bus.makeLocalServiceId(msg, serviceId)
                }
            }
        })

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            int orderOfBeforeDeliveryMessageInterceptor() {
                return 999
            }

            @Override
            void beforeDeliveryMessage(Message msg) {
                if (currentEnvSpec?.notifiersOfReceivedMessages != null) {
                    currentEnvSpec.notifiersOfReceivedMessages.each { msgClz, cs ->
                        if (msgClz.isAssignableFrom(msg.getClass())) {
                            synchronized (cs) {
                                cs.each {
                                    it(msg)
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    void buildBeanConstructor() {
        beanConstructor = BeanConstructorFactory.getBeanConstructor(this.getCaseMode())
        if (_springSpec.all) {
            beanConstructor.loadAll = true
        } else {
            _springSpec.xmls.each {it ->
                logger.debug("add xml to bean constructor ${it}")
                beanConstructor.addXml(it)
            }
        }

        componentLoader = beanConstructor.build()
    }

    protected String adminSession() {
        return currentEnvSpec.session.uuid
    }

    protected void useSpring(SpringSpec spec) {
        _springSpec = spec
    }

    private void prepare() {
        nextPhase()

        setup()

        if (API_PORTAL) {
            spring {
                include("ManagementNodeManager.xml")
                include("ApiMediator.xml")
                include("AccountManager.xml")
                include("identity.xml")
            }
        }

        if (INCLUDE_CORE_SERVICES) {
            spring {
                includeCoreServices()
            }
        }

        if (DEPLOY_DB) {
            deployDB()
        }

        buildBeanConstructor()

        nextPhase()

        hijackService()
        environment()
    }

    protected Closure notifyWithReceiveNoReplyMessage(Class msgClz, Closure c) {
        assert currentEnvSpec != null

        List<Closure> cs = currentEnvSpec.messagesWithoutReplies.computeIfAbsent(msgClz, { Collections.synchronizedList([]) })
        synchronized (cs) {
            cs.add(c)
        }

        return {
            synchronized (cs) {
                cs.remove(c)
            }
        }
    }

    protected Closure notifyWhenReceivedMessage(Class msgClz, Closure c) {
        assert currentEnvSpec != null

        List<Closure> cs = currentEnvSpec.notifiersOfReceivedMessages.computeIfAbsent(msgClz, { Collections.synchronizedList([]) })
        synchronized (cs) {
            cs.add(c)
        }

        return {
            synchronized (cs) {
                cs.remove(c)
            }
        }
    }

    protected <T> T bean(Class<T> clz) {
        assert componentLoader != null : "componentLoader is null!!!"
        return componentLoader.getComponent(clz)
    }

    protected <T> T dbFindById(Long id, Class<T> voClz) {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        return dbf.findById(id, voClz)
    }

    protected <T> T dbFindByUuid(String uuid, Class<T> voClz) {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        return dbf.findByUuid(uuid, voClz)
    }

    protected long dbCount(Class voClz) {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        return dbf.count(voClz)
    }

    protected boolean dbIsExists(Object primaryKey, Class clz) {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        return dbf.isExist(primaryKey, clz)
    }

    protected void cleanCase(Case c) {
        logger.info("start cleanup for case ${this.class}")
        long startCleanTime = System.currentTimeMillis()
        c.clean()
        methodsOnClean.each { it() }
        cleanEnvTime = System.currentTimeMillis() - startCleanTime
        logger.info("create env spend: ${envCreateTime}, test run spend: ${testRunTime}, clean env spend: ${cleanEnvTime}")

    }

    @org.junit.Test
    final void doTest() {
        try {
            configProperty()
            prepare()
            nextPhase()
            test()
            nextPhase()

            if ((this instanceof Case) && System.getProperty("clean") != null) {
                cleanCase((Case)this)
            }

            if (System.getProperty("apipath") != null) {
                def dir = new File([getResultDirBase(), "apipath"].join("/"))
                dir.deleteDir()
                dir.mkdirs()

                apiPaths.each { name, path ->
                    new File([dir.absolutePath, name.replace(".", "_")].join("/")).write(path)
                }
            }
        } catch (AssertionError e) {
            logger.warn("\n${e.message}", e)
            System.exit(1)
        } catch (Throwable t) {
            logger.warn("", t)
            System.exit(1)
        }
    }

    static void handleHttp(HttpServletRequest request, HttpServletResponse response) {
        if (WebBeanConstructor.WEB_HOOK_PATH.toString().contains(request.getRequestURI())) {
            ZSClient.webHookCallback(request, response)
        } else {
            currentEnvSpec.handleSimulatorHttpRequests(request, response)
        }
    }

    static class SubCaseResult {
        Boolean success
        String error
        String name
        String envCreateSpend
        String testRunSpend
        String cleanEnvSpend
        transient Class caseType
    }

    static Case CURRENT_SUB_CASE

    protected void beforeRunSubCase() {

    }

    protected String getResultDirBase() {
        String resultDir = System.getProperty("resultDir")
        if (resultDir == null) {
            resultDir = [System.getProperty("user.dir"), "zstack-integration-test-result"].join("/")
        }
        return resultDir
    }

    private SubCaseCollectionStrategy getSubCaseCollectionStrategy(){
        String strategyName = System.getProperty("subCaseCollectionStrategy")
        SubCaseCollectionStrategy strategy = SubCaseCollectionStrategyFactory.getSubCaseCollectionStrategy(strategyName)

        assert null != strategy : "can not find SubCaseCollectionStrategy"
        logger.info("input subCaseCollectionStrategy = ${strategyName}, subCaseCollectionStrategy is ${strategy.strategyName}")
        return strategy
    }

    protected void runSubCases() {
        def resultDir = [getResultDirBase(), this.class.name.replace(".", "_")].join("/")
        def dir = new File(resultDir)
        dir.deleteDir()
        dir.mkdirs()

        SubCaseCollectionStrategy strategy = getSubCaseCollectionStrategy()
        def caseTypes = strategy.collectSubCases(this)
        caseTypes = caseTypes.findAll { !it.isAnnotationPresent(SkipTestSuite.class) && !it.isAnnotationPresent(Deprecated.class) }

        File blackList = PathUtil.findFileOnClassPath("blackList.ut")
        if (blackList != null) {
            List<String> skippedCases = blackList.readLines().collect { it.trim() }
            logger.warn("cases listed in blackList.ut will be skipped:\n${skippedCases.join("\n")}")
            caseTypes = caseTypes.findAll { !skippedCases.contains(it.simpleName) }
        }

        String caseListString = caseTypes.collect {it.name}.join("\n")
        def cases = new File([dir.absolutePath, "cases"].join("/"))
        cases.write(caseListString)

        File testSuiteBlackList = PathUtil.findFileOnClassPath("blackList.test")
        if (testSuiteBlackList != null) {
            List<String> skippedSuite = testSuiteBlackList.readLines().collect { it.trim() }
            if (skippedSuite.contains(getClass().getSimpleName())) {
                logger.warn("the test suite[${getClass().getSimpleName()}] is listed in blackList.test, cases will be skipped:\n${caseListString}")
                return
            }
        }

        if (System.getProperty("list") != null) {
            return
        }

        System.setProperty("inTestSuite", "true")

        if (caseTypes.isEmpty()) {
            return
        }

        List<SubCaseResult> allCases = caseTypes.collect {
            SubCaseResult ret = new SubCaseResult()
            ret.caseType = it
            ret.name = it.simpleName
            return ret
        }

        def suiteStartTime = System.currentTimeMillis()
        String suiteTimeoutSecs = System.getProperty("suiteTimeoutSecs")
        boolean hasFailure = false
        boolean isTimeout = false
        String caseResultShellPath = System.getProperty("caseResultShellPath")

        for (SubCaseResult r in allCases) {
            if (suiteTimeoutSecs != null && System.currentTimeSeconds() - suiteStartTime / 1000L > Long.valueOf(suiteTimeoutSecs)){
                hasFailure = true
                isTimeout = true
                logger.error(String.format("testsuite[%s] run timeout，timeout = %s，Case[%s] skipped ", this.class.name, suiteTimeoutSecs, r.name))
                break
            }
            logger.debug("creating sub-case ${r.caseType}")
            def c = r.caseType.getConstructor().newInstance() as Case

            String caseLogStartLine = "case log of ${c.class} starts here"
            String caseLogEndLine = "case log of ${c.class} ends here"

            logger.info("starts running a sub case[${c.class}] of suite[${this.class}]")
            new File([dir.absolutePath, "current-case"].join("/")).write("${c.class}")

            def caseStartTime = System.currentTimeMillis()
            try {
                CURRENT_SUB_CASE = c
                c.metaClass.collectErrorLog = {
                    File failureLogDir = new File([dir.absolutePath, "failureLogs", r.caseType.name.replace(".", "_")].join("/"))
                    failureLogDir.mkdirs()
                    File failureLog = new File([failureLogDir.absolutePath, "case.log"].join("/"))

                    File mgmtLogPath = new File([System.getProperty("user.dir"), "management-server.log"].join("/"))

                    ShellUtils.run("""\
start=`grep -nr "$caseLogStartLine" ${mgmtLogPath.absolutePath} | grep -v ShellUtils | gawk '{print \$1}' FS=":"`
tail -n +\$start ${mgmtLogPath.absolutePath} > ${failureLog.absolutePath}

mysqldump -u root zstack > ${failureLogDir.absolutePath}/dbdump.sql
""", false)
                }

                beforeRunSubCase()

                logger.info(caseLogStartLine)

                c.run()

                r.success = true
                logger.info("a sub case[${c.class}] of suite[${this.class}] completes without any error")
            } catch (StopTestSuiteException e) {
                hasFailure = true
                r.success = false
                r.error = e.message

                logger.error("a sub case[${c.class}] of suite[${this.class}] throws StopTestSuiteException, ${e.message}", e)
                break
            } catch (Throwable t) {
                hasFailure = true
                r.success = false
                r.error = t.message

                logger.error("a sub case[${c.class}] of suite[${this.class}] fails, ${t.message}", t)
            } finally {

                def spendTime = (System.currentTimeMillis() - caseStartTime) as long
                logger.info("spend time collected: case ${c.class.simpleName} of suite[${this.class}] spends ${spendTime} millisencends")

                if (caseResultShellPath != null){
                    String shellCmd = String.format("suite_name=%s case_name=%s result=%s spend_time=%s sh %s", this.class.name, r.caseType.name, (r.success ? "success" : "failure"), spendTime, caseResultShellPath)
                    ShellUtils.run(shellCmd)
                }

                def fname = c.class.name.replace(".", "_") + "." + spendTime + "." + (r.success ? "success" : "failure")
                def rfile = new File([dir.absolutePath, fname].join("/"))
                r.envCreateSpend = envCreateTime + " ms"
                r.testRunSpend = testRunTime + " ms"
                r.cleanEnvSpend = cleanEnvTime + " ms"
                rfile.write(JSONObjectUtil.toJsonString(r))

                logger.info("write test result of a sub case[${c.class}] of suite[${this.class}] to $fname")
                logger.info(caseLogEndLine)

                assert currentEnvSpec == null: "EnvSpec is not cleaned after execute ${this.class}."
            }
        }

        int success = 0
        int failure = 0
        int skipped = 0
        int timeout = 0
        long envSpend = 0
        long runSpend = 0
        long cleanSpend = 0
        allCases.each {
            if (it.success == null && isTimeout){
                if (caseResultShellPath != null){
                    String shellCmd = String.format("suite_name=%s case_name=%s result=timeout spend_time=0 sh %s", this.class.name, it.caseType.name, caseResultShellPath)
                    ShellUtils.run(shellCmd)
                }
                def fname = it.caseType.name.replace(".", "_") + ".timeout"
                def rfile = new File([dir.absolutePath, fname].join("/"))
                rfile.createNewFile()
                timeout ++
            } else if (it.success == null){
                if (caseResultShellPath != null){
                    String shellCmd = String.format("suite_name=%s case_name=%s result=skipped spend_time=0 sh %s", this.class.name, it.caseType.name, caseResultShellPath)
                    ShellUtils.run(shellCmd)
                }
                def fname = it.caseType.name.replace(".", "_") + ".skipped"
                def rfile = new File([dir.absolutePath, fname].join("/"))
                rfile.createNewFile()
                skipped ++
            } else if (it.success) {
                success ++
                envSpend += Long.valueOf(it.envCreateSpend.split(" ")[0])
                runSpend += Long.valueOf(it.testRunSpend.split(" ")[0])
                cleanSpend += Long.valueOf(it.cleanEnvSpend.split(" ")[0])
            } else {
                failure ++
            }
        }

        def suiteEndTime = System.currentTimeMillis()

        def summary = new File([dir.absolutePath, "summary"].join("/"))
        summary.write(JSONObjectUtil.toJsonString([
                "total" : caseTypes.size(),
                "success": success,
                "failure": failure,
                "skipped": skipped,
                "timeout": timeout,
                "passRate": ((float)success / (float)caseTypes.size()) * 100,
                "spendTime": suiteEndTime - suiteStartTime,
                "createEnvTotal": envSpend,
                "runTestTotal": runSpend,
                "cleanEnvTotal": cleanSpend
        ]))

        new File([dir.absolutePath, "done"].join("/")).createNewFile()

        if (hasFailure) {
            // some cases failed, exit with code 1
            System.exit(1)
        }
    }

    protected static <T> T json(String str, Class<T> type) {
        return JSONObjectUtil.toObject(str, type)
    }

    SessionInventory loginAsAdmin() {
        return logInByAccount {
            accountName = AccountConstant.INITIAL_SYSTEM_ADMIN_NAME
            password = AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
        } as SessionInventory
    }

    protected <T extends APIMessage> Object submitLongJob(Class<T> msgClass, Consumer<T> consumer) {
        def message = msgClass.newInstance()
        consumer.accept(message)
        return submitLongJob {
            delegate.jobName = msgClass.getSimpleName()
            delegate.jobData = JSONObjectUtil.toJsonString(message)
        }
    }

    protected void attachPredefineRoles(String accountUuid, String... roleNames) {
        for (final def roleName in roleNames) {
            def roles = queryRole {
                delegate.conditions = ["name=predefined: " + roleName]
            } as List<RoleInventory>
            assert roles.size() == 1 : "failed to find predefined with name: " + roleName
            attachRoleToAccount {
                delegate.roleUuid = roles[0].uuid
                delegate.accountUuid = accountUuid
            }
        }
    }

    private static boolean getRetryReturnValue(ret, boolean throwError = false) {
        boolean judge

        if (ret instanceof Closure) {
            try {
                def r = ret()
                judge = (r != null && r instanceof Boolean) ? r : true
            } catch (Throwable t) {
                if (throwError) {
                    throw t
                } else {
                    judge = false
                }
            }
        } else {
            judge = ret
        }

        return judge
    }

    @Deprecated
    protected static boolean retryInSecs3(int total = 15, int interval = 1, Closure c) {
        int count = 0

        def ret = null

        while (count < total) {
            ret = c()

            if (getRetryReturnValue(ret)) {
                return true
            }

            TimeUnit.SECONDS.sleep(interval)
            count += interval
        }

        return getRetryReturnValue(ret, true)
    }

    protected long costMillis(Closure c){
        long startTime = new Date().getTime()
        c()
        long endTime = new Date().getTime()
        long cost =  endTime - startTime

        logger.info("cost millis is ${cost}")
        return cost
    }

    @Deprecated
    protected static boolean retryInMillis(int total, int interval = 500, Closure c) {
        int count = 0

        def ret = null

        while (count < total) {
            ret = c()

            if (getRetryReturnValue(ret)) {
                return true
            }

            TimeUnit.MILLISECONDS.sleep(interval)
            count += interval
        }

        return getRetryReturnValue(ret, true)
    }

    static class ExpectedException extends Exception {
        ExpectedException() {
        }

        ExpectedException(String var1) {
            super(var1)
        }

        ExpectedException(String var1, Throwable var2) {
            super(var1, var2)
        }

        ExpectedException(Throwable var1) {
            super(var1)
        }

        ExpectedException(String var1, Throwable var2, boolean var3, boolean var4) {
            super(var1, var2, var3, var4)
        }
    }

    static void expectError(Closure c) {
        expect(AssertionError.class, c)
    }

    static void expect(exceptions, Closure c) {
        List<Class> lst = []
        if (exceptions instanceof Collection) {
            lst.addAll(exceptions)
        } else if (exceptions instanceof Class && Throwable.class.isAssignableFrom(exceptions)) {
            lst.add(exceptions)
        } else {
            throw new Exception("the first argument must be a Throwable or a collection of Throwable, but got a ${exceptions.class.name}")
        }

        try {
            c()
            throw new ExpectedException("expect exceptions[${lst.collect { it.name }}] happen, but nothing happens")
        } catch (Throwable t) {
            if (t instanceof ExpectedException) {
                throw t
            }

            for (Class tt : lst) {
                if (tt.isAssignableFrom(t.class)) {
                    return
                }
            }

            throw new Exception("expected to get a Throwable of ${lst.collect { it.name }} but got ${t.class.name}")

        }
    }

    static void expectApiFailure(
            Closure c,
            @DelegatesTo(strategy = Closure.OWNER_FIRST, value = ErrorCode.class) Closure errorCodeChecker) {
        AssertionError error = null

        try {
            c()
        } catch (AssertionError t) {
            error = t
        } catch (Throwable t) {
            throw new Exception("expected to get a Throwable of AssertionError but got ${t.class.name}", t)
        }

        if (error == null) {
            throw new ExpectedException("expect AssertionError raised, but nothing happens")
        }

        def message = error.message
        if (!message.startsWith("API failure: ")) {
            throw new Exception("unexpected API failure", error)
        }

        ErrorCode code
        try {
            int startIndex = "API failure: ".length()
            int endIndex = message.indexOf(". Expression: ")
            code = ZSClient.fromJson(message.substring(startIndex, endIndex), ErrorCode)
        } catch (JsonSyntaxException | IndexOutOfBoundsException ignored) {
            throw new Exception("unexpected API failure", error)
        }

        if (code.code == "sdk.1000") {
            code = extractFromSDKError(code)
        }

        errorCodeChecker.resolveStrategy = Closure.OWNER_FIRST
        errorCodeChecker.delegate = code
        errorCodeChecker.call()
    }

    /**
     * MN always return HTTP code 503 when MN raise Identity ErrorCode.
     * ZStack SDK will warp true error code with sdk.1000.
     *
     * The function would extract original error code from SDKErrorCode.
     */
    static ErrorCode extractFromSDKError(ErrorCode sdkError) {
        assert sdkError.code == "sdk.1000"
        def errorCodeJson = JsonParser.parseString(sdkError.details).getAsJsonObject().get("error").toString()
        return ZSClient.fromJson(errorCodeJson, ErrorCode.class)
    }

    protected void configProperty() {
        return
    }

    protected void configSkipMNExit() {
        String skipMNExit = System.getProperty("skipMNExit")
        if (StringUtils.isEmpty(skipMNExit)) {
            System.setProperty(Platform.SKIP_STOP, Boolean.TRUE.toString())
        } else if (skipMNExit.equalsIgnoreCase(Boolean.TRUE.toString())) {
            System.setProperty(Platform.SKIP_STOP, Boolean.TRUE.toString())
        }
    }

    StartMode getStabilityTestStartMode() {
        String targetCaseList = System.getProperty(targetSubCaseParamKey)
        List<String> caseClassNameList = targetCaseList.split(",")
        def count = caseClassNameList.stream().map { it -> Class.forName(it).newInstance().getCaseMode() }.distinct().count()
        if (count != 1) {
            throw new Exception("All cases in the case list should use the same mode")
        }
        return Class.forName(caseClassNameList[0]).newInstance().getCaseMode()
    }

    StartMode getCaseMode() {
        return StartMode.DEFAULT
    }

    static long gb(double gb) {
        return SizeUnit.GIGABYTE.toByte(gb)
    }
}
