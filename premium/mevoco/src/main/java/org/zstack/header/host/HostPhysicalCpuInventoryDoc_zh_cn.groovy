package org.zstack.header.host

doc {

	title "物理CPU设备清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "serialNumber"
		desc "序列号"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "socketDesignation"
		desc "设备名称"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "version"
		desc "型号"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "currentSpeed"
		desc "频率"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "coreCount"
		desc "物理核数"
		type "Integer"
		since "zsv 4.10.0"
	}
	field {
		name "threadCount"
		desc "逻辑核数"
		type "Integer"
		since "zsv 4.10.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 4.10.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 4.10.0"
	}
}
