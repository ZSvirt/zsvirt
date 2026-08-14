package org.zstack.guesttools.advanced

import org.zstack.guesttools.advanced.APIDeleteVmCustomSpecificationEvent

doc {
	title "DeleteVmCustomSpecification"

	category "guest.tools"

	desc """删除虚拟机自定义操作系统规范"""

	rest {
		request {
			url "DELETE /v1/vm-custom-specifications/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmCustomSpecificationMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.10.18"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.10.18"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.10.18"
				}
			}
		}

		response {
			clz APIDeleteVmCustomSpecificationEvent.class
		}
	}
}