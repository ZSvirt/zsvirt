package org.zstack.testlib

import groovy.transform.AutoClone
import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.http.*
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.configuration.SqlForeignKeyGenerator
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.asyncbatch.While
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.DatabaseFacadeImpl
import org.zstack.core.db.SQL
import org.zstack.core.eventlog.EventLogVO
import org.zstack.core.progress.ActionProgressService
import org.zstack.core.retry.Retry
import org.zstack.core.retry.RetryCondition
import org.zstack.header.core.WhileCompletion
import org.zstack.header.core.WhileDoneCompletion
import org.zstack.header.core.progress.TaskProgressVO
import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.SessionVO
import org.zstack.header.image.ImageDeletionPolicyManager
import org.zstack.header.message.Message
import org.zstack.header.rest.RESTConstant
import org.zstack.header.vm.VmInstanceDeletionPolicyManager
import org.zstack.header.vm.VmSchedHistoryVO
import org.zstack.header.vo.EO
import org.zstack.header.volume.VolumeDeletionPolicyManager
import org.zstack.image.ImageGlobalConfig
import org.zstack.sdk.*
import org.zstack.sdk.attribute.api.CreateResourceAttributeKeyAction
import org.zstack.sdk.attribute.api.DeleteResourceAttributeKeyAction
import org.zstack.sdk.identity.ldap.api.AddLdapServerAction
import org.zstack.sdk.identity.ldap.api.DeleteLdapServerAction
import org.zstack.sdk.identity.role.api.CreateRoleAction
import org.zstack.sdk.identity.role.api.DeleteRoleAction
import org.zstack.sdk.sns.CreateSNSTopicAction
import org.zstack.sdk.sns.DeleteSNSApplicationEndpointAction
import org.zstack.sdk.sns.DeleteSNSApplicationPlatformAction
import org.zstack.sdk.sns.DeleteSNSTopicAction
import org.zstack.sdk.sns.platform.dingtalk.CreateSNSDingTalkEndpointAction
import org.zstack.sdk.sns.platform.email.CreateSNSEmailEndpointAction
import org.zstack.sdk.sns.platform.email.CreateSNSEmailPlatformAction
import org.zstack.sdk.sns.platform.http.CreateSNSHttpEndpointAction
import org.zstack.sdk.sns.platform.snmp.CreateSNSSnmpEndpointAction
import org.zstack.sdk.sns.platform.snmp.CreateSNSSnmpPlatformAction
import org.zstack.sdk.zwatch.alarm.CreateAlarmAction
import org.zstack.sdk.zwatch.alarm.DeleteAlarmAction
import org.zstack.sdk.zwatch.alarm.SubscribeEventAction
import org.zstack.sdk.zwatch.alarm.UnsubscribeEventAction
import org.zstack.sdk.zwatch.alarm.sns.CreateSNSTextTemplateAction
import org.zstack.sdk.zwatch.alarm.sns.DeleteSNSTextTemplateAction
import org.zstack.sdk.zwatch.alarm.sns.template.aliyunsms.CreateAliyunSmsSNSTextTemplateAction
import org.zstack.sdk.zwatch.monitorgroup.api.CreateMonitorGroupAction
import org.zstack.sdk.zwatch.monitorgroup.api.CreateMonitorTemplateAction
import org.zstack.sdk.zwatch.monitorgroup.api.DeleteMonitorGroupAction
import org.zstack.sdk.zwatch.monitorgroup.api.DeleteMonitorTemplateAction
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.testlib.identity.AccountSpec
import org.zstack.testlib.identity.IdentitySpec
import org.zstack.testlib.identity.ldap.LdapServerSpec
import org.zstack.testlib.identity.ldap.LdapVirtualEndpointSpec
import org.zstack.testlib.vfs.VFS
import org.zstack.testlib.vops.VOpsVirtualEndpointSpec
import org.zstack.testlib.zsv.telemetry.TelemetryVirtualEndpointSpec
import org.zstack.utils.BeanUtils
import org.zstack.utils.DebugUtils
import org.zstack.utils.data.Pair
import org.zstack.utils.gson.JSONObjectUtil

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * Created by xing5 on 2017/2/12.
 */
@AutoClone(includeFields=true)
class EnvSpec extends ApiHelper implements Node  {
    protected List<ZoneSpec> zones = []
    List<AccountSpec> accounts = []

    SessionInventory session

    Map specsByName = [:]
    Map specsByUuid = [:]

    private ConcurrentHashMap<String, Long> storageObjectSizeMocks = [:]

    private boolean hasCreated
    private ConcurrentHashMap<String, Closure> httpPreHandlers = [:]
    private ConcurrentHashMap<String, Closure> httpHandlers = [:]
    private ConcurrentHashMap<String, Closure> httpPostHandlers = [:]
    private ConcurrentHashMap<String, Closure> httpFinalHandlers = [:]
    private ConcurrentHashMap<String, AtomicInteger> httpHandlerCounters = [:]
    private ConcurrentHashMap<String, AtomicInteger> httpPostHandlerCounters = [:]
    private ConcurrentHashMap<String, Closure> defaultHttpHandlers = [:]
    private ConcurrentHashMap<String, Closure> defaultHttpPostHandlers = [:]
    protected ConcurrentHashMap<Class, List<Tuple>> messageHandlers = [:]
    protected ConcurrentHashMap<Class, Integer> messageHandlerCounters = [:]
    protected ConcurrentHashMap<Class, List<Closure>> notifiersOfReceivedMessages = [:]
    protected ConcurrentHashMap<Class, List<Closure>> messagesWithoutReplies = [:]
    private ConcurrentHashMap<Class, List<Tuple>> defaultMessageHandlers = [:]
    private ConcurrentHashMap<String, List<Tuple>> httpConditionHandlers = [:]
    private ConcurrentHashMap<String, List<Tuple>> defaultHttpConditionHandlers = [:]
    protected static RestTemplate restTemplate
    protected static Set<Class> simulatorClasses = Platform.reflections.getSubTypesOf(Simulator.class)

    static Set<Closure> cleanupClosures = []
    private Map<String, VFS> virtualFilesSystems = [:]

