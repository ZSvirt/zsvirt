package org.zstack.header.sriov

import org.zstack.header.sriov.EthernetVfStatus
import org.zstack.pciDevice.PciDeviceType
import org.zstack.pciDevice.PciDeviceState
import org.zstack.pciDevice.PciDeviceStatus
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus
import org.zstack.pciDevice.PciDeviceChooser
import org.zstack.pciDevice.PciDeviceMetaData
import java.sql.Timestamp
import org.zstack.pciDevice.PciDevicePciDeviceOfferingRefInventory
import org.zstack.pciDevice.specification.mdev.PciDeviceMdevSpecRefInventory

doc {

	title "网卡VF清单"

	field {
		name "hostDevUuid"
		desc "物理机Uuid"
		type "String"
		since "4.2.0"
	}
	field {
		name "interfaceName"
		desc "物理网卡名称"
		type "String"
		since "4.2.0"
	}
	field {
		name "vmUuid"
		desc "云主机Uuid"
		type "String"
		since "4.2.0"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "4.2.0"
	}
	ref {
		name "vfStatus"
		path "org.zstack.header.sriov.EthernetVfPciDeviceInventory.vfStatus"
		desc "网卡VF使用状态"
		type "EthernetVfStatus"
		since "4.2.0"
		clz EthernetVfStatus.class
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.2.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.2.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "4.2.0"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "4.2.0"
	}
	field {
		name "parentUuid"
		desc ""
		type "String"
		since "4.2.0"
	}
	field {
		name "vmInstanceUuid"
		desc "虚拟机 UUID"
		type "String"
		since "4.2.0"
	}
	field {
		name "pciSpecUuid"
		desc "PCI 设备规格 UUID"
		type "String"
		since "4.2.0"
	}
	ref {
		name "type"
		path "org.zstack.header.sriov.EthernetVfPciDeviceInventory.type"
		desc "PCI 设备类型"
		type "PciDeviceType"
		since "4.2.0"
		clz PciDeviceType.class
	}
	ref {
		name "state"
		path "org.zstack.header.sriov.EthernetVfPciDeviceInventory.state"
		desc "PCI 设备是否启用"
		type "PciDeviceState"
		since "4.2.0"
		clz PciDeviceState.class
	}
	ref {
		name "status"
		path "org.zstack.header.sriov.EthernetVfPciDeviceInventory.status"
		desc "PCI 设备绑定状态"
		type "PciDeviceStatus"
		since "4.2.0"
		clz PciDeviceStatus.class
	}
	ref {
		name "virtStatus"
		path "org.zstack.header.sriov.EthernetVfPciDeviceInventory.virtStatus"
		desc "PCI 设备虚拟化状态"
		type "PciDeviceVirtStatus"
		since "4.2.0"
		clz PciDeviceVirtStatus.class
	}
	ref {
		name "chooser"
		path "org.zstack.header.sriov.EthernetVfPciDeviceInventory.chooser"
		desc "PCI 设备通过规格指定还是设备指定"
		type "PciDeviceChooser"
		since "4.2.0"
		clz PciDeviceChooser.class
	}
	field {
		name "vendorId"
		desc ""
		type "String"
		since "4.2.0"
	}
	field {
		name "deviceId"
		desc ""
		type "String"
		since "4.2.0"
	}
	field {
		name "subvendorId"
		desc ""
		type "String"
		since "4.2.0"
	}
	field {
		name "subdeviceId"
		desc ""
		type "String"
		since "4.2.0"
	}
	field {
		name "pciDeviceAddress"
		desc ""
		type "String"
		since "4.2.0"
	}
	field {
		name "iommuGroup"
		desc ""
		type "String"
		since "4.2.0"
	}
	ref {
		name "metaData"
		path "org.zstack.header.sriov.EthernetVfPciDeviceInventory.metaData"
		desc "null"
		type "PciDeviceMetaData"
		since "4.2.0"
		clz PciDeviceMetaData.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.2.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.2.0"
	}
	ref {
		name "matchedPciDeviceOfferingRef"
		path "org.zstack.header.sriov.EthernetVfPciDeviceInventory.matchedPciDeviceOfferingRef"
		desc "匹配的 PCI 设备规格映射清单列表"
		type "List"
		since "4.2.0"
		clz PciDevicePciDeviceOfferingRefInventory.class
	}
	ref {
		name "mdevSpecRefs"
		path "org.zstack.header.sriov.EthernetVfPciDeviceInventory.mdevSpecRefs"
		desc "PCI MDev 规格映射清单列表"
		type "List"
		since "4.2.0"
		clz PciDeviceMdevSpecRefInventory.class
	}
}
