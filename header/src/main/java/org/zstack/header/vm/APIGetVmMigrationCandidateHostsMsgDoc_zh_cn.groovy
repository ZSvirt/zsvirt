package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmMigrationCandidateHostsReply

doc {
	title "获取可热迁移的物理机列表(GetVmMigrationCandidateHosts)"

	category "vmInstance"

	desc """获取一个云主机可以热迁移的物理机列表"""

	rest {
		request {
			url "GET /v1/vm-instances/{vmInstanceUuid}/migration-target-hosts"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmMigrationCandidateHostsMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
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
			clz APIGetVmMigrationCandidateHostsReply.class
		}
	}
}