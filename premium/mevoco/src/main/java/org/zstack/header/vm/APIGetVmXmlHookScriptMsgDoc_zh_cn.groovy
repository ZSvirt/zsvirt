package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmXmlHookScriptReply

doc {
	title "GetVmXmlHookScript"

	category "mevoco"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/vm-instances/{vmInstanceUuid}/xml-hook-script"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmXmlHookScriptMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetVmXmlHookScriptReply.class
		}
	}
}