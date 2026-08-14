package org.zstack.pciDevice;

import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by GuoYi on 2019-05-09.
 */
public class PciDeviceTOExampleMaker {
    private static Map<String, String> m60_2a;
    private static Map<String, String> m60_2b;
    private static Map<String, String> t4_4q;

    public static PciDeviceTO PCI_BRIDGE() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("PCI Express x16 Controller");
        t.setDescription("Intel Corporation, PCI Express x16 Controller, PCI bridge");
        t.setVendorId("8086");
        t.setDeviceId("0c01");
        t.setPciDeviceAddress("03:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/1");
        t.setType(PciDeviceType.PCI_Bridge.toString());
        t.setVirtStatus(PciDeviceVirtStatus.UNVIRTUALIZABLE.toString());
        return t;
    }

    public static PciDeviceTO HOST_BRIDGE() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("Intel Sky Lake-E DMI3 Registers");
        t.setDescription("Host bridge: Intel Sky Lake-E DMI3 Registers");
        t.setVendorId("8086");
        t.setDeviceId("2020");
        t.setPciDeviceAddress("00:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/1");
        t.setType(PciDeviceType.Host_Bridge.toString());
        t.setVirtStatus(PciDeviceVirtStatus.UNVIRTUALIZABLE.toString());
        return t;
    }

    public static PciDeviceTO INTEL_INTEGRATED_GPU() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("INTEL_INTEGRATED_GPU");
        t.setDescription("Intel Corporation, Integrated Graphics Controller, VGA compatible controller");
        t.setVendorId("8086");
        t.setDeviceId("0412");
        t.setSubvendorId("1458");
        t.setSubdeviceId("d000");
        t.setPciDeviceAddress("04:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/1");
        t.setType(PciDeviceType.Generic.toString());
        t.setVirtStatus(PciDeviceVirtStatus.UNVIRTUALIZABLE.toString());
        return t;
    }

    public static PciDeviceTO INTEL_INTEGRATED_AUDIO() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("INTEL_INTEGRATED_AUDIO");
        t.setDescription("Intel Corporation, Core Processor HD Audio Controller, Audio device");
        t.setVendorId("8086");
        t.setDeviceId("0c0c");
        t.setSubvendorId("8086");
        t.setSubdeviceId("2010");
        t.setPciDeviceAddress("05:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/1");
        t.setType(PciDeviceType.Generic.toString());
        t.setVirtStatus(PciDeviceVirtStatus.UNVIRTUALIZABLE.toString());
        return t;
    }

    public static PciDeviceTO GT710B_VIDEO() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("Geforce_GT_710B");
        t.setDescription("NVIDIA Corporation, Geforce GT 710B, VGA compatible controller");
        t.setVendorId("10de");
        t.setDeviceId("128b");
        t.setSubvendorId("10de");
        t.setSubdeviceId("118b");
        t.setPciDeviceAddress("06:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/2");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.UNVIRTUALIZABLE.toString());
        Map<String, String> map = new HashMap<>();
        map.put("memory", "24 G");
        map.put("power", "20.00 W");
        map.put("serialNumber", "1322620070289");
        t.setAddonInfo(map);
        return t;
    }

    public static PciDeviceTO GT710B_VIDEO_NO_SUB_ID() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("Geforce_GT_710B");
        t.setDescription("NVIDIA Corporation, Geforce GT 710B, VGA compatible controller");
        t.setVendorId("10de");
        t.setDeviceId("128b");
        t.setPciDeviceAddress("06:00.1");
        t.setIommuGroup("/sys/kernel/iommu_groups/_2");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.UNVIRTUALIZABLE.toString());
        return t;
    }

    public static PciDeviceTO GT710B_AUDIO() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("GK208_HDMI/DP_Audio_Controller");
        t.setDescription("NVIDIA Corporation, GK208 HDMI/DP Audio Controller, Audio device");
        t.setVendorId("10de");
        t.setDeviceId("0e0f");
        t.setSubvendorId("10de");
        t.setSubdeviceId("118b");
        t.setPciDeviceAddress("06:00.1");
        t.setIommuGroup("/sys/kernel/iommu_groups/2");
        t.setType(PciDeviceType.GPU_Audio_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.UNVIRTUALIZABLE.toString());
        return t;
    }

    public static PciDeviceTO M60_VIRTUALIZABLE() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("Tesla_M60");
        t.setDescription("NVIDIA Corporation, GM204GL [Tesla M60], VGA compatible controller");
        t.setVendorId("10de");
        t.setDeviceId("13f2");
        t.setSubvendorId("10de");
        t.setSubdeviceId("13f2");
        t.setPciDeviceAddress("07:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/3");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE.toString());
        t.setMdevSpecifications(Arrays.asList(m60_2a, m60_2b));
        return t;
    }

    public static PciDeviceTO M60_2_VIRTUALIZABLE() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("Tesla_M60");
        t.setDescription("NVIDIA Corporation, GM204GL [Tesla M60], VGA compatible controller");
        t.setVendorId("10de");
        t.setDeviceId("13f2");
        t.setSubvendorId("10de");
        t.setSubdeviceId("13f2");
        t.setPciDeviceAddress("22:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/22");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE.toString());
        t.setMdevSpecifications(Arrays.asList(m60_2a, m60_2b));
        return t;
    }

    public static PciDeviceTO M60_3_VIRTUALIZABLE() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("Tesla_M60");
        t.setDescription("NVIDIA Corporation, GM204GL [Tesla M60], VGA compatible controller");
        t.setVendorId("10de");
        t.setDeviceId("13f2");
        t.setSubvendorId("10de");
        t.setSubdeviceId("13f2");
        t.setPciDeviceAddress("23:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/23");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE.toString());
        t.setMdevSpecifications(Arrays.asList(m60_2a, m60_2b));
        return t;
    }

    public static PciDeviceTO M60_4_VIRTUALIZABLE() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("Tesla_M60");
        t.setDescription("NVIDIA Corporation, GM204GL [Tesla M60], VGA compatible controller");
        t.setVendorId("10de");
        t.setDeviceId("13f2");
        t.setSubvendorId("10de");
        t.setSubdeviceId("13f2");
        t.setPciDeviceAddress("24:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/24");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE.toString());
        t.setMdevSpecifications(Arrays.asList(m60_2a, m60_2b));
        return t;
    }

    public static PciDeviceTO M60_5_VIRTUALIZABLE() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("Tesla_M60");
        t.setDescription("NVIDIA Corporation, GM204GL [Tesla M60], VGA compatible controller");
        t.setVendorId("10de");
        t.setDeviceId("13f2");
        t.setSubvendorId("10de");
        t.setSubdeviceId("13f2");
        t.setPciDeviceAddress("25:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/25");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE.toString());
        t.setMdevSpecifications(Arrays.asList(m60_2a, m60_2b));
        return t;
    }

    public static PciDeviceTO M60_VIRTUALIZED() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("Tesla_M60");
        t.setDescription("NVIDIA Corporation, GM204GL [Tesla M60], VGA compatible controller");
        t.setVendorId("10de");
        t.setDeviceId("13f2");
        t.setSubvendorId("10de");
        t.setSubdeviceId("13f2");
        t.setPciDeviceAddress("07:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/3");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZED.toString());
        t.setMdevSpecifications(Arrays.asList(m60_2a, m60_2b));
        return t;
    }

    public static PciDeviceTO S7150_VIRTUALIZABLE() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("FirePro_S7150");
        t.setDescription("Advanced Micro Devices, Inc. [AMD/ATI], Tonga XT GL [FirePro S7150], VGA compatible controller");
        t.setVendorId("1002");
        t.setDeviceId("6929");
        t.setSubvendorId("1002");
        t.setSubdeviceId("0334");
        t.setPciDeviceAddress("08:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/4");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUALIZABLE.toString());
        t.setMaxPartNum(16);
        return t;
    }

    public static PciDeviceTO S7150_VIRTUALIZED() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("FirePro_S7150");
        t.setDescription("Advanced Micro Devices, Inc. [AMD/ATI], Tonga XT GL [FirePro S7150], VGA compatible controller");
        t.setVendorId("1002");
        t.setDeviceId("6929");
        t.setSubvendorId("1002");
        t.setSubdeviceId("0334");
        t.setPciDeviceAddress("09:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/4");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUALIZED.toString());
        t.setMaxPartNum(16);
        Map<String, String> map = new HashMap<>();
        map.put("memory", "24 G");
        map.put("power", "20 W");
        map.put("serialNumber", "1322620070289");
        t.setAddonInfo(map);
        return t;
    }

    public static PciDeviceTO S7150V_3936M_1() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("FirePro_S7150V_3936M");
        t.setDescription("Advanced Micro Devices, Inc. [AMD/ATI], Tonga XTV GL [FirePro S7150V], VGA compatible controller");
        t.setVendorId("1002");
        t.setDeviceId("692f");
        t.setPciDeviceAddress("09:02.0");
        t.setParentAddress("09:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/5");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUAL.toString());
        t.setMaxPartNum(2);
        return t;
    }

    public static PciDeviceTO S7150V_3936M_2() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("FirePro_S7150V_3936M");
        t.setDescription("Advanced Micro Devices, Inc. [AMD/ATI], Tonga XTV GL [FirePro S7150V], VGA compatible controller");
        t.setVendorId("1002");
        t.setDeviceId("692f");
        t.setPciDeviceAddress("09:02.1");
        t.setParentAddress("09:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/6");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUAL.toString());
        t.setMaxPartNum(2);
        return t;
    }

    public static PciDeviceTO S7150V_3936M_3() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("FirePro_S7150V_3936M");
        t.setDescription("Advanced Micro Devices, Inc. [AMD/ATI], Tonga XTV GL [FirePro S7150V], VGA compatible controller");
        t.setVendorId("1002");
        t.setDeviceId("692f");
        t.setPciDeviceAddress("09:02.2");
        t.setParentAddress("09:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/7");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUAL.toString());
        t.setMaxPartNum(2);
        return t;
    }

    public static PciDeviceTO S7150V_3936M_4() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("FirePro_S7150V_3936M");
        t.setDescription("Advanced Micro Devices, Inc. [AMD/ATI], Tonga XTV GL [FirePro S7150V], VGA compatible controller");
        t.setVendorId("1002");
        t.setDeviceId("692f");
        t.setPciDeviceAddress("09:02.3");
        t.setParentAddress("09:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/8");
        t.setType(PciDeviceType.GPU_Video_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUAL.toString());
        t.setMaxPartNum(2);
        return t;
    }

    public static PciDeviceTO GT750M_3D() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("Geforce_GT_750M");
        t.setDescription("NVIDIA Corporation, Geforce GT 750M, 3D controller");
        t.setVendorId("10de");
        t.setDeviceId("0fe4");
        t.setPciDeviceAddress("0a:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/9");
        t.setType(PciDeviceType.GPU_3D_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.UNVIRTUALIZABLE.toString());
        return t;
    }

    public static PciDeviceTO MOXA_DEVICE() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("CP104U");
        t.setDescription("Moxa Technologies Co Ltd, CP104U, Serial controller");
        t.setVendorId("1393");
        t.setDeviceId("1041");
        t.setSubvendorId("1393");
        t.setSubdeviceId("1041");
        t.setPciDeviceAddress("0b:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/10");
        t.setType(PciDeviceType.Moxa_Device.toString());
        t.setVirtStatus(PciDeviceVirtStatus.UNVIRTUALIZABLE.toString());
        return t;
    }

    public static PciDeviceTO PCI_CONTROLLER() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("Unassigned class");
        t.setDescription("Unassigned class Vendor Device Unknown vendor Device");
        t.setVendorId("0501");
        t.setDeviceId("1515");
        t.setSubvendorId("66cc");
        t.setSubdeviceId("1067");
        t.setPciDeviceAddress("0c:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/11");
        t.setType(PciDeviceType.Generic.toString());
        t.setVirtStatus(PciDeviceVirtStatus.UNVIRTUALIZABLE.toString());
        return t;
    }

    public static PciDeviceTO T4_VIRTUALIZABLE() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("Tesla_T4");
        t.setDescription("NVIDIA Corporation, Tesla T4, VGA compatible controller");
        t.setVendorId("10de");
        t.setDeviceId("13f3");
        t.setSubvendorId("10de");
        t.setSubdeviceId("13f3");
        t.setPciDeviceAddress("0d:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/12");
        t.setType(PciDeviceType.GPU_3D_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE.toString());
        t.setMdevSpecifications(Collections.singletonList(t4_4q));
        return t;
    }

    public static PciDeviceTO T4_VIRTUALIZED() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("Tesla_T4");
        t.setDescription("NVIDIA Corporation, Tesla T4, VGA compatible controller");
        t.setVendorId("10de");
        t.setDeviceId("13f3");
        t.setSubvendorId("10de");
        t.setSubdeviceId("13f3");
        t.setPciDeviceAddress("0d:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/12");
        t.setType(PciDeviceType.GPU_3D_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZED.toString());
        t.setMdevSpecifications(Collections.singletonList(t4_4q));
        return t;
    }

    public static PciDeviceTO INTEL_82576_VIRTUALIZABLE() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("INTEL_82576");
        t.setDescription("Ethernet controller: Intel Corporation 82576 Gigabit Network Connection");
        t.setVendorId("8086");
        t.setDeviceId("1010");
        t.setSubvendorId("8086");
        t.setSubdeviceId("1010");
        t.setPciDeviceAddress("0e:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/13");
        t.setType(PciDeviceType.Ethernet_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUALIZABLE.toString());
        t.setMaxPartNum(16);
        return t;
    }

    public static PciDeviceTO INTEL_82576_VIRTUALIZED() {
        PciDeviceTO t = INTEL_82576_VIRTUALIZABLE();
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUALIZED.toString());
        return t;
    }

    public static PciDeviceTO INTEL_82576_2_VIRTUALIZABLE() {
        PciDeviceTO t = INTEL_82576_VIRTUALIZABLE();
        t.setPciDeviceAddress("0e:00.1");
        t.setIommuGroup("/sys/kernel/iommu_groups/14");
        return t;
    }

    public static PciDeviceTO INTEL_82576_2_VIRTUALIZED() {
        PciDeviceTO t = INTEL_82576_2_VIRTUALIZABLE();
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUALIZED.toString());
        return t;
    }

    public static PciDeviceTO INTEL_82576_VF1() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("INTEL_82576_VF1");
        t.setDescription("Ethernet controller: Intel Corporation 82576 Virtual Function");
        t.setVendorId("8086");
        t.setDeviceId("1010");
        t.setPciDeviceAddress("0e:10.1");
        t.setParentAddress("0e:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/15");
        t.setType(PciDeviceType.Ethernet_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUAL.toString());
        t.setMaxPartNum(2);
        return t;
    }

    public static PciDeviceTO INTEL_82576_VF2() {
        PciDeviceTO t = INTEL_82576_VF1();
        t.setName("INTEL_82576_VF2");
        t.setPciDeviceAddress("0e:10.3");
        t.setIommuGroup("/sys/kernel/iommu_groups/16");
        return t;
    }

    public static PciDeviceTO INTEL_82576_2_VF1() {
        PciDeviceTO t = INTEL_82576_VF1();
        t.setName("INTEL_82576_2_VF1");
        t.setPciDeviceAddress("0e:10.2");
        t.setParentAddress("0e:00.1");
        t.setIommuGroup("/sys/kernel/iommu_groups/17");
        return t;
    }

    public static PciDeviceTO INTEL_82576_2_VF2() {
        PciDeviceTO t = INTEL_82576_2_VF1();
        t.setName("INTEL_82576_VF2");
        t.setPciDeviceAddress("0e:10.4");
        t.setIommuGroup("/sys/kernel/iommu_groups/18");
        return t;
    }

    public static PciDeviceTO INTEL_82576_UNVIRTUALIZABLE() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("INTEL_82576");
        t.setDescription("Ethernet controller: Intel Corporation 82576 Gigabit Network Connection");
        t.setVendorId("8086");
        t.setDeviceId("1010");
        t.setSubvendorId("8086");
        t.setSubdeviceId("1010");
        t.setPciDeviceAddress("0d:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/19");
        t.setType(PciDeviceType.Ethernet_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.UNVIRTUALIZABLE.toString());
        t.setMaxPartNum(16);
        return t;
    }

    public static PciDeviceTO MLNX_CX5_VIRTUALIZED_PF1() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("ConnectX-5");
        t.setDescription("Mellanox Technologies MT27800 Family [ConnectX-5]");
        t.setVendorId("15b3");
        t.setDeviceId("1017");
        t.setSubvendorId("15b3");
        t.setSubdeviceId("1018");
        t.setPciDeviceAddress("0e:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/13");
        t.setType(PciDeviceType.Ethernet_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUALIZED.toString());
        t.setMaxPartNum(16);
        return t;
    }

    public static PciDeviceTO MLNX_CX5_VIRTUALIZED_PF2() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("ConnectX-5");
        t.setDescription("Mellanox Technologies MT27800 Family [ConnectX-5]");
        t.setVendorId("15b3");
        t.setDeviceId("1017");
        t.setSubvendorId("15b3");
        t.setSubdeviceId("1018");
        t.setPciDeviceAddress("0e:00.1");
        t.setIommuGroup("/sys/kernel/iommu_groups/14");
        t.setType(PciDeviceType.Ethernet_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUALIZED.toString());
        t.setMaxPartNum(16);
        return t;
    }


    public static PciDeviceTO MLNX_CX5_VF1() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("ConnectX-5_VF1");
        t.setDescription("Mellanox Technologies MT27800 Family [ConnectX-5 Virtual Function]");
        t.setVendorId("15b3");
        t.setDeviceId("1018");
        t.setPciDeviceAddress("0e:00.2");
        t.setParentAddress("0e:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/15");
        t.setType(PciDeviceType.Ethernet_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUAL.toString());
        t.setMaxPartNum(2);
        return t;
    }

    public static PciDeviceTO MLNX_CX5_VF2() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("ConnectX-5_VF2");
        t.setDescription("Mellanox Technologies MT27800 Family [ConnectX-5 Virtual Function]");
        t.setVendorId("15b3");
        t.setDeviceId("1018");
        t.setPciDeviceAddress("0e:00.3");
        t.setParentAddress("0e:00.1");
        t.setIommuGroup("/sys/kernel/iommu_groups/16");
        t.setType(PciDeviceType.Ethernet_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUAL.toString());
        t.setMaxPartNum(2);
        return t;
    }

    public static PciDeviceTO MLNX_CX5_VF3() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("ConnectX-5_VF3");
        t.setDescription("Mellanox Technologies MT27800 Family [ConnectX-5 Virtual Function]");
        t.setVendorId("15b3");
        t.setDeviceId("1018");
        t.setPciDeviceAddress("0e:00.4");
        t.setParentAddress("0e:00.0");
        t.setIommuGroup("/sys/kernel/iommu_groups/17");
        t.setType(PciDeviceType.Ethernet_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUAL.toString());
        t.setMaxPartNum(2);
        return t;
    }

    public static PciDeviceTO MLNX_CX5_VF4() {
        PciDeviceTO t = new PciDeviceTO();
        t.setName("ConnectX-5_VF4");
        t.setDescription("Mellanox Technologies MT27800 Family [ConnectX-5 Virtual Function]");
        t.setVendorId("15b3");
        t.setDeviceId("1018");
        t.setPciDeviceAddress("0e:00.5");
        t.setParentAddress("0e:00.1");
        t.setIommuGroup("/sys/kernel/iommu_groups/18");
        t.setType(PciDeviceType.Ethernet_Controller.toString());
        t.setVirtStatus(PciDeviceVirtStatus.SRIOV_VIRTUAL.toString());
        t.setMaxPartNum(2);
        return t;
    }

    static {
        m60_2a = new HashMap<>();
        m60_2a.put("TypeId", "0x10");
        m60_2a.put("Name", "GRID_M60-2A");
        m60_2a.put("Vendor", "NVIDIA");
        m60_2a.put("Instance Number", "4");
        m60_2a.put("RAM", "2048MB");
        m60_2a.put("Display Heads", "4");
        m60_2a.put("Max Resolution", "1920*1080");
        m60_2a.put("Frame Rate Limit", "60FPS");
        m60_2a.put("GRID License", "GRID-Virtual-Apps,3.0");

        m60_2b = new HashMap<>();
        m60_2b.put("TypeId", "0x11");
        m60_2b.put("Name", "GRID_M60-2B");
        m60_2b.put("Vendor", "NVIDIA");
        m60_2b.put("Instance Number", "4");
        m60_2b.put("RAM", "4096MB");
        m60_2b.put("Display Heads", "4");
        m60_2b.put("Max Resolution", "4096*2160");
        m60_2b.put("Frame Rate Limit", "60FPS");
        m60_2b.put("GRID License", "GRID-Virtual-Apps,3.0");

        t4_4q = new HashMap<>();
        t4_4q.put("TypeId", "0xe7");
        t4_4q.put("Name", "GRID_T4-4Q");
        t4_4q.put("Vendor", "NVIDIA");
        t4_4q.put("Instance Number", "4");
        t4_4q.put("RAM", "4096MB");
        t4_4q.put("Display Heads", "4");
        t4_4q.put("Max Resolution", "7680*4320");
        t4_4q.put("Frame Rate Limit", "60FPS");
        t4_4q.put("GRID License", "GRID-Virtual-Apps,3.0");
    }
}
