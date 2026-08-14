package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.job.Job;
import org.zstack.core.job.JobContext;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.*;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateApplianceVmJob implements Job {
    protected static final CLogger logger = Utils.getLogger(CreateApplianceVmJob.class);

    @JobContext
    protected ApplianceVmSpec spec;

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private ApplianceVmFactory apvmFactory;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ApplianceVmFacade apvf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public void run(final ReturnValueCompletion<Object> complete) {
        // if syncCreate is set, the name is the unique id for the vm
        if (spec.isSyncCreate()) {
            ApplianceVmVO vo = Q.New(ApplianceVmVO.class)
                    .eq(ApplianceVmVO_.name, spec.getName())
                    .eq(ApplianceVmVO_.state, VmInstanceState.Running)
                    .find();
            if (vo != null) {
                complete.success(ApplianceVmInventory.valueOf(vo));
                return;
            }
        }

        FlowChain chain = new SimpleFlowChain();
        chain.setName("create-applianceVm");
        chain.then(new Flow() {
            String __name__ = "persist-applianceVm-to-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ApplianceVmVO avo = new ApplianceVmVO();
                avo.setName(spec.getName());
                if (spec.getUuid() != null) {
                    avo.setUuid(spec.getUuid());
                } else {
                    avo.setUuid(Platform.getUuid());
                }
                avo.setZoneUuid(spec.getRequiredZoneUuid());
                avo.setClusterUuid(spec.getRequiredClusterUuid());
                avo.setManagementNetworkUuid(spec.getManagementNic().getL3NetworkUuid());
                String defaultRouteL3NetworkUuid = spec.getDefaultRouteL3Network() != null ? spec.getDefaultRouteL3Network().getUuid() : spec.getManagementNic().getL3NetworkUuid();
                avo.setDefaultRouteL3NetworkUuid(defaultRouteL3NetworkUuid);
                avo.setDefaultL3NetworkUuid(spec.getDefaultL3Network().getUuid());

                String zoneUuid = Q.New(L3NetworkVO.class)
                        .select(L3NetworkVO_.zoneUuid)
                        .eq(L3NetworkVO_.uuid, defaultRouteL3NetworkUuid)
                        .findValue();
                avo.setZoneUuid(zoneUuid);

                avo.setDescription(spec.getDescription());
                avo.setImageUuid(spec.getTemplate().getUuid());
                avo.setInstanceOfferingUuid(spec.getInstanceOffering().getUuid());
                avo.setState(VmInstanceState.Created);
                avo.setType(ApplianceVmConstant.APPLIANCE_VM_TYPE);
                avo.setInternalId(dbf.generateSequenceNumber(VmInstanceSequenceNumberVO.class));
                avo.setApplianceVmType(spec.getApplianceVmType().toString());
                avo.setAgentPort(spec.getAgentPort());

                ImageVO imageVO = Q.New(ImageVO.class).eq(ImageVO_.uuid, spec.getTemplate().getUuid()).find();
                avo.setPlatform(imageVO.getPlatform().toString());
                avo.setGuestOsType(imageVO.getGuestOsType());
                avo.setArchitecture(imageVO.getArchitecture());

                InstanceOfferingVO iovo = dbf.findByUuid(spec.getInstanceOffering().getUuid(), InstanceOfferingVO.class);
                avo.setCpuNum(iovo.getCpuNum());
                avo.setCpuSpeed(iovo.getCpuSpeed());
                avo.setMemorySize(iovo.getMemorySize());
                avo.setAllocatorStrategy(iovo.getAllocatorStrategy());
                if (spec.getHaSpec() != null) {
                    avo.setHaStatus(ApplianceVmHaStatus.Backup);
                } else {
                    avo.setHaStatus(ApplianceVmHaStatus.NoHa);
                }

                ApplianceVmSubTypeFactory factory = apvmFactory.getApplianceVmSubTypeFactory(avo.getApplianceVmType());

                ApplianceVmVO finalAvo1 = avo;
                avo = new SQLBatchWithReturn<ApplianceVmVO>() {
                    @Override
                    protected ApplianceVmVO scripts() {
                        finalAvo1.setAccountUuid(spec.getAccountUuid());
                        ApplianceVmVO vo = factory.persistApplianceVm(spec, finalAvo1);

                        if (ApplianceVmGlobalConfig.APPLIANCENUMA.value(Boolean.class)) {
                            ResourceConfig rc = rcf.getResourceConfig(VmGlobalConfig.NUMA.getIdentity());
                            rc.updateValue(finalAvo1.getUuid(), Boolean.TRUE.toString());
                        }

                        return reload(vo);
                    }
                }.execute();

                data.put(ApplianceVmVO.class.getSimpleName(), avo);

                tagMgr.copySystemTag(iovo.getUuid(), InstanceOfferingVO.class.getSimpleName(), avo.getUuid(), VmInstanceVO.class.getSimpleName(), false);
                if (spec.getInherentSystemTags() != null && !spec.getInherentSystemTags().isEmpty()) {
                    tagMgr.createInherentSystemTags(spec.getInherentSystemTags(), avo.getUuid(), VmInstanceVO.class.getSimpleName());
                }
                if (spec.getNonInherentSystemTags() != null && !spec.getNonInherentSystemTags().isEmpty()) {
                    tagMgr.createNonInherentSystemTags(spec.getNonInherentSystemTags(), avo.getUuid(), VmInstanceVO.class.getSimpleName());
                }

                if (imageVO.getVirtio()) {
                    tagMgr.createNonInherentSystemTag(avo.getUuid(), VmSystemTags.VIRTIO.getTagFormat(), VmInstanceVO.class.getSimpleName());
                }

                /* if there is ha configure, appliance vm will use individual affinityGroup */
                if (spec.getHaSpec() == null) {
                    apvf.attachApplianceVmToAffinityGroup(avo.getUuid(), null);
                } else {
                    if (spec.getHaSpec().getHaUuid() != null) {
                        apvf.attachApplianceVmToHaGroup(avo.getUuid(), spec.getHaSpec().getHaUuid());
                    }

                    if (spec.getHaSpec().getAffinityGroupUuid() != null) {
                        apvf.attachApplianceVmToAffinityGroup(avo.getUuid(), spec.getHaSpec().getAffinityGroupUuid());
                    }
                }

                final ApplianceVmVO finalVO = avo;
                pluginRgty.getExtensionList(ApplianceVmInstanceCreateExtensionPoint.class).forEach(
                        extension -> extension.afterPersistApplianceVmInstanceVO(finalVO));

                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                ApplianceVmVO avo = (ApplianceVmVO) data.get(ApplianceVmVO.class.getSimpleName());
                if (avo != null) {
                    if (ApplianceVmGlobalConfig.APPLIANCENUMA.value(Boolean.class)) {
                        ResourceConfig rc = rcf.getResourceConfig(VmGlobalConfig.NUMA.getIdentity());
                        rc.deleteValue(avo.getUuid());
                    }
                    ApplianceVmSubTypeFactory factory = apvmFactory.getApplianceVmSubTypeFactory(avo.getApplianceVmType());
                    factory.removeApplianceVm(spec, avo);
                }
                trigger.rollback();
            }
        }).then(new Flow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                ApplianceVmVO avo = (ApplianceVmVO) chain.getData().get(ApplianceVmVO.class.getSimpleName());
                final ApplianceVmInventory inv = ApplianceVmInventory.valueOf(avo);
                StartNewCreatedApplianceVmMsg msg = new StartNewCreatedApplianceVmMsg();
                List<VmNicSpec> nicSpecs = new ArrayList<>();
                L3NetworkInventory mnL3 = L3NetworkInventory.valueOf(dbf.findByUuid(spec.getManagementNic().getL3NetworkUuid(), L3NetworkVO.class));
                nicSpecs.add(new VmNicSpec(mnL3));
                for (ApplianceVmNicSpec aSpec : spec.getAdditionalNics()) {
                    L3NetworkInventory l3 = L3NetworkInventory.valueOf(dbf.findByUuid(aSpec.getL3NetworkUuid(), L3NetworkVO.class));
                    nicSpecs.add(new VmNicSpec(l3));
                }
                msg.setL3NetworkUuids(nicSpecs);
                msg.setVmInstanceInventory(inv);
                msg.setApplianceVmSpec(spec);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, inv.getUuid());
                bus.send(msg, new CloudBusCallBack(complete) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        }).done(new FlowDoneHandler(complete) {
            @Override
            public void handle(Map chainData) {
                ApplianceVmVO avo = (ApplianceVmVO) chainData.get(ApplianceVmVO.class.getSimpleName());
                L3NetworkConstant.VRouterData data = new L3NetworkConstant.VRouterData();
                data.vrouterUuid = avo.getUuid();
                evtf.fire(L3NetworkConstant.VROUTER_CREATE_EVENT_PATH, data);

                logger.debug(String.format("successfully created appliance vm[uuid:%s, name: %s, appliance vm type: %s]", avo.getUuid(), avo.getName(), avo.getApplianceVmType()));
                ApplianceVmVO apvo = dbf.findByUuid(avo.getUuid(), ApplianceVmVO.class);
                ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(apvo);
                complete.success(ainv);
            }
        }).error(new FlowErrorHandler(complete) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                logger.warn(String.format("failed to create appliance vm[name: %s, appliance vm type: %s], %s", spec.getName(), spec.getApplianceVmType(), errCode.getDetails()));
                complete.fail(errCode);
            }
        }).start();
    }

    public ApplianceVmSpec getSpec() {
        return spec;
    }

    public void setSpec(ApplianceVmSpec spec) {
        this.spec = spec;
    }
}
