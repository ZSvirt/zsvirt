package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.vm.APIStartVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApplianceVmSyncConfigAfterAddToHaGroupFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(ApplianceVmSyncConfigAfterAddToHaGroupFlow.class);

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected CloudBus bus;

    private void syncApplianceVmConfigAfterAddToHaGroup(Iterator<ApplianceVmSyncConfigToHaGroupExtensionPoint> it,
                                                        ApplianceVmInventory applianceVm, String haUuid, NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        ApplianceVmSyncConfigToHaGroupExtensionPoint ext = it.next();
        ext.applianceVmSyncConfigAfterAddToHaGroup(applianceVm, haUuid,  new NoErrorCompletion(completion) {
            @Override
            public void done() {
                syncApplianceVmConfigAfterAddToHaGroup(it, applianceVm, haUuid, completion);
            }
        });
    }

    @Override
    public void run(final FlowTrigger chain, Map data) {
        /* action in this flow depends on ApplianceVmSyncConfigToHaGroupFlow
        * and this flow must be the last flow of start virtualrouter, or it need rollback */
        ApplianceVmVO applianceVmVO = (ApplianceVmVO)data.get(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_applianceVm.toString());
        String haUuid = (String)data.get(VmInstanceConstant.Params.ApplianceVmSyncHaConfig_haUuid.toString());

        if (applianceVmVO == null || haUuid == null) {
            chain.next();
            return;
        }

        Iterator<ApplianceVmSyncConfigToHaGroupExtensionPoint> iterator = pluginRgty.getExtensionList(ApplianceVmSyncConfigToHaGroupExtensionPoint.class).iterator();
        syncApplianceVmConfigAfterAddToHaGroup(iterator, ApplianceVmInventory.valueOf(applianceVmVO), haUuid, new NoErrorCompletion(chain) {
            @Override
            public void done() {
                chain.next();
            }
        });
    }
}
