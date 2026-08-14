package org.zstack.sns.platform.snmp;

import org.apache.commons.lang.StringUtils;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.zstack.core.Platform;
import org.zstack.header.core.Completion;
import org.zstack.header.message.APIMessage;
import org.zstack.sns.MessageStruct;
import org.zstack.sns.SNSApplicationEndpointBase;
import org.zstack.sns.SNSApplicationEndpointVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @Author : jingwang
 * @create 2023/7/24 11:04 PM
 */
public class SNSSnmpEndpoint extends SNSApplicationEndpointBase {
    private static final CLogger logger = Utils.getLogger(SNSSnmpEndpoint.class);

    public static final String ALARM_UUID_OID = "1.3.6.1.4.1.60687.1.1.1.1.1";
    public static final String ALARM_NAMESPACE_OID = "1.3.6.1.4.1.60687.1.1.1.1.2";
    public static final String ALARM_CONDITION = "1.3.6.1.4.1.60687.1.1.1.1.3";
    public static final String ALARM_DURATION_OID = "1.3.6.1.4.1.60687.1.1.1.1.4";
    public static final String ALARM_PREVIOUS_STATUS_OID = "1.3.6.1.4.1.60687.1.1.1.1.5";
    public static final String ALARM_CURRENT_VALUE_OID = "1.3.6.1.4.1.60687.1.1.1.1.6";
    public static final String ALARM_RESOURCE_ID_OID = "1.3.6.1.4.1.60687.1.1.1.1.7";
    public static final String ALARM_RESOURCE_NAME_OID = "1.3.6.1.4.1.60687.1.1.1.1.8";
    public static final String ALARM_EMERGENCY_LEVEL_OID = "1.3.6.1.4.1.60687.1.1.1.1.9";
    public static final String ALARM_LABELS_OID = "1.3.6.1.4.1.60687.1.1.1.1.10";
    public static final String ALARM_TIME_OID = "1.3.6.1.4.1.60687.1.1.1.1.11";
    public static final String ALARM_SNMP_TRAP_OID = "1.3.6.1.4.1.60687.1.1.1.0.1";

    public static final String EVENT_NAME_OID = "1.3.6.1.4.1.60687.1.1.1.2.1";
    public static final String EVENT_NAMESPACE_OID = "1.3.6.1.4.1.60687.1.1.1.2.2";
    public static final String EVENT_EMERGENCY_LEVEL_OID = "1.3.6.1.4.1.60687.1.1.1.2.3";
    public static final String EVENT_RESOURCE_ID_OID = "1.3.6.1.4.1.60687.1.1.1.2.4";
    public static final String EVENT_RESOURCE_NAME_OID = "1.3.6.1.4.1.60687.1.1.1.2.5";
    public static final String EVENT_TIME_OID = "1.3.6.1.4.1.60687.1.1.1.2.6";
    public static final String EVENT_SUBSCRIPTION_UUID_OID = "1.3.6.1.4.1.60687.1.1.1.2.7";
    public static final String EVENT_LABELS_OID = "1.3.6.1.4.1.60687.1.1.1.2.8";
    public static final String EVENT_ERROR_OID = "1.3.6.1.4.1.60687.1.1.1.2.9";
    public static final String EVENT_SNMP_TRAP_OID = "1.3.6.1.4.1.60687.1.1.1.0.2";

