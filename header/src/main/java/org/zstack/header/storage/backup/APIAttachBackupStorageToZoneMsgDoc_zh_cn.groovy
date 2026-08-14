package org.zstack.header.storage.backup

import org.zstack.header.storage.backup.APIAttachBackupStorageToZoneEvent

doc {
	title "挂载镜像服务器至区域(AttachBackupStorageToZone)"

	category "storage.backup"

	desc """挂载镜像服务器至区域"""

	rest {
		request {
			url "POST /v1/zones/{zoneUuid}/backup-storage/{backupStorageUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachBackupStorageToZoneMsg.class

			desc """"""

			params {

				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "backupStorageUuid"
					enclosedIn "params"
					desc "镜像存储UUID"
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
			clz APIAttachBackupStorageToZoneEvent.class
		}
	}
}