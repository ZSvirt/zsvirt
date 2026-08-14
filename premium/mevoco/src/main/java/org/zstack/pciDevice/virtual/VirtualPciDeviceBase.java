package org.zstack.pciDevice.virtual;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.message.Message;
import org.zstack.identity.AccountManager;

/**
 * Created by GuoYi on 2019-04-24.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualPciDeviceBase implements VirtualPciDevice {
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected AccountManager acntMgr;

    @Override
    public void handleMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }
}
