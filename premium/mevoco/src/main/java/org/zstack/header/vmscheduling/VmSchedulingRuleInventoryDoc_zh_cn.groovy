package org.zstack.header.vmscheduling

import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.header.affinitygroup.AffinityGroupUsageInventory

doc {

	title "虚拟机调度规则"

	field {
		name "rule"
		desc "规则"
		type "String"
		since "3.16.0"
	}
	field {
		name "mode"
		desc "执行模式"
		type "String"
		since "3.16.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.16.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.16.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.16.0"
	}
	field {
		name "policy"
		desc ""
		type "String"
		since "3.16.0"
	}
	field {
		name "version"
		desc ""
		type "String"
		since "3.16.0"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "3.16.0"
	}
	field {
		name "appliance"
		desc ""
		type "String"
		since "3.16.0"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "3.16.0"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "3.16.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.16.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.16.0"
	}
	ref {
		name "usages"
		path "org.zstack.header.vmscheduling.VmSchedulingRuleInventory.usages"
		desc "null"
		type "List"
		since "3.16.0"
		clz AffinityGroupUsageInventory.class
	}
}
