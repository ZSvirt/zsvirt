package org.zstack.billing

import org.zstack.billing.APICalculateAccountBillingSpendingReply

doc {
	title "CalculateAccountBillingSpending"

	category "billing"

	desc """查询账户花费"""

	rest {
		request {
			url "PUT /v1/billings/accounts/{accountUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICalculateAccountBillingSpendingMsg.class

			desc """"""

			params {

				column {
					name "accountUuid"
					enclosedIn "calculateAccountBillingSpending"
					desc "账户UUID"
					location "url"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "dateStart"
					enclosedIn "calculateAccountBillingSpending"
					desc "起始日期"
					location "body"
					type "Long"
					optional true
					since "3.7.0"
				}
				column {
					name "dateEnd"
					enclosedIn "calculateAccountBillingSpending"
					desc "结束日期"
					location "body"
					type "Long"
					optional true
					since "3.7.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "calculateAccountBillingSpending"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.7.1"
				}
				column {
					name "simple"
					enclosedIn "calculateAccountBillingSpending"
					desc "不输出详情"
					location "body"
					type "boolean"
					optional true
					since "3.7.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
			}
		}

		response {
			clz APICalculateAccountBillingSpendingReply.class
		}
	}
}