package org.zstack.ovf.datatype

import java.lang.Boolean

doc {

	title "OVF模板信息——光驱"

	field {
		name "autoAllocation"
		desc "是否自动分配"
		type "Boolean"
		since "3.14.6"
	}
	field {
		name "driverType"
		desc "光驱控制器类型"
		type "String"
		since "3.14.6"
	}
	field {
		name "subType"
		desc "子类型"
		type "String"
		since "3.14.6"
	}
	field {
		name "name"
		desc "光驱名称"
		type "String"
		since "3.14.6"
	}
}