    static Set<Closure> mockedResources = []

    static List deletionMethods = [
            [CreateZoneAction.metaClass, CreateZoneAction.Result.metaClass, DeleteZoneAction.class],
            [AddCephBackupStorageAction.metaClass, AddCephBackupStorageAction.Result.metaClass, DeleteBackupStorageAction.class],
            [AddCephPrimaryStorageAction.metaClass, AddCephPrimaryStorageAction.Result.metaClass, DeletePrimaryStorageAction.class],
            [AddCephPrimaryStoragePoolAction.metaClass, AddCephPrimaryStoragePoolAction.Result.metaClass, DeleteCephPrimaryStoragePoolAction.class],
            [CreateEipAction.metaClass, CreateEipAction.Result.metaClass, DeleteEipAction.class],
            [CreateClusterAction.metaClass, CreateClusterAction.Result.metaClass, DeleteClusterAction.class],
            [CreateDiskOfferingAction.metaClass, CreateDiskOfferingAction.Result.metaClass, DeleteDiskOfferingAction.class],
            [CreateInstanceOfferingAction.metaClass, CreateInstanceOfferingAction.Result.metaClass, DeleteInstanceOfferingAction.class],
            [CreateAccountAction.metaClass, CreateAccountAction.Result.metaClass, DeleteAccountAction.class],
            [CreateRoleAction.metaClass, CreateRoleAction.Result.metaClass, { [new DeleteRoleAction(deleteMode: "Enforcing")] }],
            [AddImageAction.metaClass, AddImageAction.Result.metaClass, DeleteImageAction.class],
            [CreateDataVolumeTemplateFromVolumeAction.metaClass, CreateDataVolumeTemplateFromVolumeAction.Result.metaClass, DeleteImageAction.class],
            [CreateRootVolumeTemplateFromRootVolumeAction.metaClass, CreateRootVolumeTemplateFromRootVolumeAction.Result.metaClass, DeleteImageAction.class],
            [CreateL2NoVlanNetworkAction.metaClass, CreateL2NoVlanNetworkAction.Result.metaClass, DeleteL2NetworkAction.class],
            [CreateL2VlanNetworkAction.metaClass, CreateL2VlanNetworkAction.Result.metaClass, DeleteL2NetworkAction.class],
            [AddIpRangeByNetworkCidrAction.metaClass, AddIpRangeByNetworkCidrAction.Result.metaClass, DeleteIpRangeAction.class],
            [CreateL3NetworkAction.metaClass, CreateL3NetworkAction.Result.metaClass, DeleteL3NetworkAction.class],
            [CreateSchedulerJobAction.metaClass, CreateSchedulerJobAction.Result.metaClass, DeleteSchedulerJobAction.class],
            [CreateSchedulerTriggerAction.metaClass, CreateSchedulerTriggerAction.Result.metaClass, DeleteSchedulerTriggerAction.class],
            [CreateVmInstanceAction.metaClass, CreateVmInstanceAction.Result.metaClass, DestroyVmInstanceAction.class],
            [CreateDataVolumeFromVolumeSnapshotAction.metaClass, CreateDataVolumeFromVolumeSnapshotAction.Result.metaClass, DeleteDataVolumeAction.class],
            [CreateDataVolumeFromVolumeTemplateAction.metaClass, CreateDataVolumeFromVolumeTemplateAction.Result.metaClass, DeleteDataVolumeAction.class],
            [CreateDataVolumeAction.metaClass, CreateDataVolumeAction.Result.metaClass, DeleteDataVolumeAction.class],
            [CreateVolumeSnapshotGroupAction.metaClass, CreateVolumeSnapshotGroupAction.Result.metaClass, DeleteVolumeSnapshotGroupAction.class],
            [AddKVMHostAction.metaClass, AddKVMHostAction.Result.metaClass, DeleteHostAction.class],
            [CreateLoadBalancerAction.metaClass, CreateLoadBalancerAction.Result.metaClass, DeleteLoadBalancerAction.class],
            [AddLocalPrimaryStorageAction.metaClass, AddLocalPrimaryStorageAction.Result.metaClass, DeletePrimaryStorageAction.class],
            [AddImageStoreBackupStorageAction.metaClass, AddImageStoreBackupStorageAction.Result.metaClass, DeleteBackupStorageAction.class],
            [AddNfsPrimaryStorageAction.metaClass, AddNfsPrimaryStorageAction.Result.metaClass, DeletePrimaryStorageAction.class],
            [CreatePortForwardingRuleAction.metaClass, CreatePortForwardingRuleAction.Result.metaClass, DeletePortForwardingRuleAction.class],
            [CreateSecurityGroupAction.metaClass, CreateSecurityGroupAction.Result.metaClass, DeleteSecurityGroupAction.class],
            [AddSftpBackupStorageAction.metaClass, AddSftpBackupStorageAction.Result.metaClass, DeleteBackupStorageAction.class],
            [AddSharedMountPointPrimaryStorageAction.metaClass, AddSharedMountPointPrimaryStorageAction.Result.metaClass, DeletePrimaryStorageAction.class],
            [CreateVipAction.metaClass, CreateVipAction.Result.metaClass, DeleteVipAction.class],
            [CreateVirtualRouterOfferingAction.metaClass, CreateVirtualRouterOfferingAction.Result.metaClass, DeleteInstanceOfferingAction.class],
            [CreateWebhookAction.metaClass, CreateWebhookAction.Result.metaClass, DeleteWebhookAction.class],
            [AddLdapServerAction.metaClass, AddLdapServerAction.Result.metaClass, DeleteLdapServerAction.class],
            [CreateSNSEmailPlatformAction.metaClass, CreateSNSEmailPlatformAction.Result.metaClass, DeleteSNSApplicationPlatformAction.class],
            [CreateSNSEmailEndpointAction.metaClass, CreateSNSEmailEndpointAction.Result.metaClass, DeleteSNSApplicationEndpointAction.class],
            [CreateSNSTopicAction.metaClass, CreateSNSTopicAction.Result.metaClass, DeleteSNSTopicAction.class],
            [CreateAlarmAction.metaClass, CreateAlarmAction.Result.metaClass, DeleteAlarmAction.class],
            [SubscribeEventAction.metaClass, SubscribeEventAction.Result.metaClass, UnsubscribeEventAction.class],
            [CreateSNSHttpEndpointAction.metaClass, CreateSNSHttpEndpointAction.Result.metaClass, DeleteSNSApplicationEndpointAction.class],
            [CreateSNSDingTalkEndpointAction.metaClass, CreateSNSDingTalkEndpointAction.Result.metaClass, DeleteSNSApplicationEndpointAction.class],
            [CreateSNSTextTemplateAction.metaClass, CreateSNSTextTemplateAction.Result.metaClass, DeleteSNSTextTemplateAction.class],
            [CreateAliyunSmsSNSTextTemplateAction.metaClass, CreateAliyunSmsSNSTextTemplateAction.Result.metaClass, DeleteSNSTextTemplateAction.class],
            [CreateEmailMonitorTriggerActionAction.metaClass, CreateEmailMonitorTriggerActionAction.Result.metaClass, DeleteMonitorTriggerActionAction.class],
            [CreateEmailMediaAction.metaClass, CreateEmailMediaAction.Result.metaClass, DeleteMediaAction.class],
            [SubmitLongJobAction.metaClass, SubmitLongJobAction.Result.metaClass, DeleteLongJobAction.class],
            [UpdateClusterOSAction.metaClass, UpdateClusterOSAction.Result.metaClass, DeleteLongJobAction.class],
            [BatchCreateBaremetalChassisAction.metaClass, BatchCreateBaremetalChassisAction.Result.metaClass, DeleteLongJobAction.class],
            [AddSharedBlockGroupPrimaryStorageAction.metaClass, AddSharedBlockGroupPrimaryStorageAction.Result.metaClass, DeletePrimaryStorageAction.class],
            [CreateTagAction.metaClass, CreateTagAction.Result.metaClass, DeleteTagAction.class],
            [CreateResourcePriceAction.metaClass, CreateResourcePriceAction.Result.metaClass, DeleteResourcePriceAction.class],
            [CreatePriceTableAction.metaClass, CreatePriceTableAction.Result.metaClass, DeletePriceTableAction.class],
            [CreateMonitorGroupAction.metaClass, CreateMonitorGroupAction.Result.metaClass, DeleteMonitorGroupAction.class],
            [CreateMonitorTemplateAction.metaClass, CreateMonitorTemplateAction.Result.metaClass, DeleteMonitorTemplateAction.class],
            [CreateDirectoryAction.metaClass, CreateDirectoryAction.Result.metaClass, DeleteDirectoryAction.class],
            [CreateSNSSnmpPlatformAction.metaClass, CreateSNSSnmpPlatformAction.Result.metaClass, DeleteSNSApplicationPlatformAction.class],
            [CreateSNSSnmpEndpointAction.metaClass, CreateSNSSnmpEndpointAction.Result.metaClass, DeleteSNSApplicationEndpointAction.class],
            [CreateResourceAttributeKeyAction.metaClass, CreateResourceAttributeKeyAction.Result.metaClass, DeleteResourceAttributeKeyAction.class],
    ]

