package org.zstack.guesttools

import org.zstack.guesttools.APIUpdateGuestToolsStateReply

doc {
	title "UpdateGuestToolsState"

	category "guest.tools"

	desc """更新云主机GuestTools状态"""

	rest {
		request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/guesttools-state"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateGuestToolsStateMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "updateGuestToolsState"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "3.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.16"
				}
			}
		}

		response {
			clz APIUpdateGuestToolsStateReply.class
		}
	}
}