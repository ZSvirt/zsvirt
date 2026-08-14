package org.zstack.header.host

import org.zstack.header.host.APIUpdateHostNetworkInterfaceEvent

doc {
	title "UpdateHostNetworkInterface"

	category "host"

	desc """更新interface网口"""

	rest {
		request {
			url "POST /v1/hosts/nics/{interfaceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateHostNetworkInterfaceMsg.class

			desc """"""

			params {

				column {
					name "interfaceUuid"
					enclosedIn "params"
					desc "interface网口uuid"
					location "url"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
			}
		}

		response {
			clz APIUpdateHostNetworkInterfaceEvent.class
		}
	}
}