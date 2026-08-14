package org.zstack.pciDevice

import org.zstack.pciDevice.PciDeviceOfferingType
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.pciDevice.PciDeviceOfferingInstanceOfferingRefInventory
import org.zstack.pciDevice.PciDevicePciDeviceOfferingRefInventory

doc {

	title "PCI设备规格清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.1"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.1"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.1"
	}
	ref {
		name "type"
		path "org.zstack.pciDevice.PciDeviceOfferingInventory.type"
		desc "规格类型"
		type "PciDeviceOfferingType"
		since "2.1"
		clz PciDeviceOfferingType.class
	}
	field {
		name "vendorId"
		desc "供应商ID"
		type "String"
		since "2.1"
	}
	field {
		name "deviceId"
		desc "设备ID"
		type "String"
		since "2.1"
	}
	field {
		name "subvendorId"
		desc "子供应商ID"
		type "String"
		since "2.1"
	}
	field {
		name "subdeviceId"
		desc "子设备ID"
		type "String"
		since "2.1"
	}
	field {
		name "ramSize"
		desc "显存容量"
		type "String"
		since "3.5.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.1"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.1"
	}
	ref {
		name "attachedInstanceOfferings"
		path "org.zstack.pciDevice.PciDeviceOfferingInventory.attachedInstanceOfferings"
		desc "null"
		type "List"
		since "2.1"
		clz PciDeviceOfferingInstanceOfferingRefInventory.class
	}
	ref {
		name "matchedPciDevices"
		path "org.zstack.pciDevice.PciDeviceOfferingInventory.matchedPciDevices"
		desc "null"
		type "List"
		since "2.1"
		clz PciDevicePciDeviceOfferingRefInventory.class
	}
}
