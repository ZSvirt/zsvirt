package org.zstack.header.storage.backup

import org.zstack.header.storage.backup.APIChangeBackupStorageStateEvent

doc {
	title "更改镜像服务器可用状态(ChangeBackupStorageState)"

	category "storage.backup"

	desc """更改镜像服务器的可用状态"""

	rest {
		request {
			url "PUT /v1/backup-storage/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeBackupStorageStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeBackupStorageState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeBackupStorageState"
					desc "镜像服务器的目标状态"
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
			clz APIChangeBackupStorageStateEvent.class
		}
	}
}