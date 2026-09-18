package org.zstack.pciDevice.specification.pci

import org.zstack.pciDevice.PciDeviceType

doc {

	title "PCI设备规格清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv_3.5.0"
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
		since "zsv_3.5.0"
	}
	field {
		name "vendorId"
		desc "供应商ID"
		type "String"
		since "zsv_3.5.0"
	}
	field {
		name "deviceId"
		desc "设备ID"
		type "String"
		since "zsv_3.5.0"
	}
	field {
		name "subvendorId"
		desc "子供应商ID"
		type "String"
		since "zsv_3.5.0"
	}
	field {
		name "subdeviceId"
		desc "子设备ID"
		type "String"
		since "zsv_3.5.0"
	}
	field {
		name "ramSize"
		desc "显存容量"
		type "String"
		since "zsv_3.5.0"
	}
	field {
		name "maxPartNum"
		desc "最大切分数量"
		type "Integer"
		since "zsv_3.5.0"
	}
	ref {
		name "type"
		path "org.zstack.pciDevice.specification.pci.PciDeviceSpecInventory.type"
		desc "PCI设备类型"
		type "PciDeviceType"
		since "zsv_3.5.0"
		clz PciDeviceType.class
	}
	ref {
		name "state"
		path "org.zstack.pciDevice.specification.pci.PciDeviceSpecInventory.state"
		desc "PCI设备规格状态"
		type "PciDeviceSpecState"
		since "zsv_3.5.0"
		clz PciDeviceSpecState.class
	}
	field {
		name "isVirtual"
		desc "是否虚拟设备"
		type "Boolean"
		since "zsv_3.5.0"
	}
	field {
		name "romVersion"
		desc "固件版本"
		type "String"
		since "zsv_3.5.0"
	}
	field {
		name "romMd5sum"
		desc "固件MD5"
		type "String"
		since "zsv_3.5.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv_3.5.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv_3.5.0"
	}
}
