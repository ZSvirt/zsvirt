package org.zstack.xdragon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.header.core.Completion;
import org.zstack.header.host.ChangeHostStateMsg;
import org.zstack.header.host.ConnectHostInfo;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.UpdateHostOSMsg;
import org.zstack.header.message.Message;
import org.zstack.kvm.KVMHost;
import org.zstack.kvm.KVMHostContext;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class XDragonHost extends KVMHost {
    private static final CLogger logger = Utils.getLogger(XDragonHost.class);

    @Autowired
    @Qualifier("XDragonHostFactory")
    private XDragonHostFactory factory;

    XDragonHost(XDragonHostVO self, KVMHostContext context) {
        super(self, context);
        this.self = self;
    }

    private XDragonHostVO getSelf() {
        return (XDragonHostVO) self;
    }

    @Override
    protected int getVmMigrateQuantity() {
        return 0;
    }

    @Override
    protected void updateOsHook(UpdateHostOSMsg msg, Completion completion) {
        completion.success();
    }

    @Override
    public void connectHook(final ConnectHostInfo info, final Completion complete) {
        logger.info("connecting to xdragon host and skipping these packages: " + XDragonGlobalProperty.SKIP_PACKAGES);
        info.setSkipPackages(XDragonGlobalProperty.SKIP_PACKAGES);
        super.connectHook(info, complete);
    }

    @Override
    protected void maintenanceHook(ChangeHostStateMsg msg, Completion completion) {
        completion.success();
    }

    protected HostInventory getSelfInventory() {
        return XDragonHostInventory.valueOf(getSelf());
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
    }
}
