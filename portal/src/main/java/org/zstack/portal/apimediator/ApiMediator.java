package org.zstack.portal.apimediator;

import org.zstack.header.Service;

public interface ApiMediator extends Service {
    ApiMessageProcessor getProcesser();
}
