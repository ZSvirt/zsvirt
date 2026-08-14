package org.zstack.billing.generator

import org.zstack.billing.generator.APIGenerateAccountBillingEvent

doc {
	title "GenerateAccountBilling"

	category "billing"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/billings/accounts/{accountUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGenerateAccountBillingMsg.class

			desc """"""

			params {

				column {
					name "accountUuid"
					enclosedIn "generateAccountBilling"
					desc "账户UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGenerateAccountBillingEvent.class
		}
	}
}