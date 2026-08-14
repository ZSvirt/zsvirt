package org.zstack.header.vm

import org.zstack.header.vm.APISetVmUserDefinedXmlEvent

doc {
	title "SetVmUserDefinedXml"

	category "mevoco"

	desc """配置虚拟机自定义XML"""

	rest {
		request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmUserDefinedXmlMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "setVmUserDefinedXml"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "3.10"
				}
				column {
					name "xmlBase64"
					enclosedIn "setVmUserDefinedXml"
					desc "Base64编码后的XML"
					location "body"
					type "String"
					optional false
					since "3.10"
				}
				column {
					name "resourceUuid"
					enclosedIn "setVmUserDefinedXml"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.10"
				}
				column {
					name "tagUuids"
					enclosedIn "setVmUserDefinedXml"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.10"
				}
			}
		}

		response {
			clz APISetVmUserDefinedXmlEvent.class
		}
	}
}