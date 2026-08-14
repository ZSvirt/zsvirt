package org.zstack.zmigrate.client;

import com.google.gson.annotations.SerializedName;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusGlobalProperty;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.RestHttp;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zmigrate.ZMigrateGlobalConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static org.zstack.core.Platform.err;
import static org.zstack.zmigrate.ZMigrateConstant.*;
import static org.zstack.zmigrate.ZMigratePluginErrors.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ZMigrateHttpClient {
    private static final CLogger logger = Utils.getLogger(ZMigrateHttpClient.class);

    // 8 retries + 1 initial = 9 total attempts
    private static final int VERIFY_PLATFORM_CONNECTION_RETRY_TIMES = 8;
    private static final int VERIFY_PLATFORM_CONNECTION_RETRY_INTERVAL_SECONDS = 30;
    private static final int VERIFY_GATEWAY_CONNECTION_RETRY_TIMES = 6;
    private static final int VERIFY_GATEWAY_CONNECTION_RETRY_INTERVAL_SECONDS = 30;

    private static final int DEFAULT_RETRY_TIMES = 3;
    private static final int DEFAULT_RETRY_INTERVAL_SECONDS = 10;

    private static final String ZMIGRATE_NO_DATA_RESPONSE = "false";
    private static final String EMPTY_JSON_ARRAY = "[]";

    private final long timeoutInMillisForGetter = TimeUnit.SECONDS.toMillis(30);
    private final long timeoutInMillisForGetLicenses = TimeUnit.SECONDS.toMillis(10);

    @Autowired
    private RESTFacade restFacade;
    @Autowired
    protected ErrorFacade errorFacade;
    @Autowired
    protected CloudBus bus;


    protected <T> RestHttp<T> http(Class<T> returnClass) {
        RestHttp<T> http = restFacade.http(returnClass);

        BiFunction<Exception, RestHttp<T>, ErrorCode> raw = http.getErrorCodeBuilder();
        http.withErrorCodeBuilder((e, http2) -> {
            ErrorCode first = raw.apply(e, http2);
            if (first == null) {
                return errorFacade.throwableToOperationError(e);
            }
            return first;
        });
        return http;
    }

    private ZMigrateSshClient createSshClient() {
        return Platform.New(ZMigrateSshClient::new);
    }

    private ErrorableValue<String> getEncryptKey() {
        final ErrorableValue<String> res = createSshClient().buildEncryptKey();
        if (!res.isSuccess()) {
            return err(ZMIGRATE_ENCRYPT_KEY_ERROR, "failed to build encrypt key for ZMigrate").toErrorableValue();
        }
        return res;
    }

    private String cachedManagementIp;

    private ErrorableValue<String> resolveManagementIp(ErrorCode errorTemplate) {
        if (cachedManagementIp != null) {
            return ErrorableValue.of(cachedManagementIp);
        }
        ErrorableValue<String> ip = ZMigrateGatewayHelper.getGatewayManagementIp();
        if (!ip.isSuccess()) {
            return errorTemplate.withCause(ip.error).toErrorableValue();
        }
        cachedManagementIp = ip.result;
        return ErrorableValue.of(cachedManagementIp);
    }

    public ErrorableValue<Boolean> createAccount() {
        return retryWithFreshContext(
                DEFAULT_RETRY_TIMES, DEFAULT_RETRY_INTERVAL_SECONDS,
                "createAccount",
                err(ZMIGRATE_CREATE_ACCOUNT_ERROR, "failed to create user in ZMigrate"),
                (managementIp, encryptKey) -> {
                    CreateAccountCmd cmd = new CreateAccountCmd(encryptKey);
                    ErrorableValue<String> response = http(String.class)
                            .withPath(String.format("https://%s:%d%s", managementIp, GATEWAY_RESTFUL_API_PORT, ZMIGRATE_IDENTITY_MANAGEMENT_PATH))
                            .withBodyJson(cmd)
                            .withTimeoutInMillis(timeoutInMillisForGetter)
                            .withoutRetry()
                            .postWithErrorCode();
                    if (!response.isSuccess() || "false".equals(response.result)) {
                        return err(ZMIGRATE_CREATE_ACCOUNT_ERROR, "failed to create user in ZMigrate").toErrorableValue();
                    }
                    return ErrorableValue.of(true);
                });
    }

    /**
     * Retry an action with a fresh encryptKey on each attempt (key expires in 15s).
     * managementIp is resolved once and cached (stable).
     */
    private <T> ErrorableValue<T> retryWithFreshContext(
            int retryTimes, int retryIntervalSeconds,
            String methodName, ErrorCode failureError,
            BiFunction<String, String, ErrorableValue<T>> action) {
        ErrorableValue<String> ip = resolveManagementIp(failureError);
        if (!ip.isSuccess()) {
            return ip.error.toErrorableValue();
        }
        String managementIp = ip.result;
        ErrorCode lastError = null;

        for (int i = 0; i <= retryTimes; i++) {
            if (i > 0) {
                try {
                    TimeUnit.SECONDS.sleep(retryIntervalSeconds);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            ErrorableValue<String> encryptKey = getEncryptKey();
            if (!encryptKey.isSuccess()) {
                logger.warn(String.format("%s: attempt %d/%d failed to get encrypt key",
                        methodName, i + 1, retryTimes + 1));
                lastError = encryptKey.error;
                continue;
            }

            ErrorableValue<T> result = action.apply(managementIp, encryptKey.result);
            if (result.isSuccess()) {
                return result;
            }

            logger.warn(String.format("%s: attempt %d/%d failed, error: %s",
                    methodName, i + 1, retryTimes + 1, result.error));
            lastError = result.error;
        }

        logger.error(String.format("%s: all %d attempts failed", methodName, retryTimes + 1));
        return ErrorableValue.ofErrorCode(lastError != null ? failureError.withCause(lastError) : failureError);
    }

    public ErrorableValue<Boolean> verifyPlatformConnection(String mnIpToAddZMigrate, String accessKeyID, String accessKeySecret) {
        return retryWithFreshContext(
                VERIFY_PLATFORM_CONNECTION_RETRY_TIMES,
                VERIFY_PLATFORM_CONNECTION_RETRY_INTERVAL_SECONDS,
                "verifyPlatformConnection",
                err(ZMIGRATE_VERIFY_PLATFORM_CONNECTION_ERROR, "failed to verify platform connection from ZMigrate"),
                (managementIp, encryptKey) -> {
                    VerifyPlatformConnectionCmd cmd = new VerifyPlatformConnectionCmd(encryptKey);
                    cmd.identityPort = String.valueOf(CloudBusGlobalProperty.HTTP_PORT);
                    cmd.identityProtocol = PLATFORM_IDENTITY_PROTOCOL_TYPE;
                    cmd.endPoint = mnIpToAddZMigrate;
                    cmd.accessType = ZMIGRATE_ACCESS_TYPE;
                    cmd.accessKeyID = accessKeyID;
                    cmd.accessKeySecrete = accessKeySecret;
                    ErrorableValue<VerifyPlatformConnectionResponse> response = http(VerifyPlatformConnectionResponse.class)
                            .withPath(String.format("https://%s:%d%s", managementIp, GATEWAY_RESTFUL_API_PORT, ZMIGRATE_ZSPHERE_WEB_SERVICES_PATH))
                            .withBodyJson(cmd)
                            .withTimeoutInMillis(timeoutInMillisForGetter)
                            .withoutRetry()
                            .postWithErrorCode();
                    if (response.isSuccess() && response.result != null && !"false".equals(response.result.code)) {
                        return ErrorableValue.of(true);
                    }
                    return err(ZMIGRATE_VERIFY_PLATFORM_CONNECTION_ERROR,
                            response.isSuccess() ? "response code is false" : String.valueOf(response.error))
                            .toErrorableValue();
                });
    }

    public ErrorableValue<RegisterZsvToZMigratePlatformResponse> registerZsvToZMigrate(String mnIpToAddZMigrate, String accessKeyID, String accessKeySecret) {
        return retryWithFreshContext(
                DEFAULT_RETRY_TIMES, DEFAULT_RETRY_INTERVAL_SECONDS,
                "registerZsvToZMigrate",
                err(ZMIGRATE_REGISTER_ZSV_TO_ZMIGRATE_ERROR, "failed to register ZSV to ZMigrate"),
                (managementIp, encryptKey) -> {
                    RegisterZsvToZMigratePlatformCmd cmd = new RegisterZsvToZMigratePlatformCmd(encryptKey);
                    cmd.accessType = ZMIGRATE_ACCESS_TYPE;
                    cmd.accessKeyID = accessKeyID;
                    cmd.accessKeySecrete = accessKeySecret;
                    cmd.displayName = getZsvPlatformDisplayNameOnZMigrate();
                    cmd.identityPort = String.valueOf(CloudBusGlobalProperty.HTTP_PORT);
                    cmd.identityProtocol = PLATFORM_IDENTITY_PROTOCOL_TYPE;
                    cmd.endPoint = mnIpToAddZMigrate;
                    cmd.timezoneOffset = String.valueOf(TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000);
                    ErrorableValue<String> response = http(String.class)
                            .withPath(String.format("https://%s:%d%s", managementIp, GATEWAY_RESTFUL_API_PORT, ZMIGRATE_ZSPHERE_WEB_SERVICES_PATH))
                            .withBodyJson(cmd)
                            .withTimeoutInMillis(timeoutInMillisForGetter)
                            .withoutRetry()
                            .postWithErrorCode();
                    if (!response.isSuccess() || "false".equals(response.result)) {
                        return err(ZMIGRATE_REGISTER_ZSV_TO_ZMIGRATE_ERROR, "failed to register ZSV to ZMigrate").toErrorableValue();
                    }
                    RegisterZsvToZMigratePlatformResponse rsp = JSONObjectUtil.toObject(response.result, RegisterZsvToZMigratePlatformResponse.class);
                    return ErrorableValue.of(rsp);
                });
    }

    public ErrorableValue<Boolean> verifyGatewayConnection() {
        return retryWithFreshContext(
                VERIFY_GATEWAY_CONNECTION_RETRY_TIMES, VERIFY_GATEWAY_CONNECTION_RETRY_INTERVAL_SECONDS,
                "verifyGatewayConnection",
                err(ZMIGRATE_VERIFY_GATEWAY_CONNECTION_ERROR, "failed to verify gateway connection from ZMigrate"),
                (managementIp, encryptKey) -> {
                    VerifyGatewayConnectionCmd cmd = new VerifyGatewayConnectionCmd(encryptKey);
                    cmd.mgmtADDR = managementIp;
                    cmd.servADDR = managementIp;
                    ErrorableValue<String> response = http(String.class)
                            .withPath(String.format("https://%s:%d%s", managementIp, GATEWAY_RESTFUL_API_PORT, ZMIGRATE_SERVICE_MANAGEMENT_PATH))
                            .withBodyJson(cmd)
                            .withTimeoutInMillis(timeoutInMillisForGetter)
                            .withoutRetry()
                            .postWithErrorCode();
                    if (!response.isSuccess() || "false".equals(response.result)) {
                        return err(ZMIGRATE_VERIFY_GATEWAY_CONNECTION_ERROR, "failed to verify gateway connection from ZMigrate").toErrorableValue();
                    }
                    AgentResponse rsp = JSONObjectUtil.toObject(response.result, AgentResponse.class);
                    if (rsp == null || "false".equals(rsp.code)) {
                        return err(ZMIGRATE_VERIFY_GATEWAY_CONNECTION_ERROR, "failed to verify gateway connection from ZMigrate").toErrorableValue();
                    }
                    return ErrorableValue.of(true);
                });
    }

    static class CloudServ {
        public String uuid;
        public String zone;
    }

    public ErrorableValue<Boolean> registerGatewayToZMigrate(String platformUuidOnZMigrate) {
        ErrorableValue<String> encryptKey = getEncryptKey();
        if (!encryptKey.isSuccess()) {
            return err(ZMIGRATE_REGISTER_GATEWAY_TO_ZMIGRATE_ERROR, "failed to register gateway to ZMigrate").toErrorableValue();
        }

        ErrorableValue<ZMigrateGatewayHelper.GatewayInfo> gatewayInfo = ZMigrateGatewayHelper.getGatewayInfo();
        // NOTE: this method uses getGatewayInfo() (not resolveManagementIp) because it needs vmUuid and zoneUuid
        if (!gatewayInfo.isSuccess()) {
            return err(ZMIGRATE_REGISTER_GATEWAY_TO_ZMIGRATE_ERROR, "failed to register gateway to ZMigrate, gateway not configured")
                    .withCause(gatewayInfo.error)
                    .toErrorableValue();
        }

        ZMigrateGatewayHelper.GatewayInfo info = gatewayInfo.result;

        RegisterGatewayCmd cmd = new RegisterGatewayCmd(encryptKey.result);
        CloudServ cloudServ = new CloudServ();
        cloudServ.uuid = info.vmUuid;
        cloudServ.zone = info.zoneUuid;
        cmd.cloudServ = JSONObjectUtil.toJsonString(cloudServ);
        cmd.cloudUUID = platformUuidOnZMigrate;
        cmd.mgmtADDR = info.managementIp;
        cmd.servADDR = info.managementIp;
        cmd.servName = cmd.servADDR;
        ErrorableValue<String> response = http(String.class)
                .withPath(String.format("https://%s:%d%s", info.managementIp, GATEWAY_RESTFUL_API_PORT, ZMIGRATE_SERVICE_MANAGEMENT_PATH))
                .withBodyJson(cmd)
                .withTimeoutInMillis(timeoutInMillisForGetter)
                .withoutRetry()
                .postWithErrorCode();
        if (!response.isSuccess() || "false".equals(response.result)) {
            return err(ZMIGRATE_REGISTER_GATEWAY_TO_ZMIGRATE_ERROR, "failed to register gateway to ZMigrate").toErrorableValue();
        }
        return ErrorableValue.of(true);
    }

    public ErrorableValue<GetZMigrateInfoResponse> getZMigrateManagementServerInfo(int retryTimes, int retryIntervalInSeconds) {
        return retryWithFreshContext(
                retryTimes, retryIntervalInSeconds,
                "getZMigrateManagementServerInfo",
                err(ZMIGRATE_GET_ZMIGRATE_MANAGEMENT_SERVER_INFO_ERROR, "failed to get ZMigrate management server info"),
                (managementIp, encryptKey) -> {
                    GetZMigrateInfoCmd cmd = new GetZMigrateInfoCmd(encryptKey);
                    ErrorableValue<String> response = http(String.class)
                            .withPath(String.format("https://%s:%d%s", managementIp, GATEWAY_RESTFUL_API_PORT, ZMIGRATE_SERVICE_MANAGEMENT_PATH))
                            .withBodyJson(cmd)
                            .withTimeoutInMillis(timeoutInMillisForGetter)
                            .withoutRetry()
                            .postWithErrorCode();
                    if (response.isSuccess() && response.result != null && !"false".equals(response.result)) {
                        GetZMigrateInfoResponse rsp = JSONObjectUtil.toObject(response.result, GetZMigrateInfoResponse.class);
                        if (rsp != null) {
                            return ErrorableValue.of(rsp);
                        }
                    }
                    return err(ZMIGRATE_GET_ZMIGRATE_MANAGEMENT_SERVER_INFO_ERROR, response.isSuccess() ? "response is false" : String.valueOf(response.error))
                            .toErrorableValue();
                });
    }

    public ErrorableValue<String> getPlatformInfos() {
        return retryWithFreshContext(
                DEFAULT_RETRY_TIMES, DEFAULT_RETRY_INTERVAL_SECONDS,
                "getPlatformInfos",
                err(ZMIGRATE_GET_PLATFORM_INFOS_ERROR, "failed to get platform infos from ZMigrate"),
                (managementIp, encryptKey) -> {
                    GetPlatformInfosCmd cmd = new GetPlatformInfosCmd(encryptKey);
                    ErrorableValue<String> response = http(String.class)
                            .withPath(String.format("https://%s:%d%s", managementIp, GATEWAY_RESTFUL_API_PORT, ZMIGRATE_SERVICE_MANAGEMENT_PATH))
                            .withBodyJson(cmd)
                            .withTimeoutInMillis(timeoutInMillisForGetter)
                            .withoutRetry()
                            .postWithErrorCode();
                    if (!response.isSuccess()) {
                        return err(ZMIGRATE_GET_PLATFORM_INFOS_ERROR, "failed to get platform infos from ZMigrate").toErrorableValue();
                    }
                    if (ZMIGRATE_NO_DATA_RESPONSE.equals(response.result)) {
                        return ErrorableValue.of(EMPTY_JSON_ARRAY);
                    }
                    return ErrorableValue.of(response.result);
                });
    }

    public ErrorableValue<String> getGatewayServerInfos() {
        return retryWithFreshContext(
                DEFAULT_RETRY_TIMES, DEFAULT_RETRY_INTERVAL_SECONDS,
                "getGatewayServerInfos",
                err(ZMIGRATE_GET_GATEWAY_SERVER_INFOS_ERROR, "failed to get gateway server infos from ZMigrate"),
                (managementIp, encryptKey) -> {
                    GetGatewayServerInfosCmd cmd = new GetGatewayServerInfosCmd(encryptKey);
                    ErrorableValue<String> response = http(String.class)
                            .withPath(String.format("https://%s:%d%s", managementIp, GATEWAY_RESTFUL_API_PORT, ZMIGRATE_SERVICE_MANAGEMENT_PATH))
                            .withBodyJson(cmd)
                            .withTimeoutInMillis(timeoutInMillisForGetter)
                            .withoutRetry()
                            .postWithErrorCode();
                    if (!response.isSuccess()) {
                        return err(ZMIGRATE_GET_GATEWAY_SERVER_INFOS_ERROR, "failed to get gateway server infos from ZMigrate").toErrorableValue();
                    }
                    if (ZMIGRATE_NO_DATA_RESPONSE.equals(response.result)) {
                        return ErrorableValue.of(EMPTY_JSON_ARRAY);
                    }
                    return ErrorableValue.of(response.result);
                });
    }

    public ErrorableValue<String> getMigrateJobs() {
        return retryWithFreshContext(
                DEFAULT_RETRY_TIMES, DEFAULT_RETRY_INTERVAL_SECONDS,
                "getMigrateJobs",
                err(ZMIGRATE_GET_MIGRATE_JOBS_ERROR, "failed to get migration jobs from ZMigrate"),
                (managementIp, encryptKey) -> {
                    GetMigrationJobsCmd cmd = new GetMigrationJobsCmd(encryptKey);
                    ErrorableValue<String> response = http(String.class)
                            .withPath(String.format("https://%s:%d%s", managementIp, GATEWAY_RESTFUL_API_PORT, ZMIGRATE_INTEGRATION_MANAGEMENT_PATH))
                            .withBodyJson(cmd)
                            .withTimeoutInMillis(timeoutInMillisForGetter)
                            .withoutRetry()
                            .postWithErrorCode();
                    if (!response.isSuccess()) {
                        return err(ZMIGRATE_GET_MIGRATE_JOBS_ERROR, "failed to get migration jobs from ZMigrate").toErrorableValue();
                    }
                    if (ZMIGRATE_NO_DATA_RESPONSE.equals(response.result)) {
                        return ErrorableValue.of(EMPTY_JSON_ARRAY);
                    }
                    return ErrorableValue.of(response.result);
                });
    }

    public ErrorableValue<String> getLicenses() {
        return retryWithFreshContext(
                0, 0,
                "getLicenses",
                err(GENERIC_ERROR, "failed to get licenses from ZMigrate"),
                (managementIp, encryptKey) -> {
                    GetLicensesCmd cmd = new GetLicensesCmd(encryptKey);
                    ErrorableValue<String> response = http(String.class)
                            .withPath(String.format("https://%s:%d%s", managementIp, GATEWAY_RESTFUL_API_PORT, ZMIGRATE_SERVICE_MANAGEMENT_PATH))
                            .withBodyJson(cmd)
                            .withTimeoutInMillis(timeoutInMillisForGetLicenses)
                            .withoutRetry()
                            .postWithErrorCode();
                    if (!response.isSuccess()) {
                        return err(GENERIC_ERROR, "failed to get licenses from ZMigrate").toErrorableValue();
                    }
                    if (ZMIGRATE_NO_DATA_RESPONSE.equals(response.result)) {
                        return ErrorableValue.of(EMPTY_JSON_ARRAY);
                    }
                    return ErrorableValue.of(response.result);
                });
    }

    public ErrorableValue<String> exportActivationInfos() {
        return retryWithFreshContext(
                DEFAULT_RETRY_TIMES, DEFAULT_RETRY_INTERVAL_SECONDS,
                "exportActivationInfos",
                err(ZMIGRATE_EXPORT_ACTIVATION_INFOS_ERROR, "failed to export activation infos from ZMigrate"),
                (managementIp, encryptKey) -> {
                    ExportActivationInfosCmd cmd = new ExportActivationInfosCmd(encryptKey);
                    ErrorableValue<String> response = http(String.class)
                            .withPath(String.format("https://%s:%d%s", managementIp, GATEWAY_RESTFUL_API_PORT, ZMIGRATE_SERVICE_MANAGEMENT_PATH))
                            .withBodyJson(cmd)
                            .withTimeoutInMillis(timeoutInMillisForGetter)
                            .withoutRetry()
                            .postWithErrorCode();
                    if (!response.isSuccess() || "false".equals(response.result)) {
                        return err(ZMIGRATE_EXPORT_ACTIVATION_INFOS_ERROR, "failed to export activation infos from ZMigrate")
                                .withCause(response.error)
                                .toErrorableValue();
                    }
                    return ErrorableValue.of(response.result);
                });
    }

    public ErrorableValue<String> importActivationPackage(String activationPackage) {
        if (activationPackage == null || activationPackage.trim().isEmpty()) {
            return err(ZMIGRATE_IMPORT_ACTIVATION_PACKAGE_ERROR, "activation package cannot be empty").toErrorableValue();
        }

        return retryWithFreshContext(
                DEFAULT_RETRY_TIMES, DEFAULT_RETRY_INTERVAL_SECONDS,
                "importActivationPackage",
                err(ZMIGRATE_IMPORT_ACTIVATION_PACKAGE_ERROR, "failed to import activation package to ZMigrate"),
                (managementIp, encryptKey) -> {
                    ImportActivationPackageCmd cmd = new ImportActivationPackageCmd(encryptKey);
                    cmd.activePackage = activationPackage;
                    ErrorableValue<String> response = http(String.class)
                            .withPath(String.format("https://%s:%d%s", managementIp, GATEWAY_RESTFUL_API_PORT, ZMIGRATE_SERVICE_MANAGEMENT_PATH))
                            .withBodyJson(cmd)
                            .withTimeoutInMillis(timeoutInMillisForGetter)
                            .withoutRetry()
                            .postWithErrorCode();
                    if (!response.isSuccess() || "false".equals(response.result)) {
                        return err(ZMIGRATE_IMPORT_ACTIVATION_PACKAGE_ERROR, "failed to import activation package to ZMigrate")
                                .withCause(response.error)
                                .toErrorableValue();
                    }
                    return ErrorableValue.of(response.result);
                });
    }

    public static class AgentCmd {
        @SerializedName("EncryptKey")
        public String encryptKey;
    }

    public static class AgentResponse {
        @SerializedName("Code")
        public String code;
        @SerializedName("Msg")
        public String msg;
    }

    public static class CreateAccountCmd extends AgentCmd {
        @SerializedName("Action")
        public String action = ZMIGRATE_TRANSFORM_ACCOUNT_ACTION;
        @SerializedName("AcctUUID")
        public String acctUuid = ZMigrateGlobalConfig.PLATFORM_ACCOUNT_UUID.value();
        @SerializedName("RegnUUID")
        public String regnUUID = ZMigrateGlobalConfig.PLATFORM_REGION_UUID.value();

        public CreateAccountCmd(String encryptKey) {
            this.encryptKey = encryptKey;
        }
    }

    public static class VerifyGatewayConnectionCmd extends AgentCmd {
        @SerializedName("Action")
        public String action = ZMIGRATE_TEST_SERVER_CONNECTION_ACTION;
        @SerializedName("MgmtADDR")
        public String mgmtADDR;
        @SerializedName("ServADDR")
        public String servADDR;

        public VerifyGatewayConnectionCmd(String encryptKey) {
            this.encryptKey = encryptKey;
        }
    }

    public static class RegisterGatewayCmd extends AgentCmd {
        @SerializedName("Action")
        public String action = ZMIGRATE_INITIALIZE_NEW_SERVER_ACTION;
        @SerializedName("AcctUUID")
        public String acctUuid = ZMigrateGlobalConfig.PLATFORM_ACCOUNT_UUID.value();
        @SerializedName("RegnUUID")
        public String regnUUID = ZMigrateGlobalConfig.PLATFORM_REGION_UUID.value();
        @SerializedName("CloudName")
        public String cloudName = PLATFORM_TYPE;
        @SerializedName("ServRegn")
        public String servRegn = PLATFORM_TYPE;
        @SerializedName("CloudServ")
        public String cloudServ;
        @SerializedName("CloudUUID")
        public String cloudUUID;
        @SerializedName("MgmtADDR")
        public String mgmtADDR;
        @SerializedName("ServADDR")
        public String servADDR;
        @SerializedName("ServName")
        public String servName;

        public RegisterGatewayCmd(String encryptKey) {
            this.encryptKey = encryptKey;
        }
    }

    public static class VerifyPlatformConnectionCmd extends AgentCmd {
        @SerializedName("Action")
        public String action = ZMIGRATE_VERIFY_CLOUD_CONNECTION_ACTION;
        @SerializedName("IdentityPort")
        public String identityPort;
        @SerializedName("IdentityProtocol")
        public String identityProtocol;
        @SerializedName("EndPoint")
        public String endPoint;
        // oauth | zstack
        // If AccessType=oauth,AccessUsername/AccessPassword is required
        // If AccessType=zstack,AccessKeyID/AccessKeySecrete is required
        @SerializedName("AccessType")
        public String accessType;
        @SerializedName("AccessUsername")
        public String accessUsername;
        @SerializedName("AccessPassword")
        public String accessPassword;
        @SerializedName("AccessKeyID")
        public String accessKeyID;
        @SerializedName("AccessKeySecrete")
        public String accessKeySecrete;

        public VerifyPlatformConnectionCmd(String encryptKey) {
            this.encryptKey = encryptKey;
        }
    }

    public static class VerifyPlatformConnectionResponse extends AgentResponse {
    }

    public static class RegisterZsvToZMigratePlatformCmd extends AgentCmd {
        @SerializedName("Action")
        public String action = ZMIGRATE_INITIALIZE_CLOUD_CONNECTION_ACTION;
        @SerializedName("AcctUUID")
        public String acctUuid = ZMigrateGlobalConfig.PLATFORM_ACCOUNT_UUID.value();
        @SerializedName("RegnUUID")
        public String regnUUID = ZMigrateGlobalConfig.PLATFORM_REGION_UUID.value();
        @SerializedName("DisplayName")
        public String displayName;
        @SerializedName("IdentityPort")
        public String identityPort;
        @SerializedName("IdentityProtocol")
        public String identityProtocol;
        @SerializedName("EndPoint")
        public String endPoint;
        @SerializedName("TimezoneOffset")
        public String timezoneOffset;
        @SerializedName("ExtraSupport")
        public String extraSupport = ZMIGRATE_ZSPHERE_EXTRA_SUPPORT;
        // oauth | zstack
        // If AccessType=oauth,AccessUsername/AccessPassword is required
        // If AccessType=zstack,AccessKeyID/AccessKeySecrete is required
        @SerializedName("AccessType")
        public String accessType;
        @SerializedName("AccessUsername")
        public String accessUsername;
        @SerializedName("AccessPassword")
        public String accessPassword;
        @SerializedName("AccessKeyID")
        public String accessKeyID;
        @SerializedName("AccessKeySecrete")
        public String accessKeySecrete;

        public RegisterZsvToZMigratePlatformCmd(String encryptKey) {
            this.encryptKey = encryptKey;
        }
    }

    public static class RegisterZsvToZMigratePlatformResponse extends AgentResponse {
    }

    public static class GetZMigrateInfoCmd extends AgentCmd {
        @SerializedName("Action")
        public String action = ZMIGRATE_GET_MGMT_SERVER_INFO_ACTION;

        public GetZMigrateInfoCmd(String encryptKey) {
            this.encryptKey = encryptKey;
        }
    }

    public static class GetZMigrateInfoResponse {
        public String id;
        public String version;
        public Long uptime;
        public Long timestamp;
    }

    public static class GetLicensesCmd extends AgentCmd {
        @SerializedName("Action")
        public String action = ZMIGRATE_QUERY_LICENSE_ACTION;

        public GetLicensesCmd(String encryptKey) {
            this.encryptKey = encryptKey;
        }
    }

    public static class GetGatewayServerInfosCmd extends AgentCmd {
        @SerializedName("Action")
        public String action = ZMIGRATE_QUERY_AVAILABLE_SERVER_ACTION;
        @SerializedName("AcctUUID")
        public String acctUuid = ZMigrateGlobalConfig.PLATFORM_ACCOUNT_UUID.value();

        public GetGatewayServerInfosCmd(String encryptKey) {
            this.encryptKey = encryptKey;
        }
    }

    public static class GetMigrationJobsCmd extends AgentCmd {
        @SerializedName("Action")
        public String action = ZMIGRATE_LIST_MIGRATION_JOB_ACTION;
        @SerializedName("AcctUUID")
        public String acctUuid = ZMigrateGlobalConfig.PLATFORM_ACCOUNT_UUID.value();

        public GetMigrationJobsCmd(String encryptKey) {
            this.encryptKey = encryptKey;
        }
    }

    public static class GetPlatformInfosCmd extends AgentCmd {
        @SerializedName("Action")
        public String action = ZMIGRATE_LIST_CLOUD_ACTION;
        @SerializedName("AcctUUID")
        public String acctUuid = ZMigrateGlobalConfig.PLATFORM_ACCOUNT_UUID.value();

        public GetPlatformInfosCmd(String encryptKey) {
            this.encryptKey = encryptKey;
        }
    }

    public static class ExportActivationInfosCmd extends AgentCmd {
        @SerializedName("Action")
        public String action = EXPORT_ACTIVATION_INFO;

        public ExportActivationInfosCmd(String encryptKey) {
            this.encryptKey = encryptKey;
        }
    }

    public static class ImportActivationPackageCmd extends AgentCmd {
        @SerializedName("Action")
        public String action = IMPORT_ACTIVE_PACKAGE;
        @SerializedName("ActivePackage")
        public String activePackage;

        public ImportActivationPackageCmd(String encryptKey) {
            this.encryptKey = encryptKey;
        }
    }

    private static long toLongOrMax(Object id) {
        if (id == null) {
            return Long.MAX_VALUE;
        }
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        try {
            return Long.parseLong(id.toString());
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Count valid source platforms from getPlatformInfos() result.
     * Rules (matching frontend findAllPlatforms filter, ZSV-12209):
     *   1. CLOUD_UUID must be truthy (drop dirty rows)
     *   2. CLOUD_TYPE must equal "VMware"
     */
    public static int countValidPlatforms(List<Map<String, Object>> platformInfoList) {
        if (platformInfoList == null || platformInfoList.isEmpty()) {
            return 0;
        }

        long count = platformInfoList.stream()
                .filter(Objects::nonNull)
                .filter(p -> {
                    Object uuid = p.get("CLOUD_UUID");
                    return uuid != null && !uuid.toString().isEmpty();
                })
                .filter(p -> {
                    Object cloudType = p.get("CLOUD_TYPE");
                    return cloudType != null && "VMware".equals(cloudType.toString());
                })
                .count();
        return (int) count;
    }

    /**
     * Count valid gateways from getGatewayServerInfos() result.
     * Rules (matching frontend gateway list filter, ZSV-12209):
     *   1. SERV_UUID must be truthy (drop invalid/empty rows)
     *   2. SERV_ROLE must equal 2
     *   3. SERV_LOCA must be one of ["Cloud", "OnPremise"]
     */
    public static int countValidGateways(List<Map<String, Object>> gatewayServerInfoList) {
        if (gatewayServerInfoList == null || gatewayServerInfoList.isEmpty()) {
            return 0;
        }
        Set<String> validLocations = new HashSet<>(Arrays.asList("Cloud", "OnPremise"));
        long count = gatewayServerInfoList.stream()
                .filter(Objects::nonNull)
                .filter(g -> {
                    Object uuid = g.get("SERV_UUID");
                    return uuid != null && !uuid.toString().isEmpty();
                })
                .filter(g -> toLongOrMax(g.get("SERV_ROLE")) == 2L)
                .filter(g -> {
                    Object loca = g.get("SERV_LOCA");
                    return loca != null && validLocations.contains(loca.toString());
                })
                .count();
        return (int) count;
    }
}
