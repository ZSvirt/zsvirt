package org.zstack.pluginpremium.externalapiadapter;

import org.zstack.aliyunproxy.vpc.AliyunProxyVpcVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.network.service.eip.EipVO;
import org.zstack.network.service.lb.LoadBalancerVO;
import org.zstack.tag.SystemTag;
import org.zstack.vrouterRoute.VRouterRouteTableVO;

/**
 * @Author: fubang
 * @Date: 2018/5/23
 */
@TagDefinition
public class EcsSystemTags {
    public static String VSWITCH_ID_TOKEN = "vSwitchId";
    public static SystemTag VSWITCH_ID = new SystemTag(String.format("vSwitchId::{%s}", VSWITCH_ID_TOKEN), VmNicVO.class);

    public static String SECURITYGROUP_ID_TOKEN = "securityGroupId";
    public final static String SECURITYGROUP_SPLIT_CHAR = ",";
    public static SystemTag SECURITYGROUP_ID = new SystemTag(String.format("securityGroupId::{%s}", SECURITYGROUP_ID_TOKEN), VmNicVO.class);
    public static SystemTag VPC_SECURITYGROUP_ID = new SystemTag(String.format("securityGroupId::{%s}", SECURITYGROUP_ID_TOKEN), AliyunProxyVpcVO.class);

    public static String DEFAULT_ROUTE_TABLE_TOKEN = "defaultRouteTable";
    public static SystemTag DEFAULT_ROUTE_TABLE = new SystemTag(String.format("defaultRouteTable::{%s}", DEFAULT_ROUTE_TABLE_TOKEN), AliyunProxyVpcVO.class);

    public static String DELETE_WITH_INSTANCE_TOKEN = "deleteWithInstance";
    public static SystemTag DELETE_WITH_INSTANCE = new SystemTag(String.format("deleteWithInstance::true", DELETE_WITH_INSTANCE_TOKEN), VolumeVO.class);

    public static String VROUTE_TABLE_FOR_VPC_TOKEN = "vRouteTableForVPC";
    public static SystemTag VROUTE_TABLE_FOR_VPC = new SystemTag(String.format("vRouteTableForVPC::{%s}", VROUTE_TABLE_FOR_VPC_TOKEN), VRouterRouteTableVO.class);

    public static String SLB_BACKEND_SERVER_TOKEN = "vmUuid";
    public static String SLB_BACKEND_NIC_TOKEN = "nicUuid";
    public static String SLB_BACKEND_TYPE_TOKEN = "type";
    public static String SLB_BACKEND_WEIGHT_TOKEN = "weight";
    public static SystemTag SLB_BACKEND_SERVER_EXT = new SystemTag(String.format("backendServerExt::{%s}::{%s}::{%s}::{%s}",
            SLB_BACKEND_SERVER_TOKEN, SLB_BACKEND_NIC_TOKEN, SLB_BACKEND_TYPE_TOKEN, SLB_BACKEND_WEIGHT_TOKEN), LoadBalancerVO.class);
    public static SystemTag SLB_BACKEND_SERVER = new SystemTag(String.format("backendServer::{%s}::{%s}",
            SLB_BACKEND_SERVER_TOKEN, SLB_BACKEND_NIC_TOKEN), LoadBalancerVO.class);

    public static String PCI_DEVICE_INFO_DEVICEID_TOKEN = "pciDeviceDeviceId";
    public static String PCI_DEVICE_INFO_VENDORID_TOKEN = "pciDeviceVendorId";
    public static String PCI_DEVICE_INFO_SUBDEVICEID_TOKEN = "pciDeviceSubDeviceId";
    public static String PCI_DEVICE_INFO_AMOUNT_TOKEN = "pciDeviceAmount";
    public static SystemTag PCI_DEVICE_INFO = new SystemTag(String.format("pciDeviceInfo::{%s}::{%s}::{%s}::{%s}",
            PCI_DEVICE_INFO_DEVICEID_TOKEN, PCI_DEVICE_INFO_VENDORID_TOKEN, PCI_DEVICE_INFO_SUBDEVICEID_TOKEN, PCI_DEVICE_INFO_AMOUNT_TOKEN),
            InstanceOfferingVO.class);

    public static String EIP_INTERMEDIATE_STATUS_TOKEN = "intermediateStatus";
    public static SystemTag EIP_INTERMEDIATE_STATUS = new SystemTag(String.format("eipStatus::{%s}",
            EIP_INTERMEDIATE_STATUS_TOKEN), EipVO.class);
}
