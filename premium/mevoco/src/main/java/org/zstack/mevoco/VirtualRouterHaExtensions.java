package org.zstack.mevoco;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmStatus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.ha.fencers.HaStrategyHelper;
import org.zstack.ha.header.SelfFencerStrategy;
import org.zstack.ha.vmboot.HaStartVmJudgers;
import org.zstack.ha.vmboot.VmHaStartMessageSender;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.network.service.virtualrouter.AfterAcquireVirtualRouterExtensionPoint;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.i18n;

public class VirtualRouterHaExtensions implements
        AfterAcquireVirtualRouterExtensionPoint
{
    private static final CLogger logger = Utils.getLogger(VirtualRouterHaExtensions.class);

    @Autowired
    private DatabaseFacade databaseFacade;

    @Override
    public void afterAcquireVirtualRouter(VirtualRouterVmInventory vr, Completion completion) {
        VirtualRouterVmVO vo = databaseFacade.findByUuid(vr.getUuid(), VirtualRouterVmVO.class);

        if (vo.getState().equals(VmInstanceState.Running)) {
            completion.success();
            return;
        }

        if (ApplianceVmStatus.Connected.equals(vo.getStatus())) {
            logger.debug(String.format("virtual router[uuid: %s] is in status[%s], skip HA", vr.getUuid(), vo.getStatus()));
            completion.success();
            return;
        }

        if (HaStrategyHelper.getVmHaFencerStrategy(vr.getUuid()) == SelfFencerStrategy.Permissive && vo.getState().equals(VmInstanceState.Unknown)) {
            logger.debug(String.format("Self fencer strategy if Permissive, skip HA for virtual router[uuid:%s, state:%s]", vr.getUuid(), VmInstanceState.Unknown.toString()));
            completion.success();
            return;
        }

        VmHaStartMessageSender sender = new VmHaStartMessageSender()
                .withVmInstance(vr.getUuid())
                .withJudgerClass(HaStartVmJudgers.GeneralJudger.class.getName())
                .withReasonForHA(i18n("VirtualRouterVm is not running"));
        sender.send(new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });

    }
}
