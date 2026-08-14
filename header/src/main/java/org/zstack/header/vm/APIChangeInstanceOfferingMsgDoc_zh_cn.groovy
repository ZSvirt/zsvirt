package org.zstack.header.vm

import org.zstack.header.vm.APIChangeInstanceOfferingEvent

doc {
	title "ChangeInstanceOffering"

	category "vmInstance"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeInstanceOfferingMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "changeInstanceOffering"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "instanceOfferingUuid"
					enclosedIn "changeInstanceOffering"
					desc "计算规格UUID"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIChangeInstanceOfferingEvent.class
		}
	}
}