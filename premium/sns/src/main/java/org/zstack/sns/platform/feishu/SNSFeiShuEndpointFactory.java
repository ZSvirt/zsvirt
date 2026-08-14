package org.zstack.sns.platform.feishu;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.sns.*;

import java.util.List;
import java.util.stream.Collectors;

public class SNSFeiShuEndpointFactory implements SNSApplicationEndpointFactory {
    public static final SNSApplicationEndpointType type = new SNSApplicationEndpointType("FeiShu");

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public SNSApplicationEndpointVO createApplicationEndpoint(SNSApplicationEndpointVO vo, APICreateSNSApplicationEndpointMsg msg) {
        SNSFeiShuEndpointVO dvo = new SNSFeiShuEndpointVO(vo);
        APICreateSNSFeiShuEndpointMsg dmsg = (APICreateSNSFeiShuEndpointMsg) msg;
        if (dmsg.getAtAll() != null) {
            dvo.setAtAll(dmsg.getAtAll());
        }
        if (StringUtils.isNotBlank(dmsg.getSecret())) {
            dvo.setSecret(dmsg.getSecret());
        }
        dvo.setUrl(dmsg.getUrl());
        if (dmsg.getAtPersonUserIds() != null) {
            dvo.setAtPersons(dmsg.getAtPersonUserIds().stream().map(userId -> {
                SNSFeiShuAtPersonVO avo = new SNSFeiShuAtPersonVO();
                avo.setEndpointUuid(dvo.getUuid());
                avo.setUserId(userId);
                avo.setUuid(Platform.getUuid());
                return avo;
            }).collect(Collectors.toSet()));
        }
        if (dmsg.getAtPersonList() != null) {
            dvo.setAtPersons(dmsg.getAtPersonList().entrySet().stream().map(entry -> {
                SNSFeiShuAtPersonVO avo = new SNSFeiShuAtPersonVO();
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
        return SNSFeiShuEndpointInventory.valueOf((SNSFeiShuEndpointVO)vo);
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint(String uuid) {
        return new SNSFeiShuEndpoint(dbf.findByUuid(uuid, SNSFeiShuEndpointVO.class));
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint() {
        return new SNSFeiShuEndpoint();
    }

    @Override
    public List<SNSApplicationEndpoint> getSNSApplicationEndpoints(List<String> uuids) {
        throw new CloudRuntimeException("not supported");
    }
}
