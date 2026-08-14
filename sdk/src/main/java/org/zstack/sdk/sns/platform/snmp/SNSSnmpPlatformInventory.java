package org.zstack.sdk.sns.platform.snmp;



public class SNSSnmpPlatformInventory extends org.zstack.sdk.sns.SNSApplicationPlatformInventory {

    public java.lang.String snmpAddress;
    public void setSnmpAddress(java.lang.String snmpAddress) {
        this.snmpAddress = snmpAddress;
    }
    public java.lang.String getSnmpAddress() {
        return this.snmpAddress;
    }

    public int snmpPort;
    public void setSnmpPort(int snmpPort) {
        this.snmpPort = snmpPort;
    }
    public int getSnmpPort() {
        return this.snmpPort;
    }

}
