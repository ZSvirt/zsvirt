package org.zstack.billing

import org.zstack.billing.APICalculateResourceSpendingReply

doc {
	title "CalculateResourceSpending"

	category "billing"

	desc """计算资源类型总花费"""

	rest {
		request {
			url "PUT /v1/billings/resources/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICalculateResourceSpendingMsg.class

			desc """计算资源类型总花费"""

			params {

				column {
					name "resourceType"
					enclosedIn "calculateResourceSpending"
					desc "资源类型"
					location "body"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "calculateResourceSpending"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "startTime"
					enclosedIn "CalculateResourceSpending"
					desc "起始日期"
					location "body"
					type "Long"
					optional true
					since "3.4.0"
				}
				column {
					name "endTime"
					enclosedIn "CalculateResourceSpending"
					desc "结束日期"
					location "body"
					type "Long"
					optional true
					since "3.4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "dateStart"
					enclosedIn "calculateResourceSpending"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "dateEnd"
					enclosedIn "calculateResourceSpending"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "start"
					enclosedIn "calculateResourceSpending"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "limit"
					enclosedIn "calculateResourceSpending"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APICalculateResourceSpendingReply.class
		}
	}
}