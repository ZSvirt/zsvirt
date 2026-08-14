package org.zstack.network.service.lb

import org.zstack.network.service.lb.LoadBalancerListenerCertificateRefInventory
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "证书清单"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.3"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.3"
	}
	field {
		name "certificate"
		desc "证书内容"
		type "String"
		since "2.3"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.3"
	}
	ref {
		name "listeners"
		path "org.zstack.network.service.lb.CertificateInventory.listeners"
		desc "使用该证书的负载均衡监听器列表"
		type "List"
		since "2.3"
		clz LoadBalancerListenerCertificateRefInventory.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.3"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.3"
	}
}
