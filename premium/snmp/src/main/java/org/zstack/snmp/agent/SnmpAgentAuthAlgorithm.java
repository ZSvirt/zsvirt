package org.zstack.snmp.agent;

import org.snmp4j.security.*;
import org.snmp4j.smi.OID;

/**
 *
 * @Author : jingwang
 * @create 2023/7/20 6:11 PM
 */
public enum SnmpAgentAuthAlgorithm {
    MD5(AuthMD5.ID),
    SHA(AuthSHA.ID),
    SHA224(AuthHMAC128SHA224.ID),
    SHA256(AuthHMAC192SHA256.ID),
    SHA384(AuthHMAC256SHA384.ID),
    SHA512(AuthHMAC384SHA512.ID)
    ;

    OID oid;
    SnmpAgentAuthAlgorithm(OID oid) {
        this.oid = oid;
    }
}
