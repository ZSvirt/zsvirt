package org.zstack.header.vm

import org.zstack.header.vm.APISetVmSecurityLevelEvent

doc {
	title "SetVmSecurityLevel"

	category "mevoco"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmSecurityLevelMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmSecurityLevel"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "securityLevel"
					enclosedIn "setVmSecurityLevel"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APISetVmSecurityLevelEvent.class
		}
	}
}