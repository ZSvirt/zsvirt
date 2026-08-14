package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmUsbRedirectReply

doc {
	title "GetVmUsbRedirect"

	category "vmInstance"

	desc """获取云主机usb重定向开关状态"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/usbredirect"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmUsbRedirectMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetVmUsbRedirectReply.class
		}
	}
}