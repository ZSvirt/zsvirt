package org.zstack.header.vm

import org.zstack.header.vm.APISetVmUsbRedirectEvent

doc {
	title "SetVmUsbRedirect"

	category "vmInstance"

	desc """设置云主机usb重定向开关"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmUsbRedirectMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmUsbRedirect"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "enable"
					enclosedIn "setVmUsbRedirect"
					desc "设置为true代表开关开启，设置为false代表开关关闭"
					location "body"
					type "boolean"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APISetVmUsbRedirectEvent.class
		}
	}
}