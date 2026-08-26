package org.zstack.header.pki;

public interface HostPkiConstant {
    String GLOBAL_SCOPE = "Global";

    String CA_TYPE_BUILTIN = "Builtin";
    String CA_TYPE_EXTERNAL = "External";

    String CA_STATUS_ACTIVE = "Active";
    String CA_STATUS_DISABLED = "Disabled";

    String HOST_CERT_STATUS_PENDING = "Pending";
    String HOST_CERT_STATUS_ACTIVE = "Active";
    String HOST_CERT_STATUS_RENEWING = "Renewing";
    String HOST_CERT_STATUS_ROTATING = "Rotating";
    String HOST_CERT_STATUS_REVOKED = "Revoked";
    String HOST_CERT_STATUS_EXPIRED = "Expired";
    String HOST_CERT_STATUS_INSTALL_FAILED = "InstallFailed";

    String USAGE_MIGRATION = "migration";
    String HOST_CERT_ROLE_SERVER = "server";
    String HOST_CERT_ROLE_CLIENT = "client";

    String HOST_PKI_GENERATE_CSR_PATH = "/host/pki/generate-csr";
    String HOST_PKI_INSTALL_PATH = "/host/pki/install";
    String HOST_PKI_STATUS_PATH = "/host/pki/status";
    String HOST_PKI_REVOKE_LOCAL_PATH = "/host/pki/revoke-local";
}
