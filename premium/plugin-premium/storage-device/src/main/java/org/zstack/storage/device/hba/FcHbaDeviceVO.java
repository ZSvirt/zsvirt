package org.zstack.storage.device.hba;

import javax.persistence.*;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/9/25 09:44
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class FcHbaDeviceVO extends HbaDeviceVO {
    @Column
    private String portState;
    @Column
    private String speed;
    @Column
    private String supportedSpeeds;
    @Column
    private String symbolicName;
    @Column
    private String supportedClasses;

    @Column
    private String portName;

    @Column
    private String nodeName;

    public String getPortState() {
        return portState;
    }

    public void setPortState(String portState) {
        this.portState = portState;
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

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
}
