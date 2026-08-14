package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APIDeleteVolumeBackupEvent

doc {
	title "DeleteVolumeBackup"

	category "backup.volume"

	desc """删除卷备份"""

	rest {
		request {
			url "DELETE /v1/volume-backups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVolumeBackupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6"
				}
				column {
					name "backupStorageUuids"
					enclosedIn ""
					desc "镜像服务器uuid列表"
					location "query"
					type "List"
					optional true
					since "2.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式 "
					location "query"
					type "String"
					optional true
					since "2.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.6"
				}
				column {
					name "handleDependency"
					enclosedIn ""
					desc "数据库删除策略考虑依赖关系"
					location "query"
					type "boolean"
					optional true
					since "3.17.11"
				}
			}
		}

		response {
			clz APIDeleteVolumeBackupEvent.class
		}
	}
}