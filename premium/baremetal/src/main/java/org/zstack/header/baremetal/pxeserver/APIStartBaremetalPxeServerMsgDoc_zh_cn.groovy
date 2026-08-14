package org.zstack.header.baremetal.pxeserver

import org.zstack.header.baremetal.pxeserver.APIStartBaremetalPxeServerEvent

doc {
	title "StartBaremetalPxeServer"

	category "baremetal.pxeserver"

	desc """启动PXE服务"""

	rest {
		request {
			url "PUT /v1/baremetal/pxeservers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIStartBaremetalPxeServerMsg.class

			desc """启动PXE服务"""

			params {

				column {
					name "uuid"
					enclosedIn "startBaremetalPxeServer"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
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
			clz APIStartBaremetalPxeServerEvent.class
		}
	}
}