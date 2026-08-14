package org.zstack.header.configuration

import org.zstack.header.configuration.APIChangeDiskOfferingStateEvent

doc {
	title "ChangeDiskOfferingState"

	category "configuration"

	desc """更改云盘规格的启用状态"""

	rest {
		request {
			url "PUT /v1/disk-offerings/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeDiskOfferingStateMsg.class

			desc """更改云盘规格的启用状态"""

			params {

				column {
					name "uuid"
					enclosedIn "changeDiskOfferingState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeDiskOfferingState"
					desc "状态事件"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					values ("enable","disable")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIChangeDiskOfferingStateEvent.class
		}
	}
}