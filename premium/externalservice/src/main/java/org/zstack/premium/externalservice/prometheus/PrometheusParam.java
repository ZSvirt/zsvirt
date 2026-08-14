package org.zstack.premium.externalservice.prometheus;

import org.zstack.header.exception.CloudRuntimeException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PrometheusParam {
    private String binaryPath;
    private String ip;
    private int port;
    private List<String> parameters = new ArrayList<>();
    private String logPath;
    private String storagePath;
    private String confPath;
    private String retention;
    private String retentionSize;
    private String servicePath;
    private String walBlockMinRetention;
    private String version;
    private int startUpTimeout = 20;
    private boolean basicAuthSwitch;
    private String basicAuthUsername;
    private String basicAuthPassword;


    public PrometheusParam() {
        version = PrometheusGlobalProperty.VERSION_MODE;
    }

    public String getRetention() {
        return retention;
    }

    public void setRetention(String retention) {
        this.retention = retention;
    }

    public String getRetentionSize() {
        return retentionSize;
    }

    public void setRetentionSize(String retentionSize) {
        this.retentionSize = retentionSize;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    protected void checkParams() {
        if (!new File(binaryPath).exists()) {
            throw new CloudRuntimeException(String.format("cannot find prometheus binary at %s", binaryPath));
        }
    }

    public int getStartUpTimeout() {
        return startUpTimeout;
    }

    public void setStartUpTimeout(int startUpTimeout) {
        this.startUpTimeout = startUpTimeout;
    }

    public String getConfPath() {
        return confPath;
    }

    public void setConfPath(String confPath) {
        this.confPath = confPath;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getBinaryPath() {
        return binaryPath;
    }

    public void setBinaryPath(String binaryPath) {
        this.binaryPath = binaryPath;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public String getServicePath() {
        return servicePath;
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getWalBlockMinRetention() {
        return walBlockMinRetention;
    }

    public void setWalBlockMinRetention(String walBlockMinRetention) {
        this.walBlockMinRetention = walBlockMinRetention;
    }

    public boolean getBasicAuthSwitch() {
        return basicAuthSwitch;
    }

    public void setBasicAuthSwitch(boolean basicAuthSwitch) {
        this.basicAuthSwitch = basicAuthSwitch;
    }

    public String getBasicAuthUsername() {
        return basicAuthUsername;
    }

    public void setBasicAuthUsername(String basicAuthUsername) {
        this.basicAuthUsername = basicAuthUsername;
    }

    public String getBasicAuthPassword() {
        return basicAuthPassword;
    }

    public void setBasicAuthPassword(String basicAuthPassword) {
        this.basicAuthPassword = basicAuthPassword;
    }
}
