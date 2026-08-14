package org.zstack.snmp.agent

import org.zstack.snmp.agent.APIUpdateSnmpAgentEvent

doc {
	title "UpdateSnmpAgent"

	category "SNMP"

	desc """更新SNMP代理"""

	rest {
		request {
			url "PUT /v1/snmp/agent/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSnmpAgentMsg.class

			desc """更新SNMP代理消息"""

			params {

				column {
					name "uuid"
					enclosedIn "updateSnmpAgent"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "version"
					enclosedIn "updateSnmpAgent"
					desc "SNMP代理使用协议版本"
					location "body"
					type "String"
					optional false
					since "3.17.21"
					values ("v2c","v3")
				}
				column {
					name "readCommunity"
					enclosedIn "updateSnmpAgent"
					desc "读团体字(version为v2c时使用)"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "userName"
					enclosedIn "updateSnmpAgent"
					desc "用户名(version为v3时使用)"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "authAlgorithm"
					enclosedIn "updateSnmpAgent"
					desc "认证算法"
					location "body"
					type "String"
					optional true
					since "3.17.21"
					values ("MD5","SHA","SHA224","SHA256","SHA384","SHA512")
				}
				column {
					name "authPassword"
					enclosedIn "updateSnmpAgent"
					desc "认证密码"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "privacyAlgorithm"
					enclosedIn "updateSnmpAgent"
					desc "隐私算法"
					location "body"
					type "String"
					optional true
					since "3.17.21"
					values ("DES","AES128","AES192","AES256","3DES")
				}
				column {
					name "privacyPassword"
					enclosedIn "updateSnmpAgent"
					desc "隐私密码"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "port"
					enclosedIn "updateSnmpAgent"
					desc "端口"
					location "body"
					type "int"
					optional false
					since "3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APIUpdateSnmpAgentEvent.class
		}
	}
}