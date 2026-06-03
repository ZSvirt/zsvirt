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
					desc "虚拟机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 2.6.0"
				}
				column {
					name "dstPrimaryStorageUuid"
					enclosedIn "primaryStorageMigrateVm"
					desc "目标主存储UUID"
					location "body"
					type "String"
					optional true
					since "zsv 2.6.0"
				}
				column {
					name "withDataVolumes"
					enclosedIn "primaryStorageMigrateVm"
					desc "迁移包含硬盘（已废弃，请使用 volumeMigrationAOs 指定要迁移的云盘）"
					location "body"
					type "boolean"
					optional true
					since "zsv 2.6.0"
				}
				column {
					name "withSnapshots"
					enclosedIn "primaryStorageMigrateVm"
					desc "迁移包含快照（已废弃，请使用 volumeMigrationAOs 中每项的 withSnapshots 按云盘指定）"
					location "body"
					type "boolean"
					optional true
					since "zsv 2.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 2.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 2.6.0"
				}
				column {
					name "dstHostUuid"
					enclosedIn "primaryStorageMigrateVm"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "strategy"
					enclosedIn "primaryStorageMigrateVm"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.14.34"
				}
				column {
					name "downTime"
					enclosedIn "primaryStorageMigrateVm"
					desc "迁移停机时间"
					location "body"
					type "Integer"
					optional true
					since "zsv 3.16.21"
				}
				column {
					name "bandwidth"
					enclosedIn "primaryStorageMigrateVm"
					desc "迁移存储总限速，单位MB/s"
					location "body"
					type "long"
					optional true
					since "zsv 3.17.11"
				}
				column {
					name "volumeMigrationAOs"
					enclosedIn "primaryStorageMigrateVm"
					desc "按云盘指定目标主存储的迁移规格列表，每项包含 volumeUuid、dstPrimaryStorageUuid 与 withSnapshots"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv 5.1.0"
				}
				column {
					name "enableMigrationTls"
					enclosedIn "primaryStorageMigrateVm"
					desc ""
					location "body"
					type "Boolean"
					optional true
					since "1.0.1, zsv 5.1.0"
				}
			}
		}

		response {
			clz APIPrimaryStorageMigrateVmEvent.class
		}
	}
}