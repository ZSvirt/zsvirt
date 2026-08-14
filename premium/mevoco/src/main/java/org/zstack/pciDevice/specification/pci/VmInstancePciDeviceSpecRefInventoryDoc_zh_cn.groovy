package org.zstack.pciDevice.specification.pci

doc {

	title "云主机与PCI设备规格的关联关系"

	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "3.5.0"
	}
	field {
		name "pciSpecUuid"
		desc "PCI设备规格UUID"
		type "String"
		since "3.5.0"
	}
	field {
		name "pciDeviceNumber"
		desc "需要为云主机挂载的符合设备规格的设备个数，默认为1"
		type "Integer"
		since "3.5.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.5.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.5.0"
	}
}
