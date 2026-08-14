package org.zstack.snmp.agent

import java.sql.Timestamp

doc {

	title "SNMP代理"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.17.21"
	}
	field {
		name "version"
		desc "SNMP代理使用协议版本"
		type "String"
		since "3.17.21"
	}
	field {
		name "readCommunity"
		desc "读团体字(version为v2c时使用)"
		type "String"
		since "3.17.21"
	}
	field {
		name "userName"
		desc "用户名(version为v3时使用)"
		type "String"
		since "3.17.21"
	}
	field {
		name "authAlgorithm"
		desc "认证算法"
		type "String"
		since "3.17.21"
	}
	field {
		name "authPassword"
		desc "认证密码"
		type "String"
		since "3.17.21"
	}
	field {
		name "privacyAlgorithm"
		desc "隐私算法"
		type "String"
		since "3.17.21"
	}
	field {
		name "privacyPassword"
		desc "隐私密码"
		type "String"
		since "3.17.21"
	}
	field {
		name "port"
		desc "端口"
		type "int"
		since "3.17.21"
	}
	field {
		name "status"
		desc "SNMP代理是否启用"
		type "String"
		since "3.17.21"
	}
	field {
		name "securityLevel"
		desc "安全等级"
		type "String"
		since "3.17.21"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.17.21"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.17.21"
	}
}
