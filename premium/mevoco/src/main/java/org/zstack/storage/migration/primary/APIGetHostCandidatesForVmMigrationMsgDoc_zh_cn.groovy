package org.zstack.storage.migration.primary

import org.zstack.storage.migration.primary.APIGetHostCandidatesForVmMigrationReply

doc {
	title "GetHostCandidatesForVmMigration"

	category "mevoco"

	desc """获取跨存储迁移可选物理机列表"""

	rest {
		request {
			url "GET /v1/primary-storage/hosts/{vmInstanceUuid}/migration-candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetHostCandidatesForVmMigrationMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "dstPrimaryStorageUuid"
					enclosedIn ""
					desc "目的主存储UUID"
					location "query"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "limit"
					enclosedIn ""
					desc "获取物理机列表数量"
					location "query"
					type "Integer"
					optional true
					since "3.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.10.0"
				}
			}
		}

		response {
			clz APIGetHostCandidatesForVmMigrationReply.class
		}
	}
}