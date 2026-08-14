package org.zstack.header.allocator

import org.zstack.header.allocator.APIGetCpuMemoryCapacityReply

doc {
	title "GetCpuMemoryCapacity"

	category "分配器"

	desc """获取cpu和内存容量"""

	rest {
		request {
			url "GET /v1/hosts/capacities/cpu-memory"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCpuMemoryCapacityMsg.class

			desc """"""

			params {

				column {
					name "zoneUuids"
					enclosedIn ""
					desc "区域的uuid"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "clusterUuids"
					enclosedIn ""
					desc "集群的UUID。用于挂载网络、存储等"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "hostUuids"
					enclosedIn ""
					desc "物理机的UUID。用于添加、删除host等"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "all"
					enclosedIn ""
					desc ""
					location "query"
					type "boolean"
					optional true
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
				column {
					name "hypervisorType"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
					values ("KVM","ESX")
				}
			}
		}

		response {
			clz APIGetCpuMemoryCapacityReply.class
		}
	}
}