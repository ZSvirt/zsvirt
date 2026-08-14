package org.zstack.header.console;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:26 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ConsoleProxyCommands {
    public static class AgentResponse {
        private boolean success = true;
        private String error;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    public static class AgentCommand {
    }

    public static class CheckAvailabilityCmd extends AgentCommand {
        private String proxyHostname;
        private Integer proxyPort;
        private Integer targetPort;
        private String targetHostname;
        private String targetSchema;
        private String scheme;
        private String token;
        private String proxyIdentity;

        public String getProxyHostname() {
            return proxyHostname;
        }

        public void setProxyHostname(String proxyHostname) {
            this.proxyHostname = proxyHostname;
        }

        public Integer getProxyPort() {
            return proxyPort;
        }

        public void setProxyPort(Integer proxyPort) {
            this.proxyPort = proxyPort;
        }

        public Integer getTargetPort() {
            return targetPort;
        }

        public void setTargetPort(Integer targetPort) {
            this.targetPort = targetPort;
        }

        public String getTargetHostname() {
            return targetHostname;
        }

        public void setTargetHostname(String targetHostname) {
            this.targetHostname = targetHostname;
        }

        public String getTargetSchema() {
            return targetSchema;
        }

        public void setTargetSchema(String targetSchema) {
            this.targetSchema = targetSchema;
        }

        public String getScheme() {
            return scheme;
        }

        public void setScheme(String scheme) {
            this.scheme = scheme;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getProxyIdentity() {
            return proxyIdentity;
        }

        public void setProxyIdentity(String proxyIdentity) {
            this.proxyIdentity = proxyIdentity;
        }
    }

    public static class CheckAvailabilityRsp extends AgentResponse {
        private Boolean available;

        public Boolean getAvailable() {
            return available;
        }

        public void setAvailable(Boolean available) {
            this.available = available;
        }
    }

    public static class DeleteProxyCmd extends AgentCommand {
        private String token;
        private String targetSchema;
        private String targetHostname;
        private int targetPort;
        private String proxyHostname;
        private String vmUuid;
        private int proxyPort;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getTargetSchema() {
            return targetSchema;
        }

        public void setTargetSchema(String targetSchema) {
            this.targetSchema = targetSchema;
        }

        public String getTargetHostname() {
            return targetHostname;
        }

        public void setTargetHostname(String targetHostname) {
            this.targetHostname = targetHostname;
        }

        public int getTargetPort() {
            return targetPort;
        }

        public void setTargetPort(int targetPort) {
            this.targetPort = targetPort;
        }

        public String getProxyHostname() {
            return proxyHostname;
        }

        public void setProxyHostname(String proxyHostname) {
            this.proxyHostname = proxyHostname;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public int getProxyPort() {
            return proxyPort;
        }

        public void setProxyPort(int proxyPort) {
            this.proxyPort = proxyPort;
        }
    }

    public static class DeleteProxyRsp extends AgentResponse {
    }

    public static class EstablishProxyCmd extends AgentCommand {
        private String token;
        private String targetSchema;            // vnc or http
        private String targetHostname;
        private int targetPort;
        private String proxyHostname;
        private String sslCertFile;
        private int proxyPort;
        private String vmUuid;
        private String scheme;
        private int idleTimeout;
        private int vncTokenTimeout;
        private String tlsVersion;
        private long expiredDate;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getProxyHostname() {
            return proxyHostname;
        }

        public void setProxyHostname(String proxyHostname) {
            this.proxyHostname = proxyHostname;
        }

        public String getSslCertFile() {
            return sslCertFile;
        }

        public void setSslCertFile(String sslCertFile) {
            this.sslCertFile = sslCertFile;
        }

        public String getTargetSchema() {
            return targetSchema;
        }

        public void setTargetSchema(String targetSchema) {
            this.targetSchema = targetSchema;
        }

        public String getTargetHostname() {
            return targetHostname;
        }

        public int getProxyPort() {
            return proxyPort;
        }

        public void setProxyPort(int proxyPort) {
            this.proxyPort = proxyPort;
        }

        public void setTargetHostname(String targetHostname) {
            this.targetHostname = targetHostname;
        }

        public int getTargetPort() {
            return targetPort;
        }

        public void setTargetPort(int targetPort) {
            this.targetPort = targetPort;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getScheme() {
            return scheme;
        }

        public void setScheme(String scheme) {
            this.scheme = scheme;
        }

        public int getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(int idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public int getVncTokenTimeout() {
            return vncTokenTimeout;
        }

        public void setVncTokenTimeout(int vncTokenTimeout) {
            this.vncTokenTimeout = vncTokenTimeout;
        }

        public String getTlsVersion() {
            return tlsVersion;
        }

        public void setTlsVersion(String tlsVersion) {
            this.tlsVersion = tlsVersion;
        }

        public long getExpiredDate() {
            return expiredDate;
        }

        public void setExpiredDate(long expiredDate) {
            this.expiredDate = expiredDate;
        }
    }

    public static class EstablishProxyRsp extends AgentResponse {
        private int proxyPort;
        private String token;

        public int getProxyPort() {
            return proxyPort;
        }

        public void setProxyPort(int proxyPort) {
            this.proxyPort = proxyPort;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class PingCmd extends AgentCommand {
    }

    public static class PingRsp extends AgentResponse {
    }
}
