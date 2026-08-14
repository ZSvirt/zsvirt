package org.zstack.header.host

import org.zstack.header.host.APIGetHostPhysicalMemoryFactsReply

doc {
	title "GetHostPhysicalMemoryFacts"

	category "host"

	desc """获取物理机内存卡信息"""

	rest {
		request {
			url "GET /v1/hosts/physical-memory-facts/{hostUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetHostPhysicalMemoryFactsMsg.class

			desc """"""

			params {

				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "3.12.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.12.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.12.0"
				}
			}
		}

		response {
			clz APIGetHostPhysicalMemoryFactsReply.class
		}
	}
}