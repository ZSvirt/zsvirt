package org.zstack.network.service.flat

import org.zstack.network.service.flat.APIGetL3NetworkIpStatisticReply

doc {
	title "获取三层网络IP地址使用情况统计(GetL3NetworkIpStatistic)"

	category "三层网络"

	desc """获取三层网络IP地址使用情况统计"""

	rest {
		request {
			url "GET /v1/l3-networks/{l3NetworkUuid}/ip-statistic"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetL3NetworkIpStatisticMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "resourceType"
					enclosedIn ""
					desc "统计资源类型"
					location "query"
					type "String"
					optional true
					since "3.7.0"
					values ("All","Vip","VM","KernelInterface")
				}
				column {
					name "ip"
					enclosedIn ""
					desc "指定IP地址"
					location "query"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "sortBy"
					enclosedIn ""
					desc "排序方式"
					location "query"
					type "String"
					optional true
					since "3.7.0"
					values ("Ip","CreateDate")
				}
				column {
					name "sortDirection"
					enclosedIn ""
					desc "排序方向"
					location "query"
					type "String"
					optional true
					since "3.7.0"
					values ("asc","desc")
				}
				column {
					name "start"
					enclosedIn ""
					desc "统计结果起始位置"
					location "query"
					type "Integer"
					optional true
					since "3.7.0"
				}
				column {
					name "limit"
					enclosedIn ""
					desc "统计结果数量"
					location "query"
					type "Integer"
					optional true
					since "3.7.0"
				}
				column {
					name "replyWithCount"
					enclosedIn ""
					desc "同时返回统计结果总数"
					location "query"
					type "boolean"
					optional true
					since "3.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
			}
		}

		response {
			clz APIGetL3NetworkIpStatisticReply.class
		}
	}
}