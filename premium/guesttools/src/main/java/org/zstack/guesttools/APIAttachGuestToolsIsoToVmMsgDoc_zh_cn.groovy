package org.zstack.guesttools

import org.zstack.guesttools.APIAttachGuestToolsIsoToVmEvent

doc {
	title "AttachGuestToolsIsoToVm"

	category "guest.tools"

	desc """为云主机挂载增强工具镜像"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachGuestToolsIsoToVmMsg.class

			desc """为云主机挂载增强工具镜像"""

			params {

				column {
					name "uuid"
					enclosedIn "attachGuestToolsIsoToVm"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
			}
		}

		response {
			clz APIAttachGuestToolsIsoToVmEvent.class
		}
	}
}