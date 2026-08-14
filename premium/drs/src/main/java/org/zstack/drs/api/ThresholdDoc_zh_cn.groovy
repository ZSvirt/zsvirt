package org.zstack.drs.api



doc {

	title "集群DRS的阈值配置"

	field {
		name "thresholdName"
		desc "阈值配置项目"
		type "String"
		since "4.0.0"
	}
	field {
		name "thresholdValue"
		desc "阈值"
		type "String"
		since "4.0.0"
	}
	field {
		name "operator"
		desc "运算符，大于阈值触发还是小于阈值触发"
		type "String"
		since "4.0.0"
	}
}
