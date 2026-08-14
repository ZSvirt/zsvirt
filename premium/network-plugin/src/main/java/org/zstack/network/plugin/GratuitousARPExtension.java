package org.zstack.network.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigBeforeUpdateExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.vm.*;
import org.zstack.network.service.NetworkServiceManagerImpl;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

public class GratuitousARPExtension implements Component {
    private static final CLogger logger = Utils.getLogger(NetworkServiceManagerImpl.class);

    @Autowired
    FlatGratuitousARPBackend backend;
    @Autowired
    EventFacade evtf;
    @Autowired
    ResourceDestinationMaker destinationMaker;

    @Override
    public boolean start() {

        GratuitousARPGlobalConfig.SEND_GRATUITOUS_ARP_INTERVAL.installLocalBeforeUpdateExtension(new GlobalConfigBeforeUpdateExtensionPoint() {
             @Override
             public void beforeUpdateExtensionPoint(GlobalConfig oldConfig, String newValue) {
                 if (!newValue.equals(GratuitousARPGlobalConfig.SEND_GRATUITOUS_ARP_INTERVAL.value(Integer.class).toString())) {
                     backend.updateGratuitousARPSettings(newValue, GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP, null);
                 }
             }
         });

        evtf.on(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                if (!GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP) {
                    return;
                }

                VmCanonicalEvents.VmStateChangedData d = (VmCanonicalEvents.VmStateChangedData) data;
                if (!destinationMaker.isManagedByUs(d.getVmUuid())) {
                    return;
                }

                if (d.getNewState().equals(VmInstanceState.Running.toString())) {
                    GratuitousARPStruct struct = new GratuitousARPStruct();
                    struct.setVm(d.getInventory());

                    backend.applyGratuitousARP(struct, new NopeCompletion());
                } else if (d.getNewState().equals(VmInstanceState.Destroyed.toString())) {
                    GratuitousARPStruct struct = new GratuitousARPStruct();
                    struct.setVm(d.getInventory());

                    backend.releaseGratuitousARP(struct, new NopeCompletion());
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
