package org.zstack.snmp.agent

import org.zstack.snmp.agent.SnmpAgentInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新SNMP代理"

	ref {
		name "inventory"
		path "org.zstack.snmp.agent.APIUpdateSnmpAgentEvent.inventory"
		desc "SNMP代理信息"
		type "SnmpAgentInventory"
		since "3.17.21"
		clz SnmpAgentInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.21"
	}
	ref {
		name "error"
		path "org.zstack.snmp.agent.APIUpdateSnmpAgentEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.21"
		clz ErrorCode.class
	}
}
