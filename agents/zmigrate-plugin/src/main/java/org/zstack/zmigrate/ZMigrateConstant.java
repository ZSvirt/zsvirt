package org.zstack.zmigrate;

import org.zstack.header.configuration.PythonClass;

import java.util.regex.Pattern;

@PythonClass
public class ZMigrateConstant {
    public static final String SERVICE_ID = "ZMigratePlugin";

    public static final String ZMIGRATE_SOFTWARE_PACKAGE_TYPE = "ZMigrate";
    public static final String ZMIGRATE_MANAGEMENT_VM_UUID = "f176628e5c2c4e31aa16ccac41e7b18e";

    public static final int ZMIGRATE_SOFTWARE_IMAGE_COUNT = 3;

    public static final String GATEWAY_SSH_USERNAME = "zmuser";
    public static final int GATEWAY_SSH_PORT = 50022;
    public static final int GATEWAY_RESTFUL_API_PORT = 443;
    public static final String VDDK_VM_PATH = "/root/vddk.tar.gz";
    public static final String VDDK_CONTAINER_PATH = "/tmp/vddk.tar.gz";
    public static final String VDDK_GENERATION_MARKER_PATH = "/root/.zstack-zmigrate-vddk-generation";
    public static final String VDDK_INSTALL_LOCK_PATH = "/root/.zstack-zmigrate-vddk-install.lock";
    public static final long VDDK_INSTALL_TIMEOUT_SECONDS = 600;
    public static final long VDDK_DISTRIBUTION_TIMEOUT_SECONDS = 7200;

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{32}$");

    public static String vddkUploadPath(String uploadTaskUuid) {
        validateUploadTaskUuid(uploadTaskUuid);
        return String.format("/tmp/vddk-%s.tar.gz", uploadTaskUuid);
    }

    public static String vddkGenerationPath(String uploadTaskUuid) {
        validateUploadTaskUuid(uploadTaskUuid);
        return String.format("/root/vddk-%s.tar.gz", uploadTaskUuid);
    }

    public static String vddkDistributionUploadPath(String uploadTaskUuid, String distributionUuid) {
        validateUploadTaskUuid(uploadTaskUuid);
        validateUploadTaskUuid(distributionUuid);
        return String.format("/tmp/vddk-%s-%s.tar.gz", uploadTaskUuid, distributionUuid);
    }

    private static void validateUploadTaskUuid(String uploadTaskUuid) {
        if (uploadTaskUuid == null || !UUID_PATTERN.matcher(uploadTaskUuid).matches()) {
            throw new IllegalArgumentException("invalid ZMigrate VDDK upload task UUID");
        }
    }

    public static final String GATEWAY_CONFIG_FILE_PATH = "/mnt/mgmt/htdocs/integration/component/infrastructure/extra/mgmt_settings.ini";
    public static final String BUILD_ENCRYPT_KEY_FILE_PATH = "/mnt/mgmt/htdocs/integration/component/infrastructure/extra/build_encrypt_key.py";

    public static final String PLATFORM_TYPE = "ZSphere";
    public static final String PLATFORM_IDENTITY_PROTOCOL_TYPE = "http";

    public static final String GATEWAY_IMAGE_PREFIX = "Gateway_Linux_Server";
    public static final String LINUX_BOOT_IMAGE_PREFIX = "BootImage_for_Linux";
    public static final String WINDOWS_BOOT_IMAGE_PREFIX = "BootImage_for_Windows";
    public static final String UPGRADE_PACKAGE_PREFIX = "TrekerInstallation";

    public static final String GATEWAY_IMAGE_DESCRIPTION = "TrekerImage";
    public static final String BOOT_IMAGE_DESCRIPTION = "TrekerLiteImage";

    public static final String UPGRADE_SCRIPT_PATH = "/package/upgrade.sh";
    public static final String UPGRADE_PACKAGE_ROOT_DIR = "/tmp";

    public static final String ZMIGRATE_ACCESS_OAUTH_TYPE = "oauth";
    public static final String ZMIGRATE_ACCESS_TYPE = "zstack";
    public static final String ZMIGRATE_DISPLAY_NAME = "ZSphere";
    public static final String ZMIGRATE_ZSPHERE_EXTRA_SUPPORT = "ZSphere";

    public static final String ZMIGRATE_SERVICE_MANAGEMENT_PATH = "/restful/ServiceManagement";
    // add gateway
    public static final String ZMIGRATE_INITIALIZE_NEW_SERVER_ACTION = "InitializeNewServer";
    // test gateway connection
    public static final String ZMIGRATE_TEST_SERVER_CONNECTION_ACTION = "TestServerConnection";
    // query license
    public static final String ZMIGRATE_QUERY_LICENSE_ACTION = "QueryLicense";
    public static final String EXPORT_ACTIVATION_INFO = "ExportActivationInfo";
    public static final String IMPORT_ACTIVE_PACKAGE = "ImportActivePackage";
    public static final String ZMIGRATE_LICENSE_FILE_NAME = "ZMigrateLicense";

    // get mgmt server info
    public static final String ZMIGRATE_GET_MGMT_SERVER_INFO_ACTION = "GetMgmtServerInfo";
    // get all platform info
    public static final String ZMIGRATE_LIST_CLOUD_ACTION = "ListCloud";
    // get all platform info
    public static final String ZMIGRATE_QUERY_AVAILABLE_SERVER_ACTION = "QueryAvailableServer";

    public static final String ZMIGRATE_ZSPHERE_WEB_SERVICES_PATH = "/restful/ZSphereWebServices";
    // verify platform connection
    public static final String ZMIGRATE_VERIFY_CLOUD_CONNECTION_ACTION = "VerifyCloudConnection";
    // add platform to ZMigrate
    public static final String ZMIGRATE_INITIALIZE_CLOUD_CONNECTION_ACTION = "InitializeCloudConnection";

    public static final String ZMIGRATE_IDENTITY_MANAGEMENT_PATH = "/restful/IdentityManagement";
    // create user
    public static final String ZMIGRATE_TRANSFORM_ACCOUNT_ACTION = "TransformAccount";

    public static final String ZMIGRATE_INTEGRATION_MANAGEMENT_PATH = "/restful/IntegrationManagement";
    public static final String ZMIGRATE_LIST_MIGRATION_JOB_ACTION = "ListMigrationJob";

    //[
    //    {
    //        "ID": 1,
    //            "CLOUD_UUID": "e0cc4428-3e16-4d4d-a86a-f7f4ee79d94f",
    //            "AUTH_INFO": {
    //                "display_name": "ZSphere-fefafde4d31734dfbe63b36c8093e022",
    //    },
    //        "TIMESTAMP": "2026-02-10 13:30:36"
    //    }
    //]
    public static final String ZMIGRATE_PLATFORM_INFO_ON_CLOUD_UUID_KEY = "CLOUD_UUID";
    public static final String ZMIGRATE_PLATFORM_INFO_ON_CLOUD_AUTH_INFO_KEY = "AUTH_INFO";
    public static final String ZSV_PLATFORM_DISPLAY_NAME_ON_ZMIGRATE_KEY = "display_name";

    public static String getZsvPlatformDisplayNameOnZMigrate() {
        String uuid = ZMigrateGlobalConfig.PLATFORM_ACCOUNT_UUID.value();
        if (uuid == null || uuid.isEmpty()) {
            return ZMIGRATE_DISPLAY_NAME;
        }
        return String.format("%s-%s", ZMIGRATE_DISPLAY_NAME, uuid.substring(0, Math.min(8, uuid.length())));
    }
}
