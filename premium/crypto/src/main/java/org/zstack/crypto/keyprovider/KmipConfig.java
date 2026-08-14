package org.zstack.crypto.keyprovider;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.keyprovider.KmsIdentityVO;
import org.zstack.header.keyprovider.KmsVO;

import static org.zstack.core.Platform.operr;

public class KmipConfig {
    final String host;
    final Integer port;
    final String version;
    final String username;
    final String password;
    final String clientCertPem;
    final String clientKeyPem;
    final String caCertPem;

    private KmipConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.version = builder.version;
        this.username = builder.username;
        this.password = builder.password;
        this.clientCertPem = builder.clientCertPem;
        this.clientKeyPem = builder.clientKeyPem;
        this.caCertPem = builder.caCertPem;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static KmipConfig fromKms(KmsVO kms) {
        validateWithoutIdentity(kms, null);
        return builder()
                .host(kms.getEndpoint())
                .port(kms.getPort())
                .version(kms.getKmipVersionString())
                .username(kms.getUsername())
                .password(kms.getPassword())
                .caCertPem(kms.getServerCertPem())
                .build();
    }

    public static KmipConfig fromKms(KmsVO kms, KmsIdentityVO identity) {
        return fromKms(kms, identity, null);
    }

    public static KmipConfig fromKmsForHealthCheck(KmsVO kms, KmsIdentityVO identity) {
        validateWithIdentity(kms, identity, null, false);
        return builder()
                .host(kms.getEndpoint())
                .port(kms.getPort())
                .version(kms.getKmipVersionString())
                .username(kms.getUsername())
                .password(kms.getPassword())
                .clientCertPem(identity.getClientCertPem())
                .clientKeyPem(identity.getClientKeyPem())
                .caCertPem(kms.getServerCertPem())
                .build();
    }

    public static KmipConfig fromKms(KmsVO kms, KmsIdentityVO identity, String role) {
        validateWithIdentity(kms, identity, role, true);
        return builder()
                .host(kms.getEndpoint())
                .port(kms.getPort())
                .version(kms.getKmipVersionString())
                .username(kms.getUsername())
                .password(kms.getPassword())
                .clientCertPem(identity.getClientCertPem())
                .clientKeyPem(identity.getClientKeyPem())
                .caCertPem(kms.getServerCertPem())
                .build();
    }

    private static void validateWithoutIdentity(KmsVO kms, String role) {
        if (kms == null) {
            throw new OperationFailureException(operr("%skms is null", rolePrefix(role)));
        }
        if (StringUtils.isBlank(kms.getEndpoint())) {
            throw new OperationFailureException(operr("%skms[uuid:%s] endpoint is empty", rolePrefix(role), kms.getUuid()));
        }
    }

    private static void validateWithIdentity(KmsVO kms, KmsIdentityVO identity, String role, boolean requireServerCert) {
        validateWithoutIdentity(kms, role);
        if (identity == null) {
            throw new OperationFailureException(operr("%skms[uuid:%s] active identity is not configured", rolePrefix(role), kms.getUuid()));
        }
        if (StringUtils.isBlank(identity.getClientCertPem())
                || StringUtils.isBlank(identity.getClientKeyPem())
                || (requireServerCert && StringUtils.isBlank(kms.getServerCertPem()))) {
            throw new OperationFailureException(operr("%skms[uuid:%s] auth materials are incomplete", rolePrefix(role), kms.getUuid()));
        }
    }

    private static String rolePrefix(String role) {
        return StringUtils.isBlank(role) ? "" : role + " ";
    }

    public static class Builder {
        private String host;
        private Integer port;
        private String version;
        private String username;
        private String password;
        private String clientCertPem;
        private String clientKeyPem;
        private String caCertPem;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder clientCertPem(String clientCertPem) {
            this.clientCertPem = clientCertPem;
            return this;
        }

        public Builder clientKeyPem(String clientKeyPem) {
            this.clientKeyPem = clientKeyPem;
            return this;
        }

        public Builder caCertPem(String caCertPem) {
            this.caCertPem = caCertPem;
            return this;
        }

        public KmipConfig build() {
            return new KmipConfig(this);
        }
    }

    private static String abbrevPem(String pem, int headChars) {
        if (StringUtils.isBlank(pem)) {
            return "null/empty";
        }
        String oneLine = pem.replace('\n', ' ').trim();
        if (oneLine.length() <= headChars) {
            return oneLine;
        }
        return oneLine.substring(0, headChars) + "…(" + pem.length() + " chars)";
    }

    /** For debug logs: password redacted, PEM truncated. */
    @Override
    public String toString() {
        return "KmipConfig{"
                + "host='" + host + '\''
                + ", port=" + port
                + ", version='" + version + '\''
                + ", username='" + username + '\''
                + ", password=" + (StringUtils.isBlank(password) ? "empty" : "***")
                + ", clientCertPem=" + abbrevPem(clientCertPem, 64)
                + ", clientKeyPem=" + abbrevPem(clientKeyPem, 64)
                + ", caCertPem=" + abbrevPem(caCertPem, 64)
                + '}';
    }
}
