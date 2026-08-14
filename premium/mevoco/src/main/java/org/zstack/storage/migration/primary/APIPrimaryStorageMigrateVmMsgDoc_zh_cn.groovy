package org.zstack.storage.migration.primary

import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmEvent

doc {
	title "PrimaryStorageMigrateVm"

	category "mevoco"

	desc """虚拟机跨主存储迁移"""

	rest {
		request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIPrimaryStorageMigrateVmMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "primaryStorageMigrateVm"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "dstPrimaryStorageUuid"
					enclosedIn "primaryStorageMigrateVm"
					desc "目标主存储UUID"
					location "body"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "withDataVolumes"
					enclosedIn "primaryStorageMigrateVm"
					desc "迁移包含云盘"
					location "body"
					type "boolean"
					optional true
					since "2.6.0"
				}
				column {
					name "withSnapshots"
					enclosedIn "primaryStorageMigrateVm"
					desc "迁移包含快照"
					location "body"
					type "boolean"
					optional true
					since "2.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.6.0"
				}
				column {
					name "dstHostUuid"
					enclosedIn "primaryStorageMigrateVm"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "strategy"
					enclosedIn "primaryStorageMigrateVm"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.14.34"
				}
				column {
					name "downTime"
					enclosedIn "primaryStorageMigrateVm"
					desc "迁移停机时间"
					location "body"
					type "Integer"
					optional true
					since "3.16.21"
				}
				column {
					name "bandwidth"
					enclosedIn "primaryStorageMigrateVm"
					desc "迁移存储总限速，单位MB/s"
					location "body"
					type "long"
					optional true
					since "3.17.11"
				}
			}
		}

		response {
			clz APIPrimaryStorageMigrateVmEvent.class
		}
	}
}