    static Closure GLOBAL_DELETE_HOOK
    static List<AllowedDBRemaining> allowedDBRemainingList = []
    static List<Pair<String, String>> skipResetGlobalConfigs = []

    protected ConcurrentLinkedQueue resourcesNeedDeletion = new ConcurrentLinkedQueue()

    static void addSkipResetGlobalConfig(String category, String name) {
        skipResetGlobalConfigs.add(new Pair<>(category, name))
    }

    static {
        BeanUtils.reflections.getSubTypesOf(AllowedDBRemaining.class).findAll { !Modifier.isAbstract(it.modifiers) }.each {
            allowedDBRemainingList.add(it.getConstructor().newInstance())
        }

        // Same as EnvPremiumSpec: initialized once, reject UpdateGlobalConfig (TELEMETRY.2003/2004).
        // Use literals so EnvSpec class init does not require loading telemetry classes.
        [
                "consent.granted.at",
                "source.id",
                "anonymization.salt",
        ].each { addSkipResetGlobalConfig("telemetry", it) }
    }

    protected void installDeletionMethods() {
        deletionMethods.each { it ->
            def (actionMeta, resultMeta, deleteClass) = it

            actionMeta.call = {
                ApiResult res = ZSClient.call(delegate)
                def ret = delegate.makeResult(res)
                Test.currentEnvSpec.resourcesNeedDeletion.add(ret)
                return ret
            }

            resultMeta.delete = {
                if (delegate.error != null || delegate.value.inventory == null) {
                    return false
                }
                String uuid = delegate.value.inventory.uuid

                List<Object> actions = []
                if (deleteClass instanceof Closure) {
                    actions.addAll((deleteClass as Closure).call())
                } else if (deleteClass instanceof List) {
                    actions.addAll((deleteClass as List<Class>).collect { it.getConstructor().newInstance() })
                } else {
                    actions.add((deleteClass as Class).getConstructor().newInstance())
                }

                actions.each { action ->
                    logger.debug("auto-deleting resource by ${action.class.name} uuid:${uuid}")
                    action.uuid = uuid
                    action.sessionId = session.uuid
                    def res = action.call()
                    assert res.error == null: "API failure: ${JSONObjectUtil.toJsonString(res.error)}"
                }
                return true
            }
        }
    }

    static {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory()
        factory.setReadTimeout(CoreGlobalProperty.REST_FACADE_READ_TIMEOUT)
        factory.setConnectTimeout(CoreGlobalProperty.REST_FACADE_CONNECT_TIMEOUT)
        restTemplate = new RestTemplate(factory)
    }

    EnvSpec() {
    }

    Closure getSimulator(String path) {
        return httpHandlers[path]
    }

    Closure getPostSimulator(String path) {
        return httpPostHandlers[path]
    }

    void cleanSimulatorHandlers() {
        httpPreHandlers.clear()
        httpHandlers.clear()
        httpHandlerCounters.clear()
        httpHandlers.putAll(defaultHttpHandlers)
    }

