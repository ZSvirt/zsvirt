package org.zstack.header.host

doc {

	title "物理内存卡设备清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.12.0"
	}
	field {
		name "hostUuid"
		desc "主机 UUID"
		type "String"
		since "3.12.0"
	}
	field {
		name "manufacturer"
		desc "内存卡制造商"
		type "String"
		since "3.12.0"
	}
	field {
		name "size"
		desc "内存卡容量"
		type "String"
		since "3.12.0"
	}
	field {
		name "speed"
		desc "内存卡速率"
		type "String"
		since "3.12.0"
	}
	field {
		name "clockSpeed"
		desc "内存卡时钟速率"
		type "String"
		since "3.12.0"
	}
	field {
		name "locator"
		desc "内存卡位置"
		type "String"
		since "3.12.0"
	}
	field {
		name "serialNumber"
		desc "序列号"
		type "String"
		since "3.12.0"
	}
	field {
		name "rank"
		desc "级别"
		type "String"
		since "3.12.0"
	}
	field {
		name "voltage"
		desc "内存卡当前电压"
		type "String"
		since "3.12.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.12.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.12.0"
	}
}
