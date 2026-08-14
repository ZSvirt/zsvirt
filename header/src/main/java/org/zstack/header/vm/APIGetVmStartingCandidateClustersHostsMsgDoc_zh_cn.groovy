package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmStartingCandidateClustersHostsReply

doc {
	title "获取云主机可启动目的地列表(GetVmStartingCandidateClustersHosts)"

	category "vmInstance"

	desc """获取一个停止的云主机可以启动的集群、物理机列表。用户可以用该API判断一个停止可以在哪些集群、物理机上启动。"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/starting-target-hosts"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmStartingCandidateClustersHostsMsg.class

			desc """"""

			params {

				column {
					name "uuid"
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
			clz APIGetVmStartingCandidateClustersHostsReply.class
		}
	}
}