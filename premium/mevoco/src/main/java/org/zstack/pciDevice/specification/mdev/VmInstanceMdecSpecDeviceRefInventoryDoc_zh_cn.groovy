package org.zstack.pciDevice.specification.mdev

doc {

	title "云主机与MDEV规格及设备的关联关系"

	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "3.5.0"
	}
	field {
		name "mdevSpecUuid"
		desc "MDEV设备规格"
		type "String"
		since "3.5.0"
	}
	field {
		name "mdevDeviceUuid"
		desc "MDEV设备UUID"
		type "String"
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
