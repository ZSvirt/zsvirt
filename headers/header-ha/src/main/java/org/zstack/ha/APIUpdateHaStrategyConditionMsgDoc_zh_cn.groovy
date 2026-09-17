package org.zstack.ha

import org.zstack.ha.APIUpdateHaStrategyConditionEvent

doc {
	title "UpdateHaStrategyCondition"

	category "ha"

	desc """更新Ha策略"""

	rest {
		request {
			url "PUT /v1/ha-strategy-condition/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateHaStrategyConditionMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateHaStrategyCondition"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "name"
					enclosedIn "updateHaStrategyCondition"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.17.0"
				}
				column {
					name "state"
					enclosedIn "updateHaStrategyCondition"
					desc "状态"
					location "body"
					type "String"
					optional true
					since "3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
			}
		}

		response {
			clz APIUpdateHaStrategyConditionEvent.class
		}
	}
}