package org.zstack.ovf.datatype

import java.lang.Boolean

doc {

	title "OVF模板信息——网卡"

	field {
		name "networkName"
		desc "网络名称"
		type "String"
		since "3.14.6"
	}
	field {
		name "nicModel"
		desc "网卡型号"
		type "String"
		since "3.14.6"
	}
	field {
		name "nicName"
		desc "网卡名称"
		type "String"
		since "3.14.6"
	}
	field {
		name "autoAllocation"
		desc "是否自动分配"
		type "Boolean"
		since "3.14.6"
	}
}
