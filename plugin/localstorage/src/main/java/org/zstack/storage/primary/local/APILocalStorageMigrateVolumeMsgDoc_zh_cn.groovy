package org.zstack.storage.primary.local

import org.zstack.storage.primary.local.APILocalStorageMigrateVolumeEvent

doc {
	title "迁移本地存储上存放的云盘(LocalStorageMigrateVolume)"

	category "storage.primary"

	desc """迁移本地存储上存放的云盘"""

	rest {
		request {
			url "PUT /v1/primary-storage/local-storage/volumes/{volumeUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APILocalStorageMigrateVolumeMsg.class

			desc """"""

			params {

				column {
					name "volumeUuid"
					enclosedIn "localStorageMigrateVolume"
					desc "云盘UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "destHostUuid"
					enclosedIn "localStorageMigrateVolume"
					desc "目标主机UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APILocalStorageMigrateVolumeEvent.class
		}
	}
}