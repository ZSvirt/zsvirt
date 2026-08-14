package org.zstack.ha;

import org.zstack.core.Platform;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostStatus;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeType;
import org.zstack.kvm.KVMConstant;
import org.zstack.pciDevice.PciDeviceChooser;
import org.zstack.pciDevice.PciDeviceVO;
import org.zstack.pciDevice.PciDeviceVO_;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceChooser;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO_;
import org.zstack.usbDevice.UsbDeviceVO;
import org.zstack.usbDevice.UsbDeviceVO_;

import java.util.List;

import static org.zstack.core.Platform.operr;

@InterceptorForService("ha")
public class HaInterceptor implements ApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APISetVmInstanceHaLevelMsg) {
            validate((APISetVmInstanceHaLevelMsg) msg);
        } else if (msg instanceof APIDeleteVmInstanceHaLevelMsg) {
            validate((APIDeleteVmInstanceHaLevelMsg) msg);
        } else if (msg instanceof APIUpdateHaStrategyConditionMsg) {
            validate((APIUpdateHaStrategyConditionMsg) msg);
        }

        return msg;
    }

    private void validate(APIUpdateHaStrategyConditionMsg msg) {
        List<String> hostUuids = SQL.New("select distinct host.uuid from VmInstanceVO vm, HostVO host where vm.hostUuid = host.uuid " +
                "and host.status != :hstatus and host.hypervisorType = :hypervisorType and vm.state = :vstate", String.class)
                .param("hstatus", HostStatus.Connected)
                .param("vstate", VmInstanceState.Unknown)
                .param("hypervisorType", KVMConstant.KVM_HYPERVISOR_TYPE)
                .limit(10)
                .list();
        if (!hostUuids.isEmpty()) {
            throw new GlobalConfigException(String.format("host[uuid:%s] is not %s, but still have vm on it," +
                    " please resolve hosts' problems before update fencer strategy", hostUuids, HostStatus.Connected));
        }
    }

    private void validate(APIDeleteVmInstanceHaLevelMsg msg) {
        VmHaLevel originLevel = Q.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, msg.getUuid())
                .select(VmHaVO_.haLevel)
                .findValue();
        if (!VmHaLevel.FaultTolerance.equals(originLevel)) {
            return;
        }

        VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getUuid()).find();
        checkVmForCancelFtLevel(vm);
    }

    private void validate(APISetVmInstanceHaLevelMsg msg) {
        VmHaLevel originLevel = Q.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, msg.getUuid())
                .select(VmHaVO_.haLevel)
                .findValue();
        VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getUuid()).find();

        if (VmHaLevel.FaultTolerance != originLevel && VmHaLevel.FaultTolerance.toString().equals(msg.getLevel())) {
            checkVmForFaultToleranceLevel(msg, vm);
        }

        if (VmHaLevel.FaultTolerance == originLevel && !VmHaLevel.FaultTolerance.toString().equals(msg.getLevel())) {
            checkVmForCancelFtLevel(vm);
        }
    }

    private void checkVmForCancelFtLevel(VmInstanceVO vm) {
        if (VmInstanceState.Stopped != vm.getState()) {
            throw new OperationFailureException(operr("can not set FT on vm[uuid:%s] because it is not stopped", vm.getUuid()));
        }
    }

    private void checkVmForFaultToleranceLevel(APISetVmInstanceHaLevelMsg msg, VmInstanceVO vm) {
        if (VmInstanceState.Stopped != vm.getState()) {
            throw new OperationFailureException(operr("can not set FT on vm[uuid:%s] because it is not stopped", vm.getUuid()));
        }

        if (vm.getAllVolumes().stream().anyMatch(vol -> VolumeType.Data.equals(vol.getType()))) {
            throw new OperationFailureException(operr("can not set FT on vm[uuid:%s] because some data volume is still attached", vm.getUuid()));
        }

        List<PciDeviceVO> pciDeviceVOS = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.vmInstanceUuid, msg.getUuid())
                .eq(PciDeviceVO_.chooser, PciDeviceChooser.Device)
                .list();
        if (pciDeviceVOS != null && !pciDeviceVOS.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("can not set FT on vm[uuid:%s] since pci device attached",
                    msg.getUuid()));
        }

        boolean usbAttached = Q.New(UsbDeviceVO.class).eq(UsbDeviceVO_.vmInstanceUuid, msg.getUuid()).isExists();
        if (usbAttached) {
            throw new ApiMessageInterceptionException(Platform.operr(
                    "can not set FT on vm[uuid:%s] because there are usb devices attached by passthrough",
                    msg.getUuid()
            ));
        }

        List<MdevDeviceVO> mdevDeviceVOS = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.vmInstanceUuid, msg.getUuid())
                .eq(MdevDeviceVO_.chooser, MdevDeviceChooser.Device)
                .list();
        if (mdevDeviceVOS != null && !mdevDeviceVOS.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("can not set FT on vmm[uuid:%s] since mdev device attached",
                    msg.getUuid()));
        }
    }
}
