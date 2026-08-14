package org.zstack.pciDevice.gpu

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

	title "GPU 设备信息"

	field {
		name "serialNumber"
		desc "序列号"
		type "String"
		since "4.10.6"
	}
	field {
		name "memory"
		desc "显存"
		type "String"
		since "4.10.6"
	}
	field {
		name "power"
		desc "能耗"
		type "String"
		since "4.10.6"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.10.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.10.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "4.10.6"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "4.10.6"
	}
	field {
		name "parentUuid"
		desc ""
		type "String"
		since "4.10.6"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "4.10.6"
	}
	field {
		name "pciSpecUuid"
		desc ""
		type "String"
		since "4.10.6"
	}
	ref {
		name "type"
		path "org.zstack.pciDevice.gpu.GpuDeviceInventory.type"
		desc "null"
		type "PciDeviceType"
		since "4.10.6"
		clz PciDeviceType.class
	}
	ref {
		name "state"
		path "org.zstack.pciDevice.gpu.GpuDeviceInventory.state"
		desc "null"
		type "PciDeviceState"
		since "4.10.6"
		clz PciDeviceState.class
	}
	ref {
		name "status"
		path "org.zstack.pciDevice.gpu.GpuDeviceInventory.status"
		desc "null"
		type "PciDeviceStatus"
		since "4.10.6"
		clz PciDeviceStatus.class
	}
	ref {
		name "virtStatus"
		path "org.zstack.pciDevice.gpu.GpuDeviceInventory.virtStatus"
		desc "null"
		type "PciDeviceVirtStatus"
		since "4.10.6"
		clz PciDeviceVirtStatus.class
	}
	ref {
		name "chooser"
		path "org.zstack.pciDevice.gpu.GpuDeviceInventory.chooser"
		desc "null"
		type "PciDeviceChooser"
		since "4.10.6"
		clz PciDeviceChooser.class
	}
	field {
		name "vendorId"
		desc ""
		type "String"
		since "4.10.6"
	}
	field {
		name "vendor"
		desc ""
		type "String"
		since "4.10.6"
	}
	field {
		name "deviceId"
		desc ""
		type "String"
		since "4.10.6"
	}
	field {
		name "device"
		desc ""
		type "String"
		since "4.10.6"
	}
	field {
		name "subvendorId"
		desc ""
		type "String"
		since "4.10.6"
	}
	field {
		name "subdeviceId"
		desc ""
		type "String"
		since "4.10.6"
	}
	field {
		name "pciDeviceAddress"
		desc ""
		type "String"
		since "4.10.6"
	}
	field {
		name "iommuGroup"
		desc ""
		type "String"
		since "4.10.6"
	}
	ref {
		name "metaData"
		path "org.zstack.pciDevice.gpu.GpuDeviceInventory.metaData"
		desc "null"
		type "PciDeviceMetaData"
		since "4.10.6"
		clz PciDeviceMetaData.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.10.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.10.6"
	}
	ref {
		name "matchedPciDeviceOfferingRef"
		path "org.zstack.pciDevice.gpu.GpuDeviceInventory.matchedPciDeviceOfferingRef"
		desc "PCI 规格清单列表"
		type "List"
		since "4.10.6"
		clz PciDevicePciDeviceOfferingRefInventory.class
	}
	ref {
		name "mdevSpecRefs"
		path "org.zstack.pciDevice.gpu.GpuDeviceInventory.mdevSpecRefs"
		desc "MDev 规格清单列表"
		type "List"
		since "4.10.6"
		clz PciDeviceMdevSpecRefInventory.class
	}
}
