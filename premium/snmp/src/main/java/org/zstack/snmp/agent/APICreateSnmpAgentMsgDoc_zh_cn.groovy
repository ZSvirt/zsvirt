package org.zstack.snmp.agent

import org.zstack.snmp.agent.APICreateSnmpAgentEvent

doc {
	title "CreateSnmpAgent"

	category "SNMP"

	desc """创建SNMP代理"""

	rest {
		request {
			url "POST /v1/snmp/agent"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSnmpAgentMsg.class

			desc """创建SNMP代理消息"""

			params {

				column {
					name "version"
					enclosedIn "params"
					desc "SNMP代理使用协议版本"
					location "body"
					type "String"
					optional false
					since "3.17.21"
					values ("v2c","v3")
				}
				column {
					name "readCommunity"
					enclosedIn "params"
					desc "读团体字(version为v2c时使用)"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "userName"
					enclosedIn "params"
					desc "用户名(version为v3时使用)"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "authAlgorithm"
					enclosedIn "params"
					desc "认证算法"
					location "body"
					type "String"
					optional true
					since "3.17.21"
					values ("MD5","SHA","SHA224","SHA256","SHA384","SHA512")
				}
				column {
					name "authPassword"
					enclosedIn "params"
					desc "认证密码"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "privacyAlgorithm"
					enclosedIn "params"
					desc "隐私算法"
					location "body"
					type "String"
					optional true
					since "3.17.21"
					values ("DES","AES128","AES192","AES256","3DES")
				}
				column {
					name "privacyPassword"
					enclosedIn "params"
					desc "隐私密码"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "port"
					enclosedIn "params"
					desc "端口"
					location "body"
					type "int"
					optional false
					since "3.17.21"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
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
			clz APICreateSnmpAgentEvent.class
		}
	}
}