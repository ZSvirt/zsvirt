package org.zstack.snmp;

import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.zstack.core.Platform;
import org.zstack.snmp.agent.mib.MOColumnFactory;
import org.zstack.sns.platform.snmp.SnmpSender;

import java.io.IOException;

public class SnmpUtils {
    public static Snmp createSnmpReceiver(int port) {
        Snmp snmp = null;
        try {
            UdpAddress address = new UdpAddress("0.0.0.0/" + port);
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping(address);
            snmp = new Snmp(transport);
            transport.listen();
            return snmp;
        } catch (Exception e) {
            try {
                if (snmp != null) {
                    snmp.close();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * @return return null if snmp send timeout.
     * return `noSuchInstance` if oid is not exist.
     */
    public static String getAsString(OID oid, Target target) throws IOException {
        ResponseEvent event;
        if (target instanceof UserTarget) {
            event = v3Get(oid, target);
        } else {
            event = v2Get(oid, target);
        }
        if (event == null || event.getResponse() == null) {
            return null;
        }
        return event.getResponse().get(0).getVariable().toString();
    }

    private static ResponseEvent v2Get(OID oid, Target target) throws IOException {
        Snmp snmp = SnmpSender.createSnmpInstance();
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(oid));
        pdu.setType(PDU.GET);
        ResponseEvent event = snmp.send(pdu, target, null);
        snmp.close();
        return event;
    }

    private static ResponseEvent v3Get(OID oid, Target target) throws IOException {
        Snmp snmp = SnmpSender.createSnmpInstance();
        USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(), 0);
        usm.addUser(new OctetString(target.getSecurityName()), new UsmUser(target.getSecurityName(), null, null, null, null));
        SecurityModels.getInstance().addSecurityModel(usm);
        PDU pdu = new ScopedPDU();
        pdu.add(new VariableBinding(oid));
        pdu.setType(PDU.GET);
        ResponseEvent event = snmp.send(pdu, target, null);
        snmp.close();
        return event;
    }

    public static CommunityTarget getCommunityTarget(String ipAddress,int port, String community, long timeout, int retries) {
        // udp:0.0.0.0/162
        Address targetAddress = GenericAddress.parse("udp:" + ipAddress + "/" + port);
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setAddress(targetAddress);
        target.setRetries(retries);
        target.setTimeout(timeout);
        target.setVersion(SnmpConstants.version2c);
        return target;
    }

    public static UserTarget getUserTarget(String ipAddress, int port, String userName, long timeout, int retries) {
        UserTarget target = new UserTarget();
        Address targetAddress = GenericAddress.parse("udp:" + ipAddress + "/" + port);
        target.setAddress(targetAddress);
        target.setVersion(SnmpConstants.version3);
        target.setSecurityName(new OctetString(userName));
        target.setTimeout(timeout);
        target.setRetries(retries);
        return target;
    }

    public static OID transformUuidToIndexOID(String uuid) {
        String[] parts = uuid.split("(?<=\\G.{4})");
        int[] numbers = new int[8];
        for (int i = 0; i < 8; i++) {
            numbers[i] = Integer.parseInt(parts[i], 16);
        }
        return new OID(numbers);
    }

    public static String transformIndexOIDToUuid(OID index) {
        int[] numbers = index.getValue();
        StringBuilder sb = new StringBuilder();
        for (int number : numbers) {
            sb.append(String.format("%04x", number));
        }
        return sb.toString();
    }

    public static String getRegisteredOid(String uuid, String metricName,Class<? extends MOColumnFactory> clz,String resourceOid) {
        OID rootOID = new OID(resourceOid);
        MOColumnFactory moColumnFactory = Platform.getComponentLoader().getComponent(clz);
        int columnIndex = moColumnFactory.getColumnIndex(metricName);
        rootOID.append(columnIndex);
        OID index = transformUuidToIndexOID(uuid);
        return rootOID.append(index).toString();
    }
}
