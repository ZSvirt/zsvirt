package org.zstack.baremetal.instance;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.baremetal.network.BaremetalNic;
import org.zstack.header.baremetal.network.BaremetalNicInventory;
import org.zstack.header.baremetal.network.BaremetalNicVO;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class BaremetalNoVlanNic implements BaremetalNic {
    private static final CLogger logger = Utils.getLogger(BaremetalNoVlanNic.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    protected BaremetalNicVO self;

    public BaremetalNoVlanNic() {
    }

    public BaremetalNoVlanNic(BaremetalNicVO self) {
        this.self = self;
    }

    protected BaremetalNicInventory getSelfInventory() {
        return self.toInventory();
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }
}
