package org.zstack.storage.migration.primary

import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVolumeEvent

doc {
	title "PrimaryStorageMigrateVolume"

	category "mevoco"

	desc """跨存储迁移云盘"""

	rest {
		request {
			url "PUT /v1/primary-storage/volumes/{volumeUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIPrimaryStorageMigrateVolumeMsg.class

			desc """跨存储迁移云盘"""

			params {

				column {
					name "volumeUuid"
					enclosedIn "primaryStorageMigrateVolume"
					desc "云盘UUID"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "dstPrimaryStorageUuid"
					enclosedIn "primaryStorageMigrateVolume"
					desc "目标主存储UUID"
					location "body"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.2"
				}
			}
		}

		response {
			clz APIPrimaryStorageMigrateVolumeEvent.class
		}
	}
}