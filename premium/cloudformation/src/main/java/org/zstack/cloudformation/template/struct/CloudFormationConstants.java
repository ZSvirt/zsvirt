package org.zstack.cloudformation.template.struct;

import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.*;

/**
 * Created by mingjian.deng on 2018/5/30.
 */
public interface CloudFormationConstants {
    String split = "::";
    String funcPrefix = "Fn::";
    String funcPackage = "org.zstack.cloudformation.template.function.";
    String funcSuffix = "TemplateFunction";
    List<String> paramKeys = list("Type", "Description", "DefaultValue",
            "NoEcho", "ConstraintDescription", "Label");
    List<String> functions = list("Ref", "Fn::GetAtt", "Fn::Join", "Fn::FindInMap", "Fn::Base64", "Fn::Split",
            "Fn::Select", "Fn::Not", "Fn::Equal", "Fn::And", "Fn::Or", "Fn::If");
    List<String> resourceKeys = list("Type", "Properties", "DependsOn", "DeletionPolicy", "Description", "MockFailed");
    List<String> supportedResources = list("AffinityGroup","DataVolume","DataVolumeTemplate","DiskOffering","Eip",
            "IPsecConnection","Image","InstanceOffering","L2NoVlanNetwork","L2VlanNetwork","L2VxlanNetwork",
            "L2VxlanNetworkPool","L3Network","LoadBalancer","LoadBalancerListener","PortForwardingRule",
            "RootVolumeTemplate","SecurityGroup","SecurityGroupRule","VRouterRouteTable","Vip","VmInstance",
            "VpcVRouter","VirtualRouterOffering","VniRange","SystemTag","UserTag", "DataVolumeFromVolumeTemplate", "Tag");
    List<String> supportedActions = list("AddIpRange","AddDnsToL3Network",
            "AddSecurityGroupRule","AddVmToAffinityGroup","AddVRouterRouteEntry","AddCertificateToLoadBalancerListener",
            "AddIpRangeByNetworkCidr","AddVmNicToLoadBalancer","AddVmNicToSecurityGroup","AddRemoteCidrsToIPsecConnection",
            "AttachEip","AttachDataVolumeToVm","AttachPortForwardingRule","AttachIsoToVmInstance","AttachPciDeviceToVm",
            "AttachUsbDeviceToVm","AttachL2NetworkToCluster","AttachL3NetworkToVm","AttachNetworkServiceToL3Network",
            "AttachSecurityGroupToL3Network","AttachL3NetworksToIPsecConnection","SetL3NetworkRouterInterfaceIp",
            "AttachVRouterRouteTableToVRouter","AddCertificateToLoadBalancerListener","AddHostRouteToL3Network",
            "AddDnsToVpcRouter", "AttachTagToResources", "UpdateTag", "ResizeDataVolume", "ResizeRootVolume",
            "AddResourceStackVmPortMonitor");

    List<String> expungeResources = list("VmInstance", "Image", "DataVolume", "DataVolumeFromVolumeTemplate", "Volume");

    Map<String, String> resourceDelete = map(e("VmInstance", "DestroyVmInstance"), e("Volume", "DeleteDataVolume"),
            e("DataVolumeFromVolumeTemplate", "DeleteDataVolume"), e("VirtualRouterOffering", "DeleteInstanceOffering"), e("L2VxlanNetworkPool", "DeleteL2Network"),
            e("L2VxlanNetwork", "DeleteL2Network"), e("VirtualRouterVm", "DestroyVmInstance"),
            e("L2VlanNetwork", "DeleteL2Network"),
            e("SystemTag", "DeleteTag"), e("UserTag", "DeleteTag"), e("TagPattern", "DeleteTag"));
    Map<String, String> resourceExpunge = map(e("Volume", "ExpungeDataVolume"), e("DataVolumeFromVolumeTemplate", "ExpungeDataVolume"), e("VirtualRouterVm", "ExpungeVmInstance"));
    Map<String, String> resourceCreatePrefix = map(e("Image", "Add"), e("SecurityGroupRule", "Add"));

    Map<String, String> resourceCreateSuffix = map(e("RootVolumeTemplate", "FromRootVolume"),
            e("DataVolumeTemplate", "FromVolume"));
    Map<String, String> resourceExpungeParameter = map(e("Image", "imageUuid"));

    List<String> notInRefResources = list("org.zstack.header.tag.SystemTagInventory");

    String apiPrefix = "API";
    String createPrefix = "Create";
    String deletePrefix = "Delete";
    String expungePrefix = "Expunge";
    String actionSuffix = "Action";
    String msgSuffix = "Msg";

    String sdkPackage = "org.zstack.sdk.";
}
