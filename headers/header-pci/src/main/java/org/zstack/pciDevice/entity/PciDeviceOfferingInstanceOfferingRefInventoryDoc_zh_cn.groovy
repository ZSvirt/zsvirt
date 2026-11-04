package org.zstack.pciDevice.entity

import org.zstack.pciDevice.message.PciDeviceMetaData
import java.lang.Integer

doc {

	title "在这里输入结构的名称"

	field {
		name "id"
		desc ""
		type "long"
		since "zsv_2.1"
	}
	field {
		name "instanceOfferingUuid"
		desc "计算规格UUID"
		type "String"
		since "zsv_2.1"
	}
	field {
		name "pciDeviceOfferingUuid"
		desc ""
		type "String"
		since "zsv_2.1"
	}
	ref {
		name "metadata"
		path "org.zstack.pciDevice.entity.PciDeviceOfferingInstanceOfferingRefInventory.metadata"
		desc "null"
		type "PciDeviceMetaData"
		since "zsv_2.1"
		clz PciDeviceMetaData.class
	}
	field {
		name "pciDeviceCount"
		desc ""
		type "Integer"
		since "zsv_2.1"
	}
}
