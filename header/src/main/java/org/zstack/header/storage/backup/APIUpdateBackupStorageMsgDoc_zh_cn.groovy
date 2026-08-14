package org.zstack.header.storage.backup

import org.zstack.header.storage.backup.APIUpdateBackupStorageEvent

doc {
	title "更新镜像服务器信息(UpdateBackupStorage)"

	category "storage.backup"

	desc """更新镜像服务器信息"""

	rest {
		request {
			url "PUT /v1/backup-storage/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateBackupStorage"
					desc "指定目标镜像服务器的UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updateBackupStorage"
					desc "镜像服务器的新名称"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updateBackupStorage"
					desc "镜像服务器新的详细描述"
					location "body"
					type "String"
					optional true
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
			clz APIUpdateBackupStorageEvent.class
		}
	}
}