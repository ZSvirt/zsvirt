package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIGetAuditDataReply

doc {
	title "GetAuditData"

	category "zwatch"

	desc """获取审计数据"""

	rest {
		request {
			url "GET /v1/zwatch/audits"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetAuditDataMsg.class

			desc """"""

			params {

				column {
					name "startTime"
					enclosedIn ""
					desc "起始时间"
					location "query"
					type "Long"
					optional true
					since "2.3"
				}
				column {
					name "endTime"
					enclosedIn ""
					desc "结束时间"
					location "query"
					type "Long"
					optional true
					since "2.3"
				}
				column {
					name "limit"
					enclosedIn ""
					desc "最大返回条数"
					location "query"
					type "Integer"
					optional true
					since "2.3"
				}
				column {
					name "labels"
					enclosedIn ""
					desc "过滤标签列表"
					location "query"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "auditType"
					enclosedIn ""
					desc "审计类型，默认为Resource"
					location "query"
					type "AuditType"
					optional true
					since "3.5.0"
					values ("Login","Resource")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.3"
				}
			}
		}

		response {
			clz APIGetAuditDataReply.class
		}
	}
}