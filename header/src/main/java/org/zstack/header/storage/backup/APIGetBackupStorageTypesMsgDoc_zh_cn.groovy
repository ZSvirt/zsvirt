package org.zstack.header.storage.backup

import org.zstack.header.storage.backup.APIGetBackupStorageTypesReply

doc {
	title "获取镜像服务器类型列表(GetBackupStorageTypes)"

	category "storage.backup"

	desc """获取镜像服务器类型列表"""

	rest {
		request {
			url "GET /v1/backup-storage/types"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetBackupStorageTypesMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIGetBackupStorageTypesReply.class
		}
	}
}