    void cleanAfterSimulatorHandlers() {
        httpPostHandlers.clear()
        httpPostHandlerCounters.clear()
        httpPostHandlers.putAll(defaultHttpPostHandlers)
    }

    void cleanFinalSimulatorHandlers() {
        httpFinalHandlers.clear()
    }

    void cleanMessageHandlers() {
        messageHandlers.clear()
        messageHandlerCounters.clear()
        messageHandlers.putAll(defaultMessageHandlers)
    }

    void cleanHttpConditionHandlers() {
        httpConditionHandlers.clear()
        httpConditionHandlers.putAll(defaultHttpConditionHandlers)
    }

    void cleanSimulatorAndMessageHandlers() {
        cleanSimulatorHandlers()
        cleanAfterSimulatorHandlers()
        cleanMessageHandlers()
        cleanHttpConditionHandlers()
        cleanFinalSimulatorHandlers()
    }

    void cleanStorageObjectMocks() {
        storageObjectSizeMocks.clear()
    }

    void identities(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = IdentitySpec.class) Closure c) {
        def ispec = new IdentitySpec(this)
        c.delegate = ispec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(ispec)
    }

    ZoneSpec zone(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ZoneSpec.class) Closure c)  {
        def zspec = new ZoneSpec(this)
        c.delegate = zspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        zones.add(zspec)
        addChild(zspec)
        return zspec
    }

    AccountSpec account(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = AccountSpec.class) Closure c) {
        def aspec = new AccountSpec(this)
        c.delegate = aspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(aspec)
        accounts.add(aspec)
        return aspec
    }

    InstanceOfferingSpec instanceOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = InstanceOfferingSpec.class) Closure c) {
        def spec = new InstanceOfferingSpec(this)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    BackupStorageSpec sftpBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SftpBackupStorageSpec.class) Closure c) {
        def spec = new SftpBackupStorageSpec(this)
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = spec
        c()
        addChild(spec)
        return spec
    }

    BackupStorageSpec cephBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = CephBackupStorageSpec.class) Closure c) {
        def spec = new CephBackupStorageSpec(this)
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = spec
        c()
        addChild(spec)
        return spec
    }

    DiskOfferingSpec diskOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = DiskOfferingSpec.class) Closure c) {
        def spec = new DiskOfferingSpec(this)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    VmSpec vm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = VmSpec.class) Closure c) {
        def spec = new VmSpec(this)
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    DataVolumeSpec volume(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = DataVolumeSpec.class) Closure c) {
        def i = new DataVolumeSpec(this)
        c.delegate = i
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(i)
        return i
    }

    LdapServerSpec ldapServer(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = LdapServerSpec.class) Closure c) {
        def i = new LdapServerSpec(this)
        c.delegate = i
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(i)
        return i
    }

    LdapVirtualEndpointSpec ldapEndpoint(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = LdapVirtualEndpointSpec.class) Closure c) {
        def i = new LdapVirtualEndpointSpec(this)
        c.delegate = i
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(i)
        return i
    }

    VOpsVirtualEndpointSpec vops(
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = VOpsVirtualEndpointSpec.class) Closure c) {
        def subSpec = new VOpsVirtualEndpointSpec(this)
        c.delegate = subSpec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(subSpec)
        return subSpec
    }

    TelemetryVirtualEndpointSpec telemetryEndpoint(
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = TelemetryVirtualEndpointSpec.class) Closure c) {
        def subSpec = new TelemetryVirtualEndpointSpec(this)
        c.delegate = subSpec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(subSpec)
        return subSpec
    }

    void adminLogin() {
        session = login(AccountConstant.INITIAL_SYSTEM_ADMIN_NAME, AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD)
    }

    SessionInventory login(String accountName, String password) {
        LogInByAccountAction a = new LogInByAccountAction()
        a.accountName = accountName
        a.password = password
        def res = a.call()
        assert res.error == null : "Login failure: ${JSONObjectUtil.toJsonString(res.error)}"
        return res.value.inventory
    }

    def specByUuid(String uuid) {
        return specsByUuid[uuid]
    }

    def specByName(String name) {
        return specsByName[name]
    }

    def inventoryByName(String name) {
        def spec = specByName(name)

        assert spec != null : "cannot find spec[${name}]"
        assert spec.hasProperty("inventory"): "${spec.class} doesn't have inventory"
        return spec.inventory
    }

    protected String retrieveSessionUuid(Node it) {
        String suuid = session.uuid

        if (it instanceof HasSession) {
            if (it.accountName != null) {
                AccountSpec aspec = find(it.accountName, AccountSpec.class)
                assert aspec != null: "cannot find the account[$it.accountName] defined in environment()"
                suuid = aspec.session.uuid
            } else {
                def n = it.parent
                while (n != null) {
                    if (n instanceof AccountSpec) {
                        suuid = n.session.uuid
                        break
                    } else if (!(n instanceof HasSession) || n.accountName == null) {
                        n = n.parent
                    } else {
                        // one of the parent has the accountName set, use it
                        AccountSpec aspec = find(n.accountName, AccountSpec.class)
                        assert aspec != null: "cannot find the account[$n.accountName] defined in environment()"
                        suuid = aspec.session.uuid
                        break
                    }
                }
            }
        }

        return suuid
    }

    protected void deploy() {
        logger.debug("Deploy EnvSpec started")
        def allNodes = []

        walk {
            if (it instanceof CreateAction) {
                it.preOperations.each { it() }
            }

            allNodes.add(it)
        }

        Set<Node> resolvedNodes = new LinkedHashSet<>()
        allNodes.each {
            resolveDependency(it as Node, resolvedNodes, [])
        }

        def names = resolvedNodes.collect { sn ->
            return sn.hasProperty("name") ? sn.name : sn.toString()
        }

        logger.debug("deploying path: ${names.join(" --> ")} ")

        resolvedNodes.each {
            if (!(it instanceof CreateAction)) {
                return
            }

            it.beforeOperations.each { cl -> cl() }

            def uuid = Platform.getUuid()
            specsByUuid[uuid] = it

            Spec s = it as Spec
            def suuid = s.getSessionUuid == null ? retrieveSessionUuid(it) : s.getSessionUuid()

            try {
                logger.debug(String.format("create resource of class %s", it.getClass().getName()))
                def id = (it as CreateAction).create(uuid, suuid) as SpecID

                if ((it as Spec).toPublic) {
                    shareResource {
                        resourceUuids = [id.uuid]
                        toPublic = true
                    }
                }

                if (id != null) {
                    specsByName[id.name] = it
                }
            } catch (Throwable t) {
                String name = null
                if (it.hasProperty("name")) {
                    name = it.name
                } else {
                    // the node doesn't have a name, use its parent name + its class name
                    Node n = it
                    while (n != null) {
                        if (n.hasProperty("name")) {
                            name = "${n.name}->${it.class.simpleName}"
                            break
                        }

                        n = n.parent
                    }
                }

                throw new Exception("failed to create a spec[name: $name, spec type: ${it.class.simpleName}], ${t.message}", t)
            }

            it.afterOperations.each { cl -> cl() }
        }

        allNodes.each {
            if (it instanceof CreateAction) {
                it.postOperations.each { it() }
            }
        }

        logger.debug("Deploy EnvSpec finished")
    }

    private List<GlobalConfigInventory> getChangedConfig() {
        List<GlobalConfigInventory> results = new ArrayList<>()
        def a = new QueryGlobalConfigAction()
        a.sessionId = session.uuid
        a.limit = 500
        a.start = 0
        while (true) {
            QueryGlobalConfigAction.Result res = a.call()
            assert res.error == null: res.error.toString()
            List<GlobalConfigInventory> changed = res.value.inventories.stream()
                    .filter({con -> con.value != con.defaultValue})
                    .collect(Collectors.toList())
            results.addAll(changed)
            a.start += 500

            if (res.value.inventories.size() < 500) {
                break
            }
        }

        return results
    }

    boolean shouldSkipResetGlobalConfig(GlobalConfigInventory config) {
        return skipResetGlobalConfigs.any { it.first() == config.category && it.second() == config.name }
    }

    void resetAllGlobalConfig() {
        logger.debug("Reset all global config started")
        List<GlobalConfigInventory> changedConfigs = getChangedConfig()
        List<GlobalConfigInventory> skippedConfigs = changedConfigs.findAll { shouldSkipResetGlobalConfig(it) }
        if (!skippedConfigs.isEmpty()) {
            logger.debug("Skip reset global configs: ${skippedConfigs.collect { "${it.category} - ${it.name}" }.join(", ")}")
        }
        CountDownLatch latch = new CountDownLatch(1)
        List<ErrorCode> errors = []
        new While<>(changedConfigs.findAll { !shouldSkipResetGlobalConfig(it) }).each(new While.Do<GlobalConfigInventory>() {
            @Override
            void accept(GlobalConfigInventory config, WhileCompletion completion) {
                def ua = new UpdateGlobalConfigAction()
                ua.category = config.category
                ua.name = config.name
                ua.value = config.defaultValue
                ua.sessionId = session.uuid
                ua.call { UpdateGlobalConfigAction.Result r ->
                    if (r.error != null) {
                        errors.add(r.error)
                    }

                    completion.done()
                }
            }
        }).run(new WhileDoneCompletion(null) {
            @Override
            void done(org.zstack.header.errorcode.ErrorCodeList errorCodeList) {
                latch.countDown()
            }
        })

        def ret = latch.await(1, TimeUnit.MINUTES)
        if (!ret) {
            DebugUtils.dumpAllThreads()
        }

        assert ret: "global configs not all updated after 1 minutes timeout"
        assert errors.isEmpty(): "some global configs fail to update, see ${errors.collect {JSONObjectUtil.toJsonString(it)}}"

        logger.debug("Reset all global config finished")
    }

    def recreate(String specName) {
        def spec = specByName(specName)
        assert spec != null: "cannot find the spec[name:$specName]"

        walkNode(spec) {
            if (!(it instanceof CreateAction)) {
                return
            }

            String uuid = Platform.getUuid()
            specsByUuid[uuid] = it

            SpecID id = it.create(uuid, retrieveSessionUuid(it as Node))
            if (id != null) {
                specsByName[id.name] = it
            }
        }

        return spec
    }

    protected void initializeMockedResource() {
        logger.debug("Initialize mocked resource")

        mockedResources.each {
            it(this)
        }

        logger.debug("Initialize mocked resource finished")
    }

    protected void installSimulatorHandlers() {
        logger.debug("Install simulator handlers started")
        simulatorClasses.each { clz ->
            def con = clz.getConstructors()[0]

            Simulator sim
            if (con.getParameterCount() == 0) {
                sim = con.newInstance() as Simulator
            } else {
                Object[] params = new Objects[con.getParameterCount()]
                for (int i=0; i<con.getParameterCount(); i++) {
                    params[i] = null
                }

                sim = con.newInstance(params) as Simulator
            }

            sim.registerSimulators(this)
        }
        logger.debug("Install simulator handlers finished")
    }

    synchronized VFS getVirtualFileSystem(String identity, boolean errorOnNotExisting = false) {
        VFS vfs = virtualFilesSystems[identity]
        if (errorOnNotExisting && vfs == null) {
            throw new Exception("VFS[id:${identity}] not existing")
        } else if (vfs == null) {
            vfs = new VFS(identity)
            virtualFilesSystems[identity] = vfs
            logger.debug("new VFS[id: ${vfs.id}] created")
        }

        return vfs
    }

    EnvSpec create(Closure cl = null) {
        assert Test.currentEnvSpec == null: "There is another EnvSpec created but not deleted. There can be only one EnvSpec" +
                " in used, you must delete the previous one"

        hasCreated = true
        Test.currentEnvSpec = this
        def startEnvCreate = System.currentTimeMillis()

        adminLogin()
        resetAllGlobalConfig()

        initializeMockedResource()
        installSimulatorHandlers()
        installDeletionMethods()

        /*
        simulatorClasses.each {
            Simulator sim = it.newInstance() as Simulator
            sim.registerSimulators(this)
        }
        */

        deploy()

        defaultHttpHandlers = [:]
        defaultHttpHandlers.putAll(httpHandlers)
        defaultHttpPostHandlers = [:]
        defaultHttpPostHandlers.putAll(httpPostHandlers)
        defaultMessageHandlers = [:]
        defaultMessageHandlers.putAll(messageHandlers)
        defaultHttpConditionHandlers = [:]
        defaultHttpConditionHandlers.putAll(httpConditionHandlers)

        Test.envCreateTime += System.currentTimeMillis() - startEnvCreate

        if (cl != null) {
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
        }

        return this
    }

    private void makeSureAllEntitiesDeleted() {
        DatabaseFacadeImpl dbf = Test.componentLoader.getComponent(DatabaseFacadeImpl.class)
        def entityTypes = dbf.entityManager.metamodel.entities
        entityTypes.each { type ->
            if (type.name in ["ManagementNodeVO", "SessionVO",
                              "GlobalConfigVO", "AsyncRestVO",
                              "AccountVO", "NetworkServiceProviderVO",
                              "NetworkServiceTypeVO", "VmInstanceSequenceNumberVO",
                              "BaremetalInstanceSequenceNumberVO", "BaremetalImageCacheVO",
                              "GarbageCollectorVO",
                              "GuestOsCategoryVO",
                              "TaskProgressVO", "TaskStepVO",
                              "ResourceVO","SecurityGroupSequenceNumberVO", "MediaVO",
                              "CaptchaVO", "LoginAttemptsVO", "SchedulerJobHistoryVO",
                              "HistoricalPasswordVO", "InstallPathRecycleVO",
                              "PortMirrorSessionSequenceNumberVO", "LicenseHistoryVO", "EventLogVO", "VmSchedHistoryVO",
                              "EventRecordsVO", "AuditsVO", "AlarmRecordsVO", "VmCrashHistoryVO", "EncryptionIntegrityVO", "FileIntegrityVerificationVO",
                              "EncryptEntityMetadataVO", "VmInstanceResourceMetadataGroupVO", "HostOsCategoryVO", "KvmHostHypervisorMetadataVO",
                              "HaStrategyConditionVO", "SystemTagVO", "ConsoleProxyAgentVO", "ConsoleProxyVO"]) {
                // those tables will continue having entries during running a test suite
                return
            }

            long count = SQL.New("select count(*) from ${type.name}".toString(), Long.class).find()

            if (count > 0) {
                Class voClz = dbf.entityInfoMap.keySet().find { it.simpleName == type.name }
                assert voClz != null: "cannot find the entity[${type.name}]"

                List vos = SQL.New("select a from ${type.name} a".toString(), voClz).list()

                for (AllowedDBRemaining a : allowedDBRemainingList) {
                    logger.debug("perform AllowedDBRemaining[${a.class}] check")
                    vos = a.check(type.name, vos)
                    if (vos.isEmpty()) {
                        // the remaining rows are allowed by test
                        return
                    }
                }

                List lst = vos.collect { it.getProperties() }

                def err = "[${Test.CURRENT_SUB_CASE != null ? Test.CURRENT_SUB_CASE.class : this.class}] EnvSpec.delete() didn't cleanup the environment, there are still ${vos.size()} records in the database" +
                        " table ${type.name}, go fix it immediately!!! Abort the system\n ${lst}"
                logger.fatal(err)

                // abort the test suite
                throw new StopTestSuiteException()
            }
        }
    }

    class TraverseCleanEO {
        Set<String> allNodes
        List<Pair<String, String>> allLinks
        HashMap<String, Boolean> execMap
        DatabaseFacade dbf
        Map<String, Class> eoSimpleNameEOClassMap
        Map<String, Class> eoSimpleNameVOClassMap

        TraverseCleanEO(List<Pair<String, String>> links,
                        Set<String> nodes,
                        Map<String, Class> eoNameEOClassMap,
                        Map<String, Class> eoNameVOClassMap) {
            eoSimpleNameEOClassMap = eoNameEOClassMap
            eoSimpleNameVOClassMap = eoNameVOClassMap
            dbf = Test.componentLoader.getComponent(DatabaseFacade.class)
            allNodes = nodes
            allLinks = new ArrayList<>()
            links.forEach { it ->
                logger.debug(String.format("cleanupEO->link:%s->%s", it.first(), it.second()))
                if (nodes.contains(it.first()) && nodes.contains(it.second())) {
                    allLinks.add(it)
                    logger.debug(String.format("cleanupEO->add link:%s->%s", it.first(), it.second()))
                }
            }
        }

        void traverse() {
            execMap = new HashMap<>()
            allNodes.forEach { it -> execMap.put(it, false) }

            for (String allLinkNode : allNodes) {
                process(allLinkNode, 0, new ArrayList<String>())
            }
        }

        private void process(String current, int depth, List<String> history) {
            if (execMap.get(current)) {
                return
            }

            for (Pair<String, String> p : allLinks) {
                if (p.first() == current
                        && p.first() != p.second()
                        && !execMap.get(p.second())) {
                    List<String> forkHistory = new ArrayList<String>()
                    forkHistory.addAll(history)
                    forkHistory.add(current)
                    process(p.second(), depth + 1, forkHistory)
                }
            }

            if (!execMap.get(current)) {
                history.add(current)
                logger.debug("cleanupEO:" + current +
                        ", depth:" + depth +
                        ", history: " + history.join("->"))
                dbf.eoCleanup(eoSimpleNameVOClassMap.get(current))
                execMap.put(current, true)
            }
        }
    }

    private void cleanupEO() {
        SqlForeignKeyGenerator g = new SqlForeignKeyGenerator()

        def vos = Platform.reflections.getTypesAnnotatedWith(EO.class).findAll { it.isAnnotationPresent(EO.class) }
        logger.debug(String.format("cleanupEO->clean targets(%s): %s", vos.size(), vos.toString()))
        Map<String, Class> eoNameEOClassMap = new HashMap<>()
        Map<String, Class> eoNameVOClassMap = new HashMap<>()
        Set<String> nodes = new HashSet<>()
        vos.forEach { it ->
            EO at = (EO) it.getAnnotation(EO.class)
            if (at != null) {
                Class eoClass = at.EOClazz()
                nodes.add(eoClass.getSimpleName())
                eoNameEOClassMap.put(eoClass.getSimpleName(), eoClass)
                eoNameVOClassMap.put(eoClass.getSimpleName(), it)
            }
        }

        logger.debug(String.format("cleanupEO->clean targets(%s): %s", eoNameEOClassMap.size(), eoNameEOClassMap.toString()))
        logger.debug(String.format("cleanupEO->all nodes(%s): %s", nodes.size(), nodes.toString()))

        new TraverseCleanEO(g.generateEORelations(), nodes, eoNameEOClassMap, eoNameVOClassMap).traverse()
    }

    protected void callDeleteOnResourcesNeedDeletion() {
        resourcesNeedDeletion.each {
            logger.info("run delete() method on ${it.class}")
            it.delete()
        }
    }

    void delete() {
        try {
            adminLogin()

            ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString())
            VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString())
            VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct.toString())

            cleanupClosures.each { it() }
            cleanSimulatorAndMessageHandlers()
            cleanStorageObjectMocks()

            if (session != null) {
                destroy(session.uuid)
            }

            callDeleteOnResourcesNeedDeletion()

            SQL.New(EventLogVO.class).hardDelete()
            SQL.New(VmSchedHistoryVO).hardDelete()
            SQL.New(SessionVO.class).hardDelete()

            try {
                def progressService = Platform.getComponentLoader().getComponent(ActionProgressService.class)
                progressService.clearCache()
            } catch (Exception ignored) {}
            SQL.New(TaskProgressVO.class).hardDelete()

            if (GLOBAL_DELETE_HOOK != null) {
                GLOBAL_DELETE_HOOK()
            }

            cleanupEO()
            makeSureAllEntitiesDeleted()

            testter.clearAll()
            virtualFilesSystems.values().each { it.destroy() }
        } catch (StopTestSuiteException e) {
            throw e
        } catch (Throwable t) {
            logger.fatal("an error happened when running EnvSpec.delete() for" +
                    " the case ${Test.CURRENT_SUB_CASE?.class}, we must stop the test suite, ${t.getMessage()}", t)
            throw new StopTestSuiteException(t)
        } finally {
            // set the currentEnvSpec to null anyway
            // because the current sub case may fail but
            // it should not effect the following cases
            Test.currentEnvSpec = null
        }
    }

    EnvSpec copy() {
        assert !hasCreated: "copy() can not be called after the create() is called"

        def n = new EnvSpec()
        InvokerHelper.setProperties(n, this.properties)
        return n
    }

    private void replyHttpCall(HttpEntity<String> entity, HttpServletResponse response, Object rsp) {
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

        new Retry<ResponseEntity<String>>() {
            @Override
            @RetryCondition(onExceptions = org.springframework.web.client.ResourceAccessException.class)
            protected ResponseEntity<String> call() {
                restTemplate.exchange(callbackUrl, HttpMethod.POST, rreq, String.class)
            }
        }.run();
    }

    void sizeMock(String installPath, Long size) {
        storageObjectSizeMocks.put(installPath, size)
    }

    Long getMockSize(String installPath) {
        return storageObjectSizeMocks.get(installPath, 0L)
    }

    void preSimulator(String path, Closure c) {
        httpPreHandlers[path] = c
    }

    void simulator(String path, Closure c) {
        httpHandlers[path] = c
    }

    void afterSimulator(String path, Closure c) {
        httpPostHandlers[path] = c
    }

    void hijackSimulator(String path, Closure c) {
        httpFinalHandlers[path] = c
    }

    void conditionSimulator(String path, Closure condition, Closure c) {
        def lst = httpConditionHandlers[path]
        if (lst == null) {
            lst = []
            httpConditionHandlers[path] = lst
        } else {
            // deduplication
            def ele = lst.find { it -> it.get(0) == condition }
            if (ele != null) {
                lst.remove(ele)
            }
        }

        lst.add(new Tuple(condition, c))
    }

    static void installMockedResourceToInitialize(Closure c) {
        mockedResources.add(c)
    }

    void mockFactory(Class clz, Closure c) {
        Test.functionForMockTestObjectFactory.put(clz, c)
        cleanupClosures.add({ Test.functionForMockTestObjectFactory.remove(clz) })
    }

    Closure conditionHandler(List<Tuple> handlers, HttpEntity entity, HttpServletRequest req) {
        Closure handler
        handlers.each {
            if (it.get(0) == null) {
                handler = it.get(1)
                return
            }
        }

        if (handler != null) {
            return (Closure)handler
        }

        handlers.each {
            Closure cond = it.get(0)

            if (cond != null && cond(entity)) {
                handler = it.get(1)
                return
            }
        }

        return handler == null ? null : (Closure)handler
    }

    HttpEntity<String> getEntityFromRequest(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder()
        String line
        while ((line = req.getReader().readLine()) != null) {
            sb.append(line)
        }
        req.getReader().close()

        HttpHeaders header = new HttpHeaders()
        for (Enumeration e = req.getHeaderNames() ; e.hasMoreElements() ;) {
            String name = e.nextElement().toString()
            header.add(name, req.getHeader(name))
        }
        return new HttpEntity<String>(sb.toString(), header)
    }

    void handleConditionSimulatorHttpRequests(HttpServletRequest req, HttpEntity entity, HttpServletResponse rsp) {
        def url = req.getRequestURI()
        if (httpConditionHandlers[url] == null || httpConditionHandlers[url].isEmpty()) {
            rsp.sendError(HttpStatus.NOT_FOUND.value(), "no handler found for the path $url")
            return
        }
        def handler = conditionHandler(httpConditionHandlers[url], entity, req)

        handleNoConditionSimulatorHttpRequests(req, handler, entity, rsp)
    }

    void handleNoConditionSimulatorHttpRequests(HttpServletRequest req, Closure handler, HttpEntity entity, HttpServletResponse rsp) {
        def url = req.getRequestURI()

        if (handler == null) {
            def warning = "cannot find handlers for[$url] satisfied for their conditions"
            logger.warn(warning)
            rsp.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), warning)
            return
        }

        try {
            def ret
            Closure preHandler = httpPreHandlers[url]
            if (preHandler == null) {
                for (String httpUrl : httpPreHandlers.keys()) {
                    if (Pattern.matches(httpUrl, url)) {
                        preHandler = httpPreHandlers.get(httpUrl)
                        break
                    }
                }
            }

            if (preHandler != null) {
                if (preHandler.maximumNumberOfParameters == 0) {
                    preHandler()
                } else if (preHandler.maximumNumberOfParameters == 1) {
                    preHandler(entity)
                } else {
                    preHandler(entity, this)
                }
            }

            if (handler.maximumNumberOfParameters == 0) {
                ret = handler()
            } else if (handler.maximumNumberOfParameters == 1) {
                ret = handler(entity)
            } else if (handler.maximumNumberOfParameters == 3) {
                ret = handler(req, entity, this)
            } else {
                ret = handler(entity, this)
            }

            Closure postHandler = httpPostHandlers[url]

            if (postHandler == null) {
                for (String httpUrl : httpPostHandlers.keys()) {
                    if (Pattern.matches(httpUrl, url)) {
                        postHandler = httpPostHandlers.get(httpUrl)
                        break
                    }
                }
            }

            if (postHandler != null) {
                if (postHandler.maximumNumberOfParameters <= 1) {
                    ret = postHandler(ret)
                } else if (postHandler.maximumNumberOfParameters == 2) {
                    ret = postHandler(ret, entity)
                } else {
                    ret = postHandler(ret, entity, this)
                }

                httpPostHandlerCounters.putIfAbsent(url, new AtomicInteger(0))
                httpPostHandlerCounters.get(url).incrementAndGet()
            }

            Closure finalHandler = httpFinalHandlers[url]

            if (finalHandler == null) {
                for (String httpUrl : httpFinalHandlers.keys()) {
                    if (Pattern.matches(httpUrl, url)) {
                        finalHandler = httpFinalHandlers.get(httpUrl)
                        break
                    }
                }
            }

            if (finalHandler != null) {
                if (finalHandler.maximumNumberOfParameters <= 1) {
                    ret = finalHandler(ret)
                } else if (finalHandler.maximumNumberOfParameters == 2) {
                    ret = finalHandler(ret, entity)
                } else {
                    ret = finalHandler(ret, entity, this)
                }
            }

            if (ret == null) {
                ret = [:]
            }

            replyHttpCall(entity, rsp, ret)
        } catch (HttpError he) {
            logger.warn("the simulator[$url] reports a http error[status code:${he.status}, message:${he.message}]", he)
            rsp.sendError(he.status, he.message)
        } catch (Throwable t) {
            logger.warn("error happened when handling $url", t)
            rsp.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.message)
        } finally {
            httpHandlerCounters.putIfAbsent(url, new AtomicInteger(0))
            httpHandlerCounters.get(url).incrementAndGet()
        }
    }

    void handleSimulatorHttpRequests(HttpServletRequest req, HttpServletResponse rsp) {
        def url = req.getRequestURI()
        def entity = getEntityFromRequest(req)
        def handler = httpHandlers[url]

        if (handler == null) {
            for (String httpUrl : httpHandlers.keys()) {
                if (Pattern.matches(httpUrl, url)) {
                    handler = httpHandlers.get(httpUrl)
                    break
                }
            }
        }

        if (handler == null) {
            handleConditionSimulatorHttpRequests(req, entity, rsp)
        } else {
            handleNoConditionSimulatorHttpRequests(req, handler, entity, rsp)
        }
    }

    void message(Class<? extends Message> msgClz, Closure condition, Closure handler) {
        def lst = messageHandlers[(msgClz)]
        if (lst == null) {
            lst = []
            messageHandlers[(msgClz)] = lst
        } else {
            // deduplication
            def ele = lst.find { it -> it.get(0) == condition }
            if (ele != null) {
                lst.remove(ele)
            }
        }

        lst.add(new Tuple(condition, handler))
    }

    void message(Class<? extends Message> msgClz, Closure handler) {
        message(msgClz, null, handler)
    }

    void revokeMessage(Class<? extends Message> msgClz, Closure condition){
        def lst = messageHandlers[(msgClz)]
        if (lst == null) {
            return
        }

        def ele = lst.find { it -> it.get(0) == condition }
        if (ele != null) {
            lst.remove(ele)
        }
    }

    EnvSpec more(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = EnvSpec.class) Closure c) {
        c.delegate = this
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
    }

    int getAfterSimulatorSize(String path) {
        return httpPostHandlerCounters[path] == null ? 0 : httpPostHandlerCounters[path].intValue()
    }

    int getSimulatorSize(String path) {
        return httpHandlerCounters[path] == null ? 0 : httpHandlerCounters[path].intValue()
    }

    int getMessageSize(Class clz) {
        return messageHandlerCounters[clz] == null ? 0 : messageHandlerCounters[clz].intValue()
    }

    boolean verifyAfterSimulator(String path, int times) {
        return httpPostHandlerCounters[path] == null ? times == 0 : httpPostHandlerCounters[path].intValue() == times
    }

    boolean verifySimulator(String path, int times) {
        return httpHandlerCounters[path] == null ? times == 0 : httpHandlerCounters[path].intValue() == times
    }

    boolean verifyMessage(Class clz, int times) {
        return messageHandlerCounters[clz] == null ? times == 0 : messageHandlerCounters[clz].intValue() == times
    }

    boolean verifyAfterSimulator(String path) {
        return httpPostHandlerCounters[path] != null && httpPostHandlerCounters[path].intValue() > 0
    }

    boolean verifySimulator(String path) {
        return httpHandlerCounters[path] != null && httpHandlerCounters[path].intValue() > 0
    }

    boolean verifyMessage(Class clz) {
        return messageHandlerCounters[clz] != null && messageHandlerCounters[clz].intValue() > 0
    }

    void resetAllSimulatorSize() {
        httpPostHandlerCounters.clear()
        httpHandlerCounters.clear()
    }

    void resetAllMessageSize() {
        messageHandlerCounters.clear()
    }
}