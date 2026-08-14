package org.zstack.drs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.drs.api.*;
import org.zstack.drs.data.DRSAutomationLevel;
import org.zstack.drs.data.DRSState;
import org.zstack.drs.data.DRSVmMigrationStatus;
import org.zstack.drs.entity.*;
import org.zstack.drs.header.DRSErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;

import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.err;

/**
 * Created by lining on 2019/12/12.
 */
@InterceptorForService("drs")
public class DRSInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateClusterDRSMsg) {
            validate((APICreateClusterDRSMsg) msg);
        } else if (msg instanceof APIApplyDRSAdviceMsg) {
            validate((APIApplyDRSAdviceMsg) msg);
        } else if (msg instanceof APIUpdateClusterDRSMsg) {
            validate((APIUpdateClusterDRSMsg) msg);
        } else if (msg instanceof APIExecuteDRSSchedulingMsg) {
            validate((APIExecuteDRSSchedulingMsg) msg);
        }

        return msg;
    }

    private void validate(APICreateClusterDRSMsg msg) {
        boolean exists = Q.New(ClusterDRSVO.class)
                .eq(ClusterDRSVO_.clusterUuid, msg.getClusterUuid())
                .isExists();
        if (exists) {
            throw new ApiMessageInterceptionException(
                    err(DRSErrors.DRS_ALREADY_EXISTS, "The cluster[%s] has created DRS",  msg.getClusterUuid()));
        }

        List<Threshold> thresholds = msg.getThresholds();
        if (thresholds == null || thresholds.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("thresholds can not be empty"));
        }
        validate(thresholds);
    }

    private void validate(List<Threshold> thresholds) {
        for (Threshold threshold : thresholds) {
            if (!(DRSConstants.CPU_USED_PERCENT_THRESHOLD.equals(threshold.getThresholdName()) ||
                    DRSConstants.MEMORY_USED_PERCENT_THRESHOLD.equals(threshold.getThresholdName()))) {
                throw new ApiMessageInterceptionException(argerr("illegal thresholdName[%s]", threshold.getThresholdName()));
            }

            if (!">=".equals(threshold.getOperator())) {
                throw new ApiMessageInterceptionException(argerr("illegal threshold operator[%s]", threshold.getOperator()));
            }

            if (threshold.getThresholdValue() == null || threshold.getThresholdValue().isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("thresholdValue can not be empty"));
            }

            int value = Integer.parseInt(threshold.getThresholdValue());
            if (value <= 0 || value > 100) {
                throw new ApiMessageInterceptionException(argerr("illegal thresholdValue, valid range: (0, 100]"));
            }
        }
    }

    private void validate(APIUpdateClusterDRSMsg msg) {
        List<Threshold> thresholds = msg.getThresholds();
        if (thresholds != null && !thresholds.isEmpty()) {
            validate(thresholds);
        }
    }

    private void validate(APIExecuteDRSSchedulingMsg msg) {
        DRSState state = Q.New(ClusterDRSVO.class)
                .select(ClusterDRSVO_.state)
                .eq(ClusterDRSVO_.uuid, msg.getUuid())
                .findValue();
        if (state != DRSState.Enabled) {
            throw new ApiMessageInterceptionException(
                    err(DRSErrors.DRS_DISABLED, "The DRS[%s] state is %s", msg.getUuid(), state.toString()));
        }
    }

    private void validate(APIApplyDRSAdviceMsg msg) {
        DRSAdviceVO adviceVO = Q.New(DRSAdviceVO.class)
                .eq(DRSAdviceVO_.uuid, msg.getAdviceUuid())
                .find();
        msg.setDrsUuid(adviceVO.getDrsUuid());

        DRSAutomationLevel level = Q.New(ClusterDRSVO.class)
                .select(ClusterDRSVO_.automationLevel)
                .eq(ClusterDRSVO_.uuid, adviceVO.getDrsUuid())
                .findValue();
        if (level != DRSAutomationLevel.Manual) {
            throw new ApiMessageInterceptionException(argerr("The DRS[%s] automation level is not manual", adviceVO.getDrsUuid()));
        }

        String adviceGroupUuid = Q.New(DRSAdviceVO.class)
                .select(DRSAdviceVO_.adviceGroupUuid)
                .eq(DRSAdviceVO_.uuid, msg.getAdviceUuid())
                .findValue();
        String newestAdviceGroupUuid = Q.New(DRSAdviceVO.class)
                .select(DRSAdviceVO_.adviceGroupUuid)
                .eq(DRSAdviceVO_.drsUuid, adviceVO.getDrsUuid())
                .orderBy(DRSAdviceVO_.createDate, SimpleQuery.Od.DESC)
                .limit(1)
                .findValue();
        if (!adviceGroupUuid.equals(newestAdviceGroupUuid)) {
            throw new ApiMessageInterceptionException(argerr("advice[uuid:%s] has expired as the scheduling policy has been modified", msg.getAdviceUuid()));
        }

        boolean flag = Q.New(DRSVmMigrationActivityVO.class)
                .eq(DRSVmMigrationActivityVO_.drsUuid, adviceVO.getDrsUuid())
                .eq(DRSVmMigrationActivityVO_.adviceUuid, msg.getAdviceUuid())
                .eq(DRSVmMigrationActivityVO_.status, DRSVmMigrationStatus.Successful)
                .isExists();
        if (flag) {
            throw new ApiMessageInterceptionException(argerr("Successfully executed, no repeated executions allowed"));
        }

        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, adviceVO.getVmUuid())
                .find();
        if (vmInstanceVO == null) {
            throw new ApiMessageInterceptionException(argerr("The vm[%s] has been deleted", adviceVO.getVmUuid()));
        }
        if (vmInstanceVO.getState() != VmInstanceState.Running) {
            throw new ApiMessageInterceptionException(argerr("The vm[%s] state is not running", adviceVO.getVmUuid()));
        }
        if (!vmInstanceVO.getHostUuid().equals(adviceVO.getVmSourceHostUuid())) {
            throw new ApiMessageInterceptionException(argerr("The vm[%s] is no longer on the source host[%s]", adviceVO.getVmUuid(), adviceVO.getVmSourceHostUuid()));
        }
    }

}
