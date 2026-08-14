package org.zstack.pciDevice

import org.zstack.pciDevice.PciDeviceMetaDataEntry.PciDeviceMetaDataOperator

doc {

	title "PCI设备元数据项"

	field {
		name "key"
		desc "键"
		type "String"
		since "2.1"
	}
	ref {
		name "op"
		path "org.zstack.pciDevice.PciDeviceMetaDataEntry.op"
		desc "操作符"
		type "PciDeviceMetaDataOperator"
		since "2.1"
		clz PciDeviceMetaDataOperator.class
	}
	field {
		name "value"
		desc "值"
		type "String"
		since "2.1"
	}
}
