package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.ChangeVmImageExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 12/1/17.
 */
public class InstantiateVolumeForVmChangeImageExtension extends InstantiateVolumeForNewCreatedVmExtension implements ChangeVmImageExtensionPoint {
    private static final CLogger logger = Utils.getLogger(InstantiateVolumeForVmChangeImageExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        VmInstanceSpec.ImageSpec image = spec.getImageSpec();
        InstantiateVolumeMsg rmsg = new InstantiateTemporaryRootVolumeMsg();
        ((InstantiateRootVolumeMsg)rmsg).setTemplateSpec(image);
        ((InstantiateTemporaryRootVolumeMsg)rmsg).setOriginVolumeUuid(spec.getVmInventory().getRootVolumeUuid());

        rmsg.setPrimaryStorageUuid(spec.getDestRootVolume().getPrimaryStorageUuid());
        rmsg.setHostUuid(spec.getDestHost().getUuid());
        rmsg.setVolumeUuid(spec.getDestRootVolume().getUuid());
        rmsg.setPrimaryStorageAllocated(true);
        rmsg.setSkipIfExisting(spec.isInstantiateResourcesSkipExisting());
        rmsg.setAllocatedInstallUrl(spec.getAllocatedUrlFromVolumeSpecs(spec.getDestRootVolume().getUuid()));
        bus.makeTargetServiceIdByResourceUuid(rmsg, VolumeConstant.SERVICE_ID, spec.getDestRootVolume().getUuid());

        doInstantiate(Collections.singletonList((NeedReplyMessage)rmsg).iterator(), spec, completion);
    }
}
