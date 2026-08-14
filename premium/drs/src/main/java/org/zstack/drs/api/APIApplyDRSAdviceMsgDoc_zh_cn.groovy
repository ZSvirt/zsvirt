package org.zstack.drs.api

import org.zstack.drs.api.APIApplyDRSAdviceEvent

doc {
	title "ApplyDRSAdvice"

	category "drs"

	desc """应用DRS建议"""

	rest {
		request {
			url "PUT /v1/clusters/drs/advice/{adviceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIApplyDRSAdviceMsg.class

			desc """"""

			params {

				column {
					name "adviceUuid"
					enclosedIn "applyDRSAdvice"
					desc "DRS调度建议UUID"
					location "url"
					type "String"
					optional false
					since "4.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.0.0"
				}
			}
		}

		response {
			clz APIApplyDRSAdviceEvent.class
		}
	}
}