package org.zstack.snmp.agent;

import org.snmp4j.smi.OctetString;
import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;

/**
 * @Author : jingwang
 * @create 2023/7/14 4:40 PM
 */
public interface SnmpAgent extends SnmpV2cAgent, SnmpV3Agent {
    OctetString sysDescr =
            new OctetString("Cloud - "+
                    System.getProperty("os.name","")+
                    " - "+System.getProperty("os.arch")+
                    " - "+System.getProperty("os.version"));

    enum SnmpAgentState {
        STATE_CREATED(0),
        STATE_INIT_STARTED(10),
        STATE_INIT_FINISHED(20),
        STATE_RUNNING(40),
        STATE_STOPPED(30)
        ;

        int value;
        SnmpAgentState(int i) {
            this.value = i;
        }
    }

    void start(ReturnValueCompletion<SnmpAgentInventory> completion);
    void stop(ReturnValueCompletion<SnmpAgentInventory> completion);
    void update(ReturnValueCompletion<SnmpAgentInventory> completion);
}
