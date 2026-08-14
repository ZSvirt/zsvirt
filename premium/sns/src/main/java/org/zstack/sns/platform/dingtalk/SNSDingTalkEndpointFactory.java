package org.zstack.sns.platform.dingtalk;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.sns.*;

import java.util.List;
import java.util.stream.Collectors;

public class SNSDingTalkEndpointFactory implements SNSApplicationEndpointFactory {
    public static final SNSApplicationEndpointType type = new SNSApplicationEndpointType("DingTalk");

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public SNSApplicationEndpointVO createApplicationEndpoint(SNSApplicationEndpointVO vo, APICreateSNSApplicationEndpointMsg msg) {
        SNSDingTalkEndpointVO dvo = new SNSDingTalkEndpointVO(vo);
        APICreateSNSDingTalkEndpointMsg dmsg = (APICreateSNSDingTalkEndpointMsg) msg;
        if (dmsg.getAtAll() != null) {
            dvo.setAtAll(dmsg.getAtAll());
        }
        if (StringUtils.isNotBlank(dmsg.getSecret())) {
            dvo.setSecret(dmsg.getSecret());
        }
        dvo.setUrl(dmsg.getUrl());
        if (dmsg.getAtPersonPhoneNumbers() != null) {
            dvo.setAtPersons(dmsg.getAtPersonPhoneNumbers().stream().map(pn -> {
                SNSDingTalkAtPersonVO avo = new SNSDingTalkAtPersonVO();
                avo.setEndpointUuid(dvo.getUuid());
                avo.setPhoneNumber(pn);
                avo.setUuid(Platform.getUuid());
                return avo;
            }).collect(Collectors.toSet()));
        }
        if (dmsg.getAtPersonList() != null) {
            dvo.setAtPersons(dmsg.getAtPersonList().entrySet().stream().map(entry -> {
                SNSDingTalkAtPersonVO avo = new SNSDingTalkAtPersonVO();
                avo.setEndpointUuid(dvo.getUuid());
                avo.setPhoneNumber(entry.getKey());
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
        return SNSDingTalkEndpointInventory.valueOf((SNSDingTalkEndpointVO) vo);
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint(String uuid) {
        return new SNSDingTalkEndpoint(dbf.findByUuid(uuid, SNSDingTalkEndpointVO.class));
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint() {
        return new SNSDingTalkEndpoint();
    }

    @Override
    public List<SNSApplicationEndpoint> getSNSApplicationEndpoints(List<String> uuids) {
        throw new CloudRuntimeException("not supported");
    }
}
