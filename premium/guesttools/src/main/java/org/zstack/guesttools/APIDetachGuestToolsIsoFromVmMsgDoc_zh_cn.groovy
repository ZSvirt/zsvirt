package org.zstack.guesttools

import org.zstack.guesttools.APIDetachGuestToolsIsoFromVmEvent

doc {
	title "DetachGuestToolsIsoFromVm"

	category "guest.tools"

	desc """卸载虚拟机增强工具镜像的返回"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachGuestToolsIsoFromVmMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "detachGuestToolsIsoFromVm"
					desc "虚拟机的 UUID"
					location "url"
					type "String"
					optional false
					since "4.10.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.16"
				}
			}
		}

		response {
			clz APIDetachGuestToolsIsoFromVmEvent.class
		}
	}
}