package org.zstack.header.baremetal.chassis

import org.zstack.header.baremetal.chassis.APICreateBaremetalChassisEvent

doc {
	title "CreateBaremetalChassis"

	category "baremetal.chassis"

	desc """创建裸机设备"""

	rest {
		request {
			url "POST /v1/baremetal/chassis"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateBaremetalChassisMsg.class

			desc """创建裸机设备"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "body"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "ipmiAddress"
					enclosedIn "params"
					desc "IPMI地址"
					location "body"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "ipmiPort"
					enclosedIn "params"
					desc "IPMI端口"
					location "body"
					type "Integer"
					optional true
					since "2.6.0"
				}
				column {
					name "ipmiUsername"
					enclosedIn "params"
					desc "IPMI用户名"
					location "body"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "ipmiPassword"
					enclosedIn "params"
					desc "IPMI密码"
					location "body"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.6.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateBaremetalChassisEvent.class
		}
	}
}