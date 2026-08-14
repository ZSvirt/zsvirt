package org.zstack.header.baremetal.network

import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "裸金属网卡绑定清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.4.0"
	}
	field {
		name "chassisUuid"
		desc "裸金属设备UUID"
		type "String"
		since "3.4.0"
	}
	field {
		name "name"
		desc "网卡绑定名称"
		type "String"
		since "3.4.0"
	}
	field {
		name "mode"
		desc "网卡绑定模式"
		type "Integer"
		since "3.4.0"
	}
	field {
		name "slaves"
		desc "Slave MAC地址，逗号分隔"
		type "String"
		since "3.4.0"
	}
	field {
		name "opts"
		desc "网卡绑定选项"
		type "String"
		since "3.4.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.4.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.4.0"
	}
}
