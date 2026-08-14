package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APIGetCandidateHostKernelInterfacesReply

doc {
	title "GetCandidateHostKernelInterfaces"

	category "network.l2"

	desc """获取候选的主机Kernel适配器"""

	rest {
		request {
			url "GET /v1/hosts/kernel-interfaces"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateHostKernelInterfacesMsg.class

			desc """"""

			params {

				column {
					name "hostUuids"
					enclosedIn ""
					desc "主机UUID列表"
					location "query"
					type "List"
					optional false
					since "4.10.20"
				}
				column {
					name "cidr"
					enclosedIn ""
					desc "无类别域间路由"
					location "query"
					type "String"
					optional true
					since "4.10.20"
				}
				column {
					name "trafficTypes"
					enclosedIn ""
					desc "流量类型"
					location "query"
					type "List"
					optional true
					since "4.10.20"
					values ("Management","Storage")
				}
				column {
					name "containsRejected"
					enclosedIn ""
					desc "是否包含被拒绝的候选项"
					location "query"
					type "boolean"
					optional true
					since "4.10.20"
				}
				column {
					name "limit"
					enclosedIn ""
					desc "最多返回的记录数，类似MySQL的limit，默认值1000"
					location "query"
					type "Integer"
					optional true
					since "4.10.20"
				}
				column {
					name "start"
					enclosedIn ""
					desc "起始查询记录位置，类似MySQL的offset。跟`limit`配合使用可以实现分页"
					location "query"
					type "Integer"
					optional true
					since "4.10.20"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.10.20"
				}
			}
		}

		response {
			clz APIGetCandidateHostKernelInterfacesReply.class
		}
	}
}