package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APIDeleteVmBackupEvent

doc {
	title "DeleteVmBackup"

	category "backup.volume"

	desc """删除虚拟机备份"""

	rest {
		request {
			url "DELETE /v1/vm-backups/{groupUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmBackupMsg.class

			desc """"""

			params {

				column {
					name "groupUuid"
					enclosedIn ""
					desc "备份组uuid"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "backupStorageUuids"
					enclosedIn ""
					desc "镜像服务器uuid列表"
					location "query"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.0.0"
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
			clz APIDeleteVmBackupEvent.class
		}
	}
}