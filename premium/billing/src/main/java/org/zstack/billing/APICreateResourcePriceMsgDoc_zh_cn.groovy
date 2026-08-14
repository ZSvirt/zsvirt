package org.zstack.billing

import org.zstack.billing.APICreateResourcePriceEvent

doc {
	title "CreateResourcePrice"

	category "billing"

	desc """创建资源价格"""

	rest {
		request {
			url "POST /v1/billings/prices"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateResourcePriceMsg.class

			desc """创建资源价格"""

			params {

				column {
					name "resourceName"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("cpu","memory","rootVolume","dataVolume","snapShot","gpu","pubIpVmNicBandwidthOut","pubIpVmNicBandwidthIn","pubIpVipBandwidthOut","pubIpVipBandwidthIn")
				}
				column {
					name "resourceUnit"
					enclosedIn "params"
					desc "资源计费单元"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "timeUnit"
					enclosedIn "params"
					desc "计费时间单元"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "price"
					enclosedIn "params"
					desc "单位价格"
					location "body"
					type "double"
					optional false
					since "0.6"
				}
				column {
					name "accountUuid"
					enclosedIn "params"
					desc "账户UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "dateInLong"
					enclosedIn "params"
					desc "长整型时刻"
					location "body"
					type "Long"
					optional true
					since "0.6"
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
					name "tableUuid"
					enclosedIn "params"
					desc "价目表UUID"
					location "body"
					type "String"
					optional true
					since "3.7"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateResourcePriceEvent.class
		}
	}
}