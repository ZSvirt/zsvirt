package org.zstack.compute.vmscheduling;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.affinityGroup.AffinityGroupBase;
import org.zstack.compute.affinityGroup.AffinityGroupFilterFlow;
import org.zstack.compute.affinityGroup.AffinityGroupManager;
import org.zstack.compute.vm.VmInstanceManager;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.affinitygroup.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vmscheduling.*;
import org.zstack.identity.AccountManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

/**
 * @Author: DaoDao
 * @Date: 2022/11/30
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmSchedulingRuleBase {
    private static CLogger logger = Utils.getLogger(AffinityGroupBase.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    protected VmInstanceManager vmMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected AffinityGroupFilterFlow affinityfilterFlow;
    @Autowired
    protected AffinityGroupManager agMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AccountManager accr;

    protected VmSchedulingRuleVO self;

    public VmSchedulingRuleBase(VmSchedulingRuleVO self) {
        this.self = self;
    }

    private String getSyncId() {
        return String.format("vm-scheduling-rule-%s", self.getUuid());
    }


    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    protected void handleLocalMessage(Message msg){
        bus.dealWithUnknownMessage(msg);
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeVmSchedulingRuleStateMsg) {
            handle((APIChangeVmSchedulingRuleStateMsg) msg);
        } else if (msg instanceof APIUpdateVmSchedulingRuleMsg) {
            handle((APIUpdateVmSchedulingRuleMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateVmSchedulingRuleMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateVmSchedulingRuleEvent evt = new APIUpdateVmSchedulingRuleEvent(msg.getId());

                boolean update = false;
                if (msg.getName() != null) {
                    self.setName(msg.getName());
                    update = true;
                }
                if (msg.getDescription() != null) {
                    self.setDescription(msg.getDescription());
                    update = true;
                }
                if (msg.getMode() != null) {
                    self.setMode(VMSchedulingRuleMode.valueOf(msg.getMode()));
                    AffinityGroupPolicy policy = VmSchedulingRulePolicy.getAffinityGroupPolicy(self.getRule().toString(),
                            self.getMode().toString());
                    self.setPolicy(policy);
                    update = true;
                }

                if (update) {
                    self = dbf.updateAndRefresh(self);
                }

                evt.setInventory(VmSchedulingRuleInventory.valueOf(self));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APIChangeVmSchedulingRuleStateMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                APIChangeVmSchedulingRuleStateEvent evt = new APIChangeVmSchedulingRuleStateEvent(msg.getId());
                final AffinityGroupState nextState = self.getState().nextState(AffinityGroupStateEvent.valueOf(msg.getState()));
                if (nextState == self.getState()) {
                    evt.setInventory(VmSchedulingRuleInventory.valueOf(self));
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                self.setState(nextState);
                self = dbf.updateAndRefresh(self);
                evt.setInventory(VmSchedulingRuleInventory.valueOf(self));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public String getName() {
                return getSyncId();
            }
        });
    }

}
