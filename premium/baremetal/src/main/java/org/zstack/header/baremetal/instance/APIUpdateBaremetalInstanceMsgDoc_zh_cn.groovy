package org.zstack.header.baremetal.instance

import org.zstack.header.baremetal.instance.APIUpdateBaremetalInstanceEvent

doc {
	title "UpdateBaremetalInstance"

	category "baremetal.instance"

	desc """更新裸机实例"""

	rest {
		request {
			url "PUT /v1/baremetal/instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateBaremetalInstanceMsg.class

			desc """更新裸机实例"""

			params {

				column {
					name "uuid"
					enclosedIn "updateBaremetalInstance"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "name"
					enclosedIn "updateBaremetalInstance"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "description"
					enclosedIn "updateBaremetalInstance"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "password"
					enclosedIn "updateBaremetalInstance"
					desc "系统ROOT密码"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "platform"
					enclosedIn "updateBaremetalInstance"
					desc "系统平台"
					location "body"
					type "String"
					optional true
					since "2.6.0"
					values ("Linux")
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
			clz APIUpdateBaremetalInstanceEvent.class
		}
	}
}