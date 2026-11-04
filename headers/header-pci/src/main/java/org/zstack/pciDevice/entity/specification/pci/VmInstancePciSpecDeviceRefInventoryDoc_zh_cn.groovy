package org.zstack.pciDevice.entity.specification.pci

doc {

	title "虚拟机与PCI规格及设备的关联关系"

	field {
		name "vmInstanceUuid"
		desc "虚拟机UUID"
		type "String"
		since "zsv_3.5.0"
	}
	field {
		name "pciSpecUuid"
		desc "PCI设备规格UUID"
		type "String"
		since "zsv_3.5.0"
	}
	field {
		name "pciDeviceUuid"
		desc "PCI设备UUID"
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
