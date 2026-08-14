package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIReconnectPrimaryStorageEvent

doc {
	title "重连主存储(ReconnectPrimaryStorage)"

	category "storage.primary"

	desc """重连主存储"""

	rest {
		request {
			url "PUT /v1/primary-storage/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIReconnectPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "reconnectPrimaryStorage"
					desc "主存储的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
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
			clz APIReconnectPrimaryStorageEvent.class
		}
	}
}