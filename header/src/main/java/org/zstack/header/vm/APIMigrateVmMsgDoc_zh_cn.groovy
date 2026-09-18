package org.zstack.header.vm

import org.zstack.header.vm.APIMigrateVmEvent

doc {
	title "热迁移云主机(MigrateVm)"

	category "vmInstance"

	desc """将云主机热迁移至另一个物理机"""

	rest {
		request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIMigrateVmMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "migrateVm"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv_0.6"
				}
				column {
					name "hostUuid"
					enclosedIn "migrateVm"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "zsv_0.6"
				}
				column {
					name "migrateFromDestination"
					enclosedIn "migrateVm"
					desc "从迁移目的物理机器发起迁移命令"
					location "body"
					type "Boolean"
					optional true
					since "zsv_2.3"
				}
				column {
					name "allowUnknown"
					enclosedIn "migrateVm"
					desc "允许未知状态的虚拟机"
					location "body"
					type "boolean"
					optional true
					since "zsv_3.6.0"
				}
				column {
					name "strategy"
					enclosedIn "migrateVm"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv_3.6.0"
					values ("auto-converge")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv_0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv_0.6"
				}
				column {
					name "downTime"
					enclosedIn "migrateVm"
					desc "热迁移物理机停机时间"
					location "body"
					type "Integer"
					optional true
					since "zsv_3.16.21"
				}
				column {
					name "enableMigrationTls"
					enclosedIn "migrateVm"
					desc "迁移过程加密"
					location "body"
					type "Boolean"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
			}
		}

		response {
			clz APIMigrateVmEvent.class
		}
	}
}