package org.zstack.core.externalservice;

/**
 * Created by mingjian.deng on 2019/12/13.
 */
public class LocalServiceUnitConfig {
    private String serviceUnitContent;
    private String serviceUnitPath;
    private String configFileContent;
    private String configFilePath;

    public String getServiceUnitContent() {
        return serviceUnitContent;
    }

    public void setServiceUnitContent(String serviceUnitContent) {
        this.serviceUnitContent = serviceUnitContent;
    }

    public String getServiceUnitPath() {
        return serviceUnitPath;
    }

    public void setServiceUnitPath(String serviceUnitPath) {
        this.serviceUnitPath = serviceUnitPath;
    }

    public String getConfigFileContent() {
        return configFileContent;
    }

    public void setConfigFileContent(String configFileContent) {
        this.configFileContent = configFileContent;
    }

    public String getConfigFilePath() {
        return configFilePath;
    }

    public void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }
}
