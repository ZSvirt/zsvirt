package org.zstack.sns.platform.snmp;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;


public class SnmpSender {
    private static final CLogger logger = Utils.getLogger(SnmpSender.class);
    private static final String CLOUD_SNMP_ENTERPRISE_OID = "1.3.6.1.4.1.60687";
    private static final long DEFAULT_SNMP_AGENT_SEND_TRAP_TIMEOUT_MS = 3000L;
    private static final int DEFAULT_SNMP_AGENT_SEND_TRAP_RETRIES = 1;
    private static final int GENERIC_TRAP_ENTERPRISE_SPECIFIC = 6;

    private static final AtomicReference<Snmp> sharedSnmpRef = new AtomicReference<>();

    /**
     * Send SNMP v1 Trap
     *
     * @param ipAddress target IP address
     * @param port      Target Port
     * @param binds     OID binds map
     * @throws IOException if SNMP trap sending fails or network error occurs
     */
    public static void v1Trap(String ipAddress, int port, Map<String, Variable> binds) throws IOException {
        try {
            Snmp snmp = getOrCreateSnmpInstance();
            PDUv1 pdu = createPDU(binds);
            CommunityTarget target = createTarget(ipAddress, port);
            snmp.trap(pdu, target);
            logger.debug("SNMP trap sent successfully to " + ipAddress + ":" + port);
        } catch (IOException e) {
            // close snmp instance on failure
            closeSharedInstance();
            throw e;
        } catch (Exception e) {
            // close snmp instance on failure
            closeSharedInstance();
            throw new CloudRuntimeException("Failed to send SNMP trap to " + ipAddress + ":" + port, e);
        }
    }

    /**
     * create pdu
     */
    private static PDUv1 createPDU(Map<String, Variable> binds) {

        if (binds == null || binds.isEmpty()) {
            throw new CloudRuntimeException("Binds parameter cannot be " + (binds == null ? "null" : "empty"));
        }

        PDUv1 pdu = new PDUv1();
        pdu.setEnterprise(new OID(CLOUD_SNMP_ENTERPRISE_OID));
        pdu.setGenericTrap(GENERIC_TRAP_ENTERPRISE_SPECIFIC);

        binds.forEach((oid, value) -> {
            pdu.add(new VariableBinding(new OID(oid), value));
        });

        return pdu;
    }

    /**
     * create target
     */
    private static CommunityTarget createTarget(String ipAddress, int port) {
        CommunityTarget target = new CommunityTarget();
        target.setTimeout(DEFAULT_SNMP_AGENT_SEND_TRAP_TIMEOUT_MS);
        target.setRetries(DEFAULT_SNMP_AGENT_SEND_TRAP_RETRIES);
        target.setAddress(new UdpAddress(ipAddress + "/" + port));
        return target;
    }

    /**
     * get or create snmp instance
     */
    private static synchronized Snmp getOrCreateSnmpInstance() throws IOException {
        Snmp snmp = sharedSnmpRef.get();
        if (snmp == null) {
            snmp = createSnmpInstance();
            sharedSnmpRef.set(snmp);
        }
        return snmp;

    }

    /**
     * create snmp instance
     */
    public static Snmp createSnmpInstance() throws IOException {
        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        Snmp snmp = new Snmp(transport);
        transport.listen();
        return snmp;
    }


    /**
     * close instance
     */
    public static void closeSharedInstance() throws IOException {
        Snmp snmp = sharedSnmpRef.getAndSet(null);
        if (snmp != null) {
            snmp.close();
        }
    }

}
