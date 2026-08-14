package org.zstack.storage.device.hba;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/9/18 16:58
 */
public class HbaDeviceStruct {
    private String name;
    private String portName;
    private String portState;
    private String hbaType;
    private String speed;
    private String supportedSpeeds;
    private String symbolicName;
    private String supportedClasses;
    private String nodeName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public String getPortState() {
        return portState;
    }

    public void setPortState(String portState) {
        this.portState = portState;
    }

    public String getHbaType() {
        return hbaType;
    }

    public void setHbaType(String hbaType) {
        this.hbaType = hbaType;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public String getSupportedSpeeds() {
        return supportedSpeeds;
    }

    public void setSupportedSpeeds(String supportedSpeeds) {
        this.supportedSpeeds = supportedSpeeds;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    public String getSupportedClasses() {
        return supportedClasses;
    }

    public void setSupportedClasses(String supportedClasses) {
        this.supportedClasses = supportedClasses;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
}
