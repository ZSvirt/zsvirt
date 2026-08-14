package org.zstack.billing

import org.zstack.billing.APICalculateAccountSpendingReply

doc {
	title "CalculateAccountSpending"

	category "billing"

	desc """计算账户花费"""

	rest {
		request {
			url "PUT /v1/billings/accounts/{accountUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICalculateAccountSpendingMsg.class

			desc """计算账户花费"""

			params {

				column {
					name "accountUuid"
					enclosedIn "calculateAccountSpending"
					desc "账户UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "dateStart"
					enclosedIn "calculateAccountSpending"
					desc "起始日期"
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "dateEnd"
					enclosedIn "calculateAccountSpending"
					desc "结束日期"
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "simple"
					enclosedIn "calculateAccountSpending"
					desc "不输出详情"
					location "body"
					type "boolean"
					optional true
					since "3.7.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "hypervisorType"
					enclosedIn "calculateAccountSpending"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("KVM","Simulator","ESX","xdragon")
				}
			}
		}

		response {
			clz APICalculateAccountSpendingReply.class
		}
	}
}