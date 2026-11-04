package org.zstack.pciDevice.message

import org.zstack.pciDevice.message.PciDeviceMetaDataEntry

doc {

	title "PCI设备元数据"

	field {
		name "metaData"
		desc ""
		type "String"
		since "zsv_2.1"
	}
	ref {
		name "metaDataEntries"
		path "org.zstack.pciDevice.message.PciDeviceMetaData.metaDataEntries"
		desc "元数据项"
		type "List"
		since "zsv_2.1"
		clz PciDeviceMetaDataEntry.class
	}
}
