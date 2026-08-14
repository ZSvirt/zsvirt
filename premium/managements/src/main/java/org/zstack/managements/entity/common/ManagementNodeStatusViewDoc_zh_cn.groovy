package org.zstack.managements.entity.common

import org.zstack.header.errorcode.ErrorCode

doc {

	title "单管理节点相关信息"

	field {
		name "ip"
		desc "管理节点 IP"
		type "String"
		since "4.10.20"
	}
	field {
		name "gatewayIp"
		desc "网关 IP"
		type "String"
		since "4.10.20"
	}
	field {
		name "ownsVip"
		desc "是否拥有 VIP"
		type "boolean"
		since "4.10.20"
	}
	field {
		name "peerReachable"
		desc "对端管理节点是否可访问"
		type "boolean"
		since "4.10.20"
	}
	field {
		name "gatewayReachable"
		desc "网关是否可访问"
		type "boolean"
		since "4.10.20"
	}
	field {
		name "vipReachable"
		desc "VIP 是否可访问"
		type "boolean"
		since "4.10.20"
	}
	field {
		name "keepalivedStatus"
		desc "keepalived 服务状态, 一般为 'alive'"
		type "String"
		since "4.10.20"
	}
	field {
		name "haMonitorStatus"
		desc "HA monitor 服务状态, 一般为 'alive'"
		type "String"
		since "4.10.20"
	}
	field {
		name "databaseStatus"
		desc "数据库状态, 一般为短语 'mysqld is alive'"
		type "String"
		since "4.10.20"
	}
	field {
		name "uiStatus"
		desc "UI 状态, 一般为 'running'"
		type "String"
		since "4.10.20"
	}
	field {
		name "managementsNodeStatus"
		desc "管理节点状态, 一般为 'running'"
		type "String"
		since "4.10.20"
	}
	field {
		name "slaveIoRunning"
		desc "Slave IO 是否运行"
		type "boolean"
		since "4.10.20"
	}
	field {
		name "slaveSqlRunning"
		desc "Slave SQL 是否运行"
		type "boolean"
		since "4.10.20"
	}
	field {
		name "error"
		desc "最近一次操作产生的错误信息"
		type "ErrorCode"
		since "4.10.20"
	}
}
