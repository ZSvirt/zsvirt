package org.zstack.sns.platform.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.sns.*;

import java.util.List;

public class SNSHttpEndpointFactory implements SNSApplicationEndpointFactory {
    public static final SNSApplicationEndpointType type = new SNSApplicationEndpointType("HTTP");

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public SNSApplicationEndpointVO createApplicationEndpoint(SNSApplicationEndpointVO vo, APICreateSNSApplicationEndpointMsg msg) {
        SNSHttpEndpointVO hvo = new SNSHttpEndpointVO(vo);
        APICreateSNSHttpEndpointMsg hmsg = (APICreateSNSHttpEndpointMsg) msg;
        hvo.setUsername(hmsg.getUsername());
        hvo.setPassword(hmsg.getPassword());
        hvo.setUrl(hmsg.getUrl());
        return hvo;
    }

    @Override
    public String getApplicationEndpointType() {
        return type.toString();
    }

    @Override
    public SNSApplicationEndpointInventory getSNSApplicationEndpointInventory(SNSApplicationEndpointVO vo) {
        return SNSHttpEndpointInventory.valueOf((SNSHttpEndpointVO)vo);
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint(String uuid) {
        return new SNSHttpEndpoint(dbf.findByUuid(uuid, SNSHttpEndpointVO.class));
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint() {
        return new SNSHttpEndpoint();
    }

    @Override
    public List<SNSApplicationEndpoint> getSNSApplicationEndpoints(List<String> uuids) {
        throw new CloudRuntimeException("not supported");
    }
}
