package org.zstack.pciDevice

import org.zstack.pciDevice.PciDeviceMetaDataEntry

doc {

	title "PCI设备元数据"

	field {
		name "metaData"
		desc ""
		type "String"
		since "2.1"
	}
	ref {
		name "metaDataEntries"
		path "org.zstack.pciDevice.PciDeviceMetaData.metaDataEntries"
		desc "元数据项"
		type "List"
		since "2.1"
		clz PciDeviceMetaDataEntry.class
	}
}
