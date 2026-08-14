package org.zstack.billing

import org.zstack.billing.APIDeleteBillingEvent

doc {
	title "DeleteBilling"

	category "billing"

	desc """删除计费数据"""

	rest {
		request {
			url "DELETE /v1/billings/billings"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteBillingMsg.class

			desc """"""

			params {

				column {
					name "accountUuid"
					enclosedIn ""
					desc "账户UUID"
					location "query"
					type "String"
					optional true
					since "3.10"
				}
				column {
					name "startTime"
					enclosedIn ""
					desc "删除在此之后产生的计费数据。内容为时间戳，毫秒"
					location "query"
					type "Long"
					optional true
					since "3.10"
				}
				column {
					name "endTime"
					enclosedIn ""
					desc "删除在此之前产生的计费数据。内容为时间戳，毫秒"
					location "query"
					type "Long"
					optional true
					since "3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeleteBillingEvent.class
		}
	}
}