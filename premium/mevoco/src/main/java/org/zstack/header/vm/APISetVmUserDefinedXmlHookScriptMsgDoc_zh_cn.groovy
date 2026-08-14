package org.zstack.header.vm

import org.zstack.header.vm.APISetVmUserDefinedXmlHookScriptEvent

doc {
	title "SetVmUserDefinedXmlHookScript"

	category "mevoco"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmUserDefinedXmlHookScriptMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "setVmUserDefinedXmlHookScript"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "xmlHookScriptBase64"
					enclosedIn "setVmUserDefinedXmlHookScript"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "setVmUserDefinedXmlHookScript"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "setVmUserDefinedXmlHookScript"
					desc "标签UUID列表"
					location "body"
					type "List"
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
			}
		}

		response {
			clz APISetVmUserDefinedXmlHookScriptEvent.class
		}
	}
}