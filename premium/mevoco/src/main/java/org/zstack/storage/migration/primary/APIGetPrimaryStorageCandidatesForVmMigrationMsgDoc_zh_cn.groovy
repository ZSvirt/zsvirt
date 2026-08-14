package org.zstack.storage.migration.primary

import org.zstack.storage.migration.primary.APIGetPrimaryStorageCandidatesForVmMigrationReply

doc {
	title "GetPrimaryStorageCandidatesForVmMigration"

	category "mevoco"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/vm-instances/{vmInstanceUuid}/storage-migration-candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetPrimaryStorageCandidatesForVmMigrationMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "withDataVolumes"
					enclosedIn ""
					desc ""
					location "query"
					type "boolean"
					optional true
					since "3.7"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "migrateStorageOnly"
					enclosedIn ""
					desc ""
					location "query"
					type "boolean"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetPrimaryStorageCandidatesForVmMigrationReply.class
		}
	}
}