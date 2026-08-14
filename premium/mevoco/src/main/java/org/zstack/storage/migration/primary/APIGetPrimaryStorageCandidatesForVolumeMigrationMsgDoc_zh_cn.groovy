package org.zstack.storage.migration.primary

import org.zstack.storage.migration.primary.APIGetPrimaryStorageCandidatesForVolumeMigrationReply

doc {
	title "GetPrimaryStorageCandidatesForVolumeMigration"

	category "mevoco"

	desc """获取云盘迁移的可选主存储列表"""

	rest {
		request {
			url "GET /v1/primary-storage/volumes/{volumeUuid}/migration-candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetPrimaryStorageCandidatesForVolumeMigrationMsg.class

			desc """获取云盘迁移的可选主存储列表"""

			params {

				column {
					name "volumeUuid"
					enclosedIn ""
					desc "云盘UUID"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.2"
				}
			}
		}

		response {
			clz APIGetPrimaryStorageCandidatesForVolumeMigrationReply.class
		}
	}
}