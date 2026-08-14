package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmXmlReply

doc {
	title "GetVmXml"

	category "mevoco"

	desc """获取VM的XML"""

	rest {
		request {
			url "GET /v1/vm-instances/{vmInstanceUuid}/xml"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmXmlMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
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
			}
		}

		response {
			clz APIGetVmXmlReply.class
		}
	}
}