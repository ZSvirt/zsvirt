package org.zstack.header.bonding

import org.zstack.header.bonding.APIDeleteBondingEvent

doc {
	title "DeleteBonding"

	category "bonding"

	desc """删除Bond网口"""

	rest {
		request {
			url "DELETE /v1/hosts/bondings/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteBondingMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.17.0"
				}
			}
		}

		response {
			clz APIDeleteBondingEvent.class
		}
	}
}