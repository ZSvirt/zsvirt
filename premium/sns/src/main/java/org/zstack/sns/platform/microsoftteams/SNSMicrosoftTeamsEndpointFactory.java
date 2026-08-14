package org.zstack.sns.platform.microsoftteams;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.sns.*;
import java.util.List;

public class SNSMicrosoftTeamsEndpointFactory implements SNSApplicationEndpointFactory {

    public static final SNSApplicationEndpointType type = new SNSApplicationEndpointType("MicrosoftTeams");

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public SNSApplicationEndpointVO createApplicationEndpoint(SNSApplicationEndpointVO vo, APICreateSNSApplicationEndpointMsg msg) {
        SNSMicrosoftTeamsEndpointVO dvo = new SNSMicrosoftTeamsEndpointVO(vo);
        APICreateSNSMicrosoftTeamsEndpointMsg dmsg = (APICreateSNSMicrosoftTeamsEndpointMsg) msg;
        dvo.setUrl(dmsg.getUrl());

        return dvo;

    }

    @Override
    public String getApplicationEndpointType() {
        return type.toString();
    }

    @Override
    public SNSApplicationEndpointInventory getSNSApplicationEndpointInventory(SNSApplicationEndpointVO vo) {
        return SNSMicrosoftTeamsEndpointInventory.valueOf((SNSMicrosoftTeamsEndpointVO)vo);
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint(String uuid) {
        return new SNSMicrosoftTeamsEndpoint(dbf.findByUuid(uuid, SNSMicrosoftTeamsEndpointVO.class));
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint() {
        return new SNSMicrosoftTeamsEndpoint();
    }

    @Override
    public List<SNSApplicationEndpoint> getSNSApplicationEndpoints(List<String> uuids) {
        throw new CloudRuntimeException("not supported");
    }
}
