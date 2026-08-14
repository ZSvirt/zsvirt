package org.zstack.network.securitygroup

import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.network.securitygroup.SecurityGroupRuleInventory

doc {

	title "安全组规则的清单（Security Group Rule Inventory）"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "ipVersion"
		desc "ip协议号"
		type "Integer"
		since "3.1"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	ref {
		name "rules"
		path "org.zstack.network.securitygroup.SecurityGroupInventory.rules"
		desc "null"
		type "List"
		since "0.6"
		clz SecurityGroupRuleInventory.class
	}
	field {
		name "attachedL3NetworkUuids"
		desc ""
		type "Set"
		since "0.6"
	}
}
