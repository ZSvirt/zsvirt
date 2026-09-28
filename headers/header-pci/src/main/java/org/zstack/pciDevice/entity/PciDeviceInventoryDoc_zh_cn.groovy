package org.zstack.pciDevice.entity


import org.zstack.pciDevice.header.PciDeviceType
import org.zstack.pciDevice.header.PciDeviceState
import org.zstack.pciDevice.header.PciDeviceStatus
import org.zstack.pciDevice.header.virtual.PciDeviceVirtStatus
import org.zstack.pciDevice.header.PciDevicePassThroughState
import org.zstack.pciDevice.message.PciDeviceMetaData
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.pciDevice.entity.PciDevicePciDeviceOfferingRefInventory
import org.zstack.pciDevice.entity.specification.mdev.PciDeviceMdevSpecRefInventory

doc {

	title "PCI 设备清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv_2.1"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv_3.5.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv_2.1"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "zsv_2.1"
	}
	field {
		name "parentUuid"
		desc "物理PCI设备UUID"
		type "String"
		since "zsv_3.5.0"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "zsv_2.1"
	}
	field {
		name "pciSpecUuid"
		desc "PCI设备规格UUID"
		type "String"
		since "zsv_3.5.0"
	}
	ref {
		name "type"
		path "org.zstack.pciDevice.entity.PciDeviceInventory.type"
		desc "PCI设备类型"
		type "PciDeviceType"
		since "zsv_2.1"
		clz PciDeviceType.class
	}
	ref {
		name "state"
		path "org.zstack.pciDevice.entity.PciDeviceInventory.state"
		desc "PCI设备启用状态"
		type "PciDeviceState"
		since "zsv_2.1"
		clz PciDeviceState.class
	}
	ref {
		name "status"
		path "org.zstack.pciDevice.entity.PciDeviceInventory.status"
		desc "PCI设备挂载状态"
		type "PciDeviceStatus"
		since "zsv_2.1"
		clz PciDeviceStatus.class
	}
	ref {
		name "virtStatus"
		path "org.zstack.pciDevice.entity.PciDeviceInventory.virtStatus"
		desc "PCI设备虚拟类型(可虚拟化设备、已虚拟化设备、虚拟设备)"
		type "PciDeviceVirtStatus"
		since "zsv_3.5.0"
		clz PciDeviceVirtStatus.class
	}
	ref {
		name "passThroughState"
		path "org.zstack.pciDevice.entity.PciDeviceInventory.passThroughState"
		desc "PCI设备直通状态"
		type "PciDevicePassThroughState"
		since "zsv_4.10.0"
		clz PciDevicePassThroughState.class
	}
	ref {
		name "chooser"
		path "org.zstack.pciDevice.entity.PciDeviceInventory.chooser"
		desc "PCI设备选取者"
		type "PciDeviceChooser"
		since "zsv_3.11.0"
		clz PciDeviceChooser.class
	}
	field {
		name "vendorId"
		desc "供应商ID"
		type "String"
		since "zsv_2.1"
	}
	field {
		name "deviceId"
		desc "设备ID"
		type "String"
		since "zsv_2.1"
	}
	field {
		name "subvendorId"
		desc "子供应商ID"
		type "String"
		since "zsv_2.1"
	}
	field {
		name "subdeviceId"
		desc "子设备ID"
		type "String"
		since "zsv_2.1"
	}
	field {
		name "pciDeviceAddress"
		desc "PCI设备地址"
		type "String"
		since "zsv_2.1"
	}
	ref {
		name "metaData"
		path "org.zstack.pciDevice.entity.PciDeviceInventory.metaData"
		desc "PCI设备元数据"
		type "PciDeviceMetaData"
		since "zsv_2.1"
		clz PciDeviceMetaData.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv_2.1"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv_2.1"
	}
	ref {
		name "matchedPciDeviceOfferingRef"
		path "org.zstack.pciDevice.entity.PciDeviceInventory.matchedPciDeviceOfferingRef"
		desc "null"
		type "List"
		since "zsv_2.1"
		clz PciDevicePciDeviceOfferingRefInventory.class
	}
	ref {
		name "mdevSpecRefs"
		path "org.zstack.pciDevice.entity.PciDeviceInventory.mdevSpecRefs"
		desc "可用MDEV设备规格"
		type "List"
		since "zsv_3.5.0"
		clz PciDeviceMdevSpecRefInventory.class
	}
}
