package org.zstack.ovf.datatype

import java.lang.Integer
import java.lang.Integer

doc {

	title "OVF模板信息——CPU"

	field {
		name "instanceId"
		desc "硬件ID"
		type "String"
		since "3.14.6"
	}
	field {
		name "quantity"
		desc "CPU内核数量"
		type "Integer"
		since "3.14.6"
	}
	field {
		name "coresPerSocket"
		desc "每CPU内核数"
		type "Integer"
		since "3.14.6"
	}
}
