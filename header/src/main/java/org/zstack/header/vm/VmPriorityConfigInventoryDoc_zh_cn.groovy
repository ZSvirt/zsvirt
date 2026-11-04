package org.zstack.header.vm

import org.zstack.header.vm.VmPriorityLevel
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "虚拟机优先级配置"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv_3.7"
	}
	field {
		name "accountUuid"
		desc "账户UUID"
		type "String"
		since "zsv_3.7"
	}
	ref {
		name "level"
		path "org.zstack.header.vm.VmPriorityConfigInventory.level"
		desc "null"
		type "VmPriorityLevel"
		since "zsv_3.7"
		clz VmPriorityLevel.class
	}
	field {
		name "cpuShares"
		desc ""
		type "int"
		since "zsv_3.7"
	}
	field {
		name "oomScoreAdj"
		desc ""
		type "int"
		since "zsv_3.7"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv_3.7"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv_3.7"
	}
}
