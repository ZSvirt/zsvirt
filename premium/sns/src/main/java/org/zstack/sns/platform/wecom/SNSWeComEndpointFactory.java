package org.zstack.sns.platform.wecom;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.sns.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SNSWeComEndpointFactory implements SNSApplicationEndpointFactory {
    public static final SNSApplicationEndpointType type = new SNSApplicationEndpointType("WeCom");

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public SNSApplicationEndpointVO createApplicationEndpoint(SNSApplicationEndpointVO vo, APICreateSNSApplicationEndpointMsg msg) {
        SNSWeComEndpointVO dvo = new SNSWeComEndpointVO(vo);
        APICreateSNSWeComEndpointMsg dmsg = (APICreateSNSWeComEndpointMsg) msg;
        if (dmsg.getAtAll() != null) {
            dvo.setAtAll(dmsg.getAtAll());
        }
        dvo.setUrl(dmsg.getUrl());
        if (dmsg.getAtPersonUserIds() != null) {
            dvo.setAtPersons(dmsg.getAtPersonUserIds().stream().map(userId -> {
                SNSWeComAtPersonVO avo = new SNSWeComAtPersonVO();
                avo.setEndpointUuid(dvo.getUuid());
                avo.setUserId(userId);
                avo.setUuid(Platform.getUuid());
                return avo;
            }).collect(Collectors.toSet()));
        }
        if (dmsg.getAtPersonList() != null) {
            dvo.setAtPersons(dmsg.getAtPersonList().entrySet().stream().map(entry -> {
                SNSWeComAtPersonVO avo = new SNSWeComAtPersonVO();
                avo.setEndpointUuid(dvo.getUuid());
                avo.setUserId(entry.getKey());
                avo.setRemark(entry.getValue());
                avo.setUuid(Platform.getUuid());
                return avo;
            }).collect(Collectors.toSet()));
        }
        return dvo;
    }

    @Override
    public String getApplicationEndpointType() {
        return type.toString();
    }

    @Override
    public SNSApplicationEndpointInventory getSNSApplicationEndpointInventory(SNSApplicationEndpointVO vo) {
        return SNSWeComEndpointInventory.valueOf((SNSWeComEndpointVO) vo);
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint(String uuid) {
        return new SNSWeComEndpoint(dbf.findByUuid(uuid, SNSWeComEndpointVO.class));
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint() {
        return new SNSWeComEndpoint();
    }

    @Override
    public List<SNSApplicationEndpoint> getSNSApplicationEndpoints(List<String> uuids) {
        throw new CloudRuntimeException("not supported");
    }
}
