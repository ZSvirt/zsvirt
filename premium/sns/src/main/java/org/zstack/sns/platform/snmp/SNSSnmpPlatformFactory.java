package org.zstack.sns.platform.snmp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.sns.*;

/**
 * @author : jingwang
 * @date 2023/8/30 10:59
 */
public class SNSSnmpPlatformFactory implements SNSApplicationPlatformFactory {
    @Autowired
    private DatabaseFacade dbf;

    public static final SNSApplicationPlatformType type = new SNSApplicationPlatformType(SNSConstants.SNMP_PLATFORM);
    public static final SNSApplicationEndpointType endpointType = new SNSApplicationEndpointType(SNSConstants.SNMP_PLATFORM);

    @Override
    public SNSApplicationPlatformVO createApplicationPlatform(SNSApplicationPlatformVO vo, APICreateSNSApplicationPlatformMsg msg) {
        SNSSnmpPlatformVO svo = new SNSSnmpPlatformVO(vo);
        APICreateSNSSnmpPlatformMsg smsg = (APICreateSNSSnmpPlatformMsg) msg;
        svo.setSnmpPort(smsg.getSnmpPort());
        svo.setSnmpAddress(smsg.getSnmpAddress());
        return svo;
    }

    @Override
    public String getApplicationPlatformType() {
        return SNSConstants.SNMP_PLATFORM;
    }

    @Override
    public SNSApplicationPlatformInventory getSNSApplicationPlatformInventory(SNSApplicationPlatformVO vo) {
        return SNSApplicationPlatformInventory.valueOf(vo);
    }

    @Override
    public SNSApplicationPlatform getSNSApplicationPlatform(String uuid) {
        return new SNSSnmpApplicationPlatform(dbf.findByUuid(uuid, SNSApplicationPlatformVO.class));
    }
}
