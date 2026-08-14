#!/bin/sh

base_dir=`dirname $0`
#target_dir=$base_dir/../core/src/main/java
#config_schema_pkg_name="org.zstack.core.config.schema"
#config_xsd_file=$base_dir/../core/src/main/resources/META-INF/config.xsd
#xjc -p $config_schema_pkg_name -d $target_dir $config_xsd_file

#target_dir=$base_dir/../portal/src/main/java
#config_schema_pkg_name="org.zstack.portal.apimediator.schema"
#config_xsd_file=$base_dir/../portal/src/main/resource/ServicePortalConfig.xsd
#xjc -p $config_schema_pkg_name -d $target_dir $config_xsd_file

#target_dir=$base_dir/../test/src/test/java/
#config_xsd_file=$base_dir/../test/src/test/resources/xsd/UnitTestSuite.xsd
#config_schema_pkg_name="org.zstack.test"
#xjc -p $config_schema_pkg_name -d $target_dir $config_xsd_file

target_dir=${base_dir}/../test/src/test/java/
config_xsd_file=${base_dir}/../test/src/test/resources/xsd/deployer/main.xsd
config_schema_pkg_name="org.zstack.test.deployer.schema"
xjc -no-header -readOnly -p ${config_schema_pkg_name} -d ${target_dir} ${config_xsd_file}

#target_dir=$base_dir/../identity/src/main/java
#config_schema_pkg_name="org.zstack.identity.schema"
#config_xsd_file=$base_dir/../identity/src/main/resources/policy.xsd
#xjc -p $config_schema_pkg_name -d $target_dir $config_xsd_file

#target_dir=$base_dir/../header/src/main/java/
#config_xsd_file=$base_dir/../search/src/main/resources/DIHConfig.xsd
#config_schema_pkg_name="org.zstack.header.search.dih.schema"
#xjc -p $config_schema_pkg_name -d $target_dir $config_xsd_file

#target_dir=$base_dir/../configuration/src/main/java/
#config_xsd_file=$base_dir/../configuration/src/main/resources/testlink.xsd
#config_schema_pkg_name="org.zstack.configuration.testlink.schema"
#xjc -p $config_schema_pkg_name -d $target_dir $config_xsd_file

#target_dir=$base_dir/../core/src/main/java
#config_schema_pkg_name="org.zstack.core.errorcode.schema"
#config_xsd_file=$base_dir/../core/src/main/resources/errorcode.xsd
#xjc -p $config_schema_pkg_name -d $target_dir $config_xsd_file
