package org.zstack.header.baremetal.chassis

import org.zstack.header.baremetal.chassis.APIUpdateBaremetalChassisEvent

doc {
	title "UpdateBaremetalChassis"

	category "baremetal.chassis"

	desc """更新裸机设备"""

	rest {
		request {
			url "PUT /v1/baremetal/chassis/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateBaremetalChassisMsg.class

			desc """更新裸机设备"""

			params {

				column {
					name "uuid"
					enclosedIn "updateBaremetalChassis"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "name"
					enclosedIn "updateBaremetalChassis"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "description"
					enclosedIn "updateBaremetalChassis"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "ipmiAddress"
					enclosedIn "updateBaremetalChassis"
					desc "IPMI地址"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "ipmiPort"
					enclosedIn "updateBaremetalChassis"
					desc "IPMI端口"
					location "body"
					type "Integer"
					optional true
					since "2.6.0"
				}
				column {
					name "ipmiUsername"
					enclosedIn "updateBaremetalChassis"
					desc "IPMI用户名"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "ipmiPassword"
					enclosedIn "updateBaremetalChassis"
					desc "IPMI密码"
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
			}
		}

		response {
			clz APIUpdateBaremetalChassisEvent.class
		}
	}
}