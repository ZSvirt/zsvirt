package org.zstack.ovf.datatype

import java.lang.Long

doc {

	title "OVF模板信息——内存"

	field {
		name "instanceId"
		desc "硬件ID"
		type "String"
		since "3.14.6"
	}
	field {
		name "quantity"
		desc "内存容量，单位Byte"
		type "Long"
		since "3.14.6"
	}
}
