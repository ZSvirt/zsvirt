package org.zstack.pciDevice

import org.zstack.pciDevice.PciDeviceMetaData
import java.lang.Integer

doc {

	title "在这里输入结构的名称"

	field {
		name "id"
		desc ""
		type "long"
		since "2.1"
	}
	field {
		name "instanceOfferingUuid"
		desc "计算规格UUID"
		type "String"
		since "2.1"
	}
	field {
		name "pciDeviceOfferingUuid"
		desc ""
		type "String"
		since "2.1"
	}
	ref {
		name "metadata"
		path "org.zstack.pciDevice.PciDeviceOfferingInstanceOfferingRefInventory.metadata"
		desc "null"
		type "PciDeviceMetaData"
		since "2.1"
		clz PciDeviceMetaData.class
	}
	field {
		name "pciDeviceCount"
		desc ""
		type "Integer"
		since "2.1"
	}
}
