package org.zstack.snmp.agent;

import org.snmp4j.security.*;
import org.snmp4j.smi.OID;

/**
 * @Author : jingwang
 * @create 2023/7/20 6:17 PM
 */
public enum SnmpAgentPrivacyAlgorithm {
    DES("DES"),
    AES128("AES128"),
    AES192("AES192"),
    AES256("AES256"),
    ThreeDES("3DES")
    ;

    String name;
    SnmpAgentPrivacyAlgorithm(String name) {
        this.name = name;
    }

    public static SnmpAgentPrivacyAlgorithm getByName(String name) {
        for(SnmpAgentPrivacyAlgorithm t : values()){
            if(t.name.equalsIgnoreCase(name)){
                return t;
            }
        }
        throw new IllegalArgumentException(String.format("SnmpAgentPrivacyAlgorithm[%s] not support", name));
    }

    public OID getOid() {
        switch (this) {
            case DES: {
                return PrivDES.ID;
            }
            case AES128: {
                return PrivAES128.ID;
            }
            case AES192: {
                return PrivAES192.ID;
            }
            case AES256: {
                return PrivAES256.ID;
            }
            case ThreeDES: {
                return Priv3DES.ID;
            }
            default: {
                return null;
            }
        }
    }
}