    public SNSSnmpEndpoint() {}
    public SNSSnmpEndpoint(SNSApplicationEndpointVO self) {
        super(self);
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APISNSSnmpTestConnectionMsg) {
            handle((APISNSSnmpTestConnectionMsg)msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APISNSSnmpTestConnectionMsg msg) {
        APISNSSnmpTestConnectionEvent evt = new APISNSSnmpTestConnectionEvent(msg.getId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(ALARM_SNMP_TRAP_OID, true);
        metadata.put(ALARM_UUID_OID, new OctetString(UUID.randomUUID().toString()));
        metadata.put(ALARM_NAMESPACE_OID, new OctetString("ZStack::Host"));
        metadata.put(ALARM_CONDITION, new OctetString(""));
        metadata.put(ALARM_DURATION_OID, new OctetString("100"));
        metadata.put(ALARM_PREVIOUS_STATUS_OID, new OctetString("ALARM"));
        metadata.put(ALARM_CURRENT_VALUE_OID, new OctetString("ALARM"));
        metadata.put(ALARM_RESOURCE_ID_OID, new OctetString(UUID.randomUUID().toString()));
        metadata.put(ALARM_RESOURCE_NAME_OID, new OctetString("Test-Host"));
        metadata.put(ALARM_EMERGENCY_LEVEL_OID, new OctetString("Warning"));
        metadata.put(ALARM_LABELS_OID, new OctetString(""));
        metadata.put(ALARM_TIME_OID, new OctetString(System.currentTimeMillis() + ""));
        metadata.put(SnmpConstants.snmpTrapOID.toString(), new OID(ALARM_SNMP_TRAP_OID));

        MessageStruct messageStruct = new MessageStruct();
        messageStruct.setMessage("SNMP test msg");
        messageStruct.setMetadata(metadata);

        Map<String, Variable> m = new HashMap<>();
        populateAlarm(m, messageStruct);
        try {
            SNSSnmpPlatformVO platformVO;
            if (StringUtils.isNotBlank(msg.getPlatformUuid())) {
                platformVO = dbf.findByUuid(msg.getPlatformUuid(), SNSSnmpPlatformVO.class);
            } else {
                SNSApplicationEndpointVO endpointVO = dbf.findByUuid(msg.getEndpointUuid(), SNSApplicationEndpointVO.class);
                platformVO = (SNSSnmpPlatformVO) endpointVO.getPlatform();
            }

            String address = platformVO.getSnmpAddress();
            int port = platformVO.getSnmpPort();
            SnmpSender.v1Trap(address, port, m);
            evt.setConnected(true);
        } catch (Exception e) {
            logger.error(Platform.ioerr("failed to test send snmp trap, due to", e.getMessage()).toString());
            evt.setConnected(false);
        }
        bus.publish(evt);
    }

    @Override
    public void publish(MessageStruct message, Completion completion) {
        if (message.getMetadata() == null) {
            logger.debug("SNS message field not defined for SNMP");
            completion.success();
            return;
        }

        Map<String, Variable> m = new HashMap<>();
        if (message.getMetadata().containsKey(ALARM_SNMP_TRAP_OID)) {
            populateAlarm(m, message);
        } else {
            populateEvent(m, message);
        }
        try {
            String address = ((SNSSnmpPlatformVO)getSelf().getPlatform()).getSnmpAddress();
            int port = ((SNSSnmpPlatformVO)getSelf().getPlatform()).getSnmpPort();
            SnmpSender.v1Trap(address, port, m);
            completion.success();
        } catch (Exception e) {
            completion.fail(Platform.ioerr("failed to send snmp trap, due to", e.getMessage()));
        }
    }

    private void populateEvent(Map<String, Variable> m, MessageStruct message) {
        m.put(EVENT_NAME_OID, new OctetString(message.getMetadata().get(EVENT_NAME_OID).toString()));
        m.put(EVENT_NAMESPACE_OID, new OctetString(message.getMetadata().get(EVENT_NAMESPACE_OID).toString()));
        m.put(EVENT_EMERGENCY_LEVEL_OID, new OctetString(message.getMetadata().get(EVENT_EMERGENCY_LEVEL_OID).toString()));
        m.put(EVENT_RESOURCE_ID_OID, new OctetString(message.getMetadata().get(EVENT_RESOURCE_ID_OID).toString()));
        m.put(EVENT_RESOURCE_NAME_OID, new OctetString(message.getMetadata().get(EVENT_RESOURCE_NAME_OID).toString()));
        m.put(EVENT_TIME_OID, new OctetString(message.getMetadata().get(EVENT_TIME_OID).toString()));
        m.put(EVENT_SUBSCRIPTION_UUID_OID, new OctetString(message.getMetadata().get(EVENT_SUBSCRIPTION_UUID_OID).toString()));
        m.put(EVENT_LABELS_OID, new OctetString(message.getMetadata().get(EVENT_LABELS_OID).toString()));
        m.put(EVENT_ERROR_OID, new OctetString(message.getMetadata().get(EVENT_ERROR_OID).toString()));
        m.put(SnmpConstants.snmpTrapOID.toString(), new OID(EVENT_SNMP_TRAP_OID));
    }

    private void populateAlarm(Map<String, Variable> m, MessageStruct message) {
        m.put(ALARM_UUID_OID, new OctetString(message.getMetadata().get(ALARM_UUID_OID).toString()));
        m.put(ALARM_NAMESPACE_OID, new OctetString(message.getMetadata().get(ALARM_NAMESPACE_OID).toString()));
        m.put(ALARM_CONDITION, new OctetString(message.getMetadata().get(ALARM_CONDITION).toString()));
        m.put(ALARM_DURATION_OID, new OctetString(message.getMetadata().get(ALARM_DURATION_OID).toString()));
        m.put(ALARM_PREVIOUS_STATUS_OID, new OctetString(message.getMetadata().get(ALARM_PREVIOUS_STATUS_OID).toString()));
        m.put(ALARM_CURRENT_VALUE_OID, new OctetString(message.getMetadata().get(ALARM_CURRENT_VALUE_OID).toString()));
        m.put(ALARM_RESOURCE_ID_OID, new OctetString(message.getMetadata().get(ALARM_RESOURCE_ID_OID).toString()));
        m.put(ALARM_RESOURCE_NAME_OID, new OctetString(message.getMetadata().get(ALARM_RESOURCE_NAME_OID).toString()));
        m.put(ALARM_EMERGENCY_LEVEL_OID, new OctetString(message.getMetadata().get(ALARM_EMERGENCY_LEVEL_OID).toString()));
        m.put(ALARM_LABELS_OID, new OctetString(message.getMetadata().get(ALARM_LABELS_OID).toString()));
        m.put(ALARM_TIME_OID, new OctetString(message.getMetadata().get(ALARM_TIME_OID).toString()));
        m.put(SnmpConstants.snmpTrapOID.toString(), new OID(ALARM_SNMP_TRAP_OID));
    }
}
