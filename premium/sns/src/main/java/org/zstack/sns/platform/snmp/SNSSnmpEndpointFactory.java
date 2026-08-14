package org.zstack.sns.platform.snmp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.sns.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @Author : jingwang
 * @create 2023/7/24 7:35 PM
 */
public class SNSSnmpEndpointFactory implements SNSApplicationEndpointFactory {
    public static final SNSApplicationEndpointType type = new SNSApplicationEndpointType("SNMP");
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public SNSApplicationEndpointVO createApplicationEndpoint(SNSApplicationEndpointVO vo, APICreateSNSApplicationEndpointMsg msg) {
        return vo;
    }

    @Override
    public String getApplicationEndpointType() {
        return type.toString();
    }

    @Override
    public SNSApplicationEndpointInventory getSNSApplicationEndpointInventory(SNSApplicationEndpointVO vo) {
        return SNSApplicationEndpointInventory.valueOf(vo);
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint(String uuid) {
        return new SNSSnmpEndpoint(dbf.findByUuid(uuid, SNSApplicationEndpointVO.class));
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint() {
        return new SNSSnmpEndpoint();
    }

    @Override
    public List<SNSApplicationEndpoint> getSNSApplicationEndpoints(List<String> uuids) {
        List<SNSApplicationEndpointVO> vos = Q.New(SNSApplicationEndpointVO.class).in(SNSApplicationEndpointVO_.uuid, uuids).list();
        return vos.stream().map(SNSSnmpEndpoint::new).collect(Collectors.toList());
    }
}
