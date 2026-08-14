package org.zstack.sns.platform.snmp;

import org.zstack.sns.SNSApplicationPlatformVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 *
 * @Author : jingwang
 * @create 2023/8/24 14:08
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class SNSSnmpPlatformVO extends SNSApplicationPlatformVO {
    @Column
    private String snmpAddress;
    @Column
    private int snmpPort;

    public SNSSnmpPlatformVO() {
    }

    public SNSSnmpPlatformVO(SNSApplicationPlatformVO other) {
        super(other);
    }

    public String getSnmpAddress() {
        return snmpAddress;
    }

    public void setSnmpAddress(String snmpAddress) {
        this.snmpAddress = snmpAddress;
    }

    public int getSnmpPort() {
        return snmpPort;
    }

    public void setSnmpPort(int snmpPort) {
        this.snmpPort = snmpPort;
    }
}
