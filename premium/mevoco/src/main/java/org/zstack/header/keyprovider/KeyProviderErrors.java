package org.zstack.header.keyprovider;

import java.util.EnumSet;

public enum KeyProviderErrors {
    OK(1000),
    INVALID_CONTENT(1001),
    INTERNAL_ERROR(1002),
    BACKEND_UNAVAILABLE(1500),
    SOCKET_NOT_FOUND(1506),
    SOCKET_NOT_SOCKET(1507),
    KMIP_CONNECT_FAILED(1700),
    KMIP_TIMEOUT(1701),
    KMIP_TLS_HANDSHAKE_FAILED(1702),
    KMIP_CERT_INVALID(1703),
    KMIP_OPERATION_FAILED(1704),
    KMIP_KMS_CERT_UNTRUSTED(1705),
    KMIP_CLIENT_CERT_UNTRUSTED(1706),
    KMIP_MUTUAL_CERT_UNTRUSTED(1707),
    ROOT_KEY_SHA256_MISMATCH(1600),
    ROOT_KEY_SHA256_FILE_MISSING(1601),
    ZIP_DATA_REQUIRED(1602),
    CHECKSUM_MISMATCH(1603),
    PASSWORD_INVALID(1604),
    ROOT_KEY_EXTENSION_MISSING(1605),
    NAME_DUPLICATE(1900),
    UUID_DUPLICATE(1901),

    // 2XXX: TPM + KMS related error (general error)
    // +   210X: TPM attached error
    TPM_RELATED_ERROR(2000),
    TPM_ALREADY_ATTACHED_KEY_PROVIDER(2101),
    ;

    private static final EnumSet<KeyProviderErrors> KMIP_CONNECTED_STATUSES = EnumSet.of(
            OK,
            KMIP_CERT_INVALID,
            KMIP_OPERATION_FAILED,
            KMIP_KMS_CERT_UNTRUSTED,
            KMIP_CLIENT_CERT_UNTRUSTED,
            KMIP_MUTUAL_CERT_UNTRUSTED,
            KMIP_TLS_HANDSHAKE_FAILED
    );

    public final String code;

    KeyProviderErrors(int id) {
        code = String.format("KP.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }

    public static KeyProviderErrors fromKeyToolStatus(int statusCode) {
        switch (statusCode) {
            case 0:
                return OK;
            case 602:
            case 505:
                return ZIP_DATA_REQUIRED;
            case 603:
                return CHECKSUM_MISMATCH;
            case 604:
                return PASSWORD_INVALID;
            case 605:
                return ROOT_KEY_EXTENSION_MISSING;
            case 600:
                return ROOT_KEY_SHA256_MISMATCH;
            case 601:
                return ROOT_KEY_SHA256_FILE_MISSING;
            case 500:
            case 503:
                return BACKEND_UNAVAILABLE;
            case 506:
                return SOCKET_NOT_FOUND;
            case 507:
                return SOCKET_NOT_SOCKET;
            case 700:
                return KMIP_CONNECT_FAILED;
            case 701:
                return KMIP_TIMEOUT;
            case 702:
                return KMIP_TLS_HANDSHAKE_FAILED;
            case 703:
                return KMIP_CERT_INVALID;
            case 704:
                return KMIP_OPERATION_FAILED;
            case 705:
                return KMIP_KMS_CERT_UNTRUSTED;
            case 706:
                return KMIP_CLIENT_CERT_UNTRUSTED;
            case 707:
                return KMIP_MUTUAL_CERT_UNTRUSTED;
            case 1:
                return INVALID_CONTENT;
            case 2:
                return INTERNAL_ERROR;
            default:
                return INTERNAL_ERROR;
        }
    }

    public static boolean isKmipConnectedStatus(int statusCode) {
        return KMIP_CONNECTED_STATUSES.contains(fromKeyToolStatus(statusCode));
    }
}
