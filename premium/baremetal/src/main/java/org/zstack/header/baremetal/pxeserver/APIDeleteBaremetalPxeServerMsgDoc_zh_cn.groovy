package org.zstack.header.baremetal.pxeserver

import org.zstack.header.baremetal.pxeserver.APIDeleteBaremetalPxeServerEvent

doc {
	title "DeleteBaremetalPxeServer"

	category "baremetal.pxeserver"

	desc """删除PXE服务"""

	rest {
		request {
			url "DELETE /v1/baremetal/pxeservers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteBaremetalPxeServerMsg.class

			desc """删除PXE服务"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.6.0"
				}
			}
		}

		response {
			clz APIDeleteBaremetalPxeServerEvent.class
		}
	}
}