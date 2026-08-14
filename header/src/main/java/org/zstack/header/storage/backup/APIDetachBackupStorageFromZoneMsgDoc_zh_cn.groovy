package org.zstack.header.storage.backup

import org.zstack.header.storage.backup.APIDetachBackupStorageFromZoneEvent

doc {
	title "从区域中卸载已经挂载的镜像服务器(DetachBackupStorageFromZone)"

	category "storage.backup"

	desc """从区域中卸载已经挂载的镜像服务器"""

	rest {
		request {
			url "DELETE /v1/zones/{zoneUuid}/backup-storage/{backupStorageUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachBackupStorageFromZoneMsg.class

			desc """"""

			params {

				column {
					name "backupStorageUuid"
					enclosedIn ""
					desc "镜像存储UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "zoneUuid"
					enclosedIn ""
					desc "区域UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
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
			clz APIDetachBackupStorageFromZoneEvent.class
		}
	}
}