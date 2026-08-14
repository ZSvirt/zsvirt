package org.zstack.pciDevice.gpu;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.Component;
import org.zstack.header.host.AfterSyncPciDeviceExtensionPoint;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.pciDevice.*;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/8 10:08
 */
public class GpuDeviceFactory implements PciDeviceTypeFactory, Component {
    private static CLogger logger = Utils.getLogger(GpuDeviceFactory.class);

    @Autowired
    protected DatabaseFacade dbf;

    @Autowired
    protected EventFacade evf;

    @Override
    public List<PciDeviceType> getTypes() {
        List<PciDeviceType> types = new ArrayList<>();
        types.add(PciDeviceType.GPU_Video_Controller);
        types.add(PciDeviceType.GPU_3D_Controller);
        return types;
    }

    @Override
    public void createPciDevices(List<PciDeviceVO> vos) {
        List<GpuDeviceVO> gpuDeviceVOS = new ArrayList<>();
        for(PciDeviceVO vo : vos) {
            GpuDeviceVO gpu = new GpuDeviceVO(vo);
            if (vo.getAddonInfo() != null && !vo.getAddonInfo().isEmpty()) {
                gpu.setMemory(SizeUtils.sizeStringToBytes(vo.getAddonInfo().get(GpuConstant.GPU_MEMORY_NAME).trim()));
                gpu.setPower(Double.valueOf(vo.getAddonInfo().get(GpuConstant.GPU_POWER_NAME)
                        .replaceAll("[a-zA-Z]", "").trim()).longValue());
                gpu.setSerialNumber(vo.getAddonInfo().get(GpuConstant.GPU_SERIALNUMBER_NAME));
                gpu.setDriverLoaded(Boolean.parseBoolean(vo.getAddonInfo().get(GpuConstant.GPU_IS_DRIVER_LOADED_NAME)));
            }
            gpuDeviceVOS.add(gpu);
        }
        dbf.persistCollection(gpuDeviceVOS);
    }

    @Override
    public void updatePciDevices(List<PciDeviceVO> vos) {
        List<GpuDeviceVO> gpuVOs = new ArrayList<>();
        for (PciDeviceVO vo : vos) {
            GpuDeviceVO gpu = dbf.findByUuid(vo.getUuid(), GpuDeviceVO.class);
            if (gpu == null) {
                continue;
            }
            gpu.setDevice(vo.getDevice());
            if (vo.getAddonInfo() != null && !vo.getAddonInfo().isEmpty()) {
                gpu.setMemory(SizeUtils.sizeStringToBytes(vo.getAddonInfo().get(GpuConstant.GPU_MEMORY_NAME).trim()));
                gpu.setPower(Double.valueOf(vo.getAddonInfo().get(GpuConstant.GPU_POWER_NAME)
                        .replaceAll("[a-zA-Z]", "").trim()).longValue());
                gpu.setSerialNumber(vo.getAddonInfo().get(GpuConstant.GPU_SERIALNUMBER_NAME));
                gpu.setDriverLoaded(Boolean.parseBoolean(vo.getAddonInfo().get(GpuConstant.GPU_IS_DRIVER_LOADED_NAME)));
            }
            gpuVOs.add(gpu);
        }

        if (!gpuVOs.isEmpty()) {
            dbf.updateCollection(gpuVOs);
        }

    }

    @Override
    public void removePciDevices(List<PciDeviceTO> tos) {
       if (CollectionUtils.isEmpty(tos)) {
           return;
       }

       for (PciDeviceTO to : tos) {
           if (StringUtils.isEmpty(to.getUuid())) {
               continue;
           }
           SQL.New(PciDeviceVO.class).eq(PciDeviceVO_.uuid, to.getUuid()).hardDelete();
           SQL.New(MdevDeviceVO.class).eq(MdevDeviceVO_.parentUuid, to.getUuid()).hardDelete();
           HostCanonicalEvents.HostPhysicalGpuRemoveTriggeredData data = new HostCanonicalEvents.HostPhysicalGpuRemoveTriggeredData();
           data.setHostUuid(to.getHostUuid());
           data.setPcideviceAddress(to.getPciDeviceAddress());
           evf.fire(HostCanonicalEvents.HOST_PHYSICAL_GPU_REMOVE_TRIGGERED, data);
       }
    }

    @Override
    public boolean start() {
        evf.on(HostCanonicalEvents.HOST_PHYSICAL_GPU_STATUS_ABNORMAL, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostPhysicalGpuStatusAbnormalData d = (HostCanonicalEvents.HostPhysicalGpuStatusAbnormalData) data;
                GpuDeviceVO gpu = Q.New(GpuDeviceVO.class)
                        .eq(GpuDeviceVO_.hostUuid, d.getHostUuid())
                        .eq(GpuDeviceVO_.pciDeviceAddress, d.getPcideviceAddress())
                        .find();

                if (gpu == null) {
                    return;
                }

                List<MdevDeviceVO> mdevDeviceVOS = Q.New(MdevDeviceVO.class)
                        .eq(MdevDeviceVO_.parentUuid, gpu.getUuid())
                        .eq(MdevDeviceVO_.hostUuid, d.getHostUuid())
                        .list();

                for (MdevDeviceVO mdev : mdevDeviceVOS) {
                    HostCanonicalEvents.HostPhysicalVGpuStatusAbnormalData cdata = new HostCanonicalEvents.HostPhysicalVGpuStatusAbnormalData();
                    cdata.setHostUuid(mdev.getHostUuid());
                    cdata.setUuid(mdev.getUuid());
                    cdata.setStatus(d.getStatus());
                    cdata.setName(mdev.getName());
                    evf.fire(HostCanonicalEvents.HOST_PHYSICAL_VGPU_STATUS_ABNORMAL, cdata);
                }

                List<PciDeviceVO> pciDeviceVOS = Q.New(PciDeviceVO.class)
                        .eq(PciDeviceVO_.hostUuid, d.getHostUuid())
                        .eq(PciDeviceVO_.parentUuid, gpu.getUuid())
                        .list();

                for (PciDeviceVO pci : pciDeviceVOS) {
                    HostCanonicalEvents.HostPhysicalVGpuStatusAbnormalData cdata = new HostCanonicalEvents.HostPhysicalVGpuStatusAbnormalData();
                    cdata.setHostUuid(pci.getHostUuid());
                    cdata.setUuid(pci.getUuid());
                    cdata.setStatus(d.getStatus());
                    cdata.setName(pci.getName());
                    evf.fire(HostCanonicalEvents.HOST_PHYSICAL_VGPU_STATUS_ABNORMAL, cdata);
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
