#!/bin/sh
## pip install codemod

cd ..

mv conf/preconfigurationTemplates/zstack_expert_x86_64_v2.cfg conf/preconfigurationTemplates/cloud_expert_x86_64_v2.cfg
mv conf/preconfigurationTemplates/zstack_host_x86_64_v2.cfg conf/preconfigurationTemplates/cloud_host_x86_64_v2.cfg

mv conf/cloudFormationTemplates/ZStack.System.v1.VlanVPC.json conf/cloudFormationTemplates/Cloud.System.v1.VlanVPC.json
mv conf/cloudFormationTemplates/ZStack.System.v1.VxlanVPC.json conf/cloudFormationTemplates/Cloud.System.v1.VxlanVPC.json
mv conf/cloudFormationTemplates/ZStack.System.v2.LNMP.json conf/cloudFormationTemplates/Cloud.System.v2.LNMP.json
mv conf/cloudFormationTemplates/ZStack.System.v2.MysqlHA.json conf/cloudFormationTemplates/Cloud.System.v2.MysqlHA.json
mv conf/cloudFormationTemplates/ZStack.System.v2.Tomcat.json conf/cloudFormationTemplates/Cloud.System.v2.Tomcat.json
mv conf/cloudFormationTemplates/ZStack.System.v3.EIP.json conf/cloudFormationTemplates/Cloud.System.v3.EIP.json
mv conf/cloudFormationTemplates/ZStack.System.v3.LB.json conf/cloudFormationTemplates/Cloud.System.v3.LB.json
mv conf/cloudFormationTemplates/ZStack.System.v3.PF.json conf/cloudFormationTemplates/Cloud.System.v3.PF.json
mv conf/cloudFormationTemplates/ZStack.System.v3.SG.json conf/cloudFormationTemplates/Cloud.System.v3.SG.json

sed -i "s/ZStack::/Cloud::/g" conf/cloudFormationTemplates/*

mv sdk/src/main/java/org/zstack/ sdk/src/main/java/org/cloud/
codemod -m -d sdk/src/main/* --extensions java,groovy org.zstack.heder org.cloud.heder --accept-all
codemod -m -d sdk/src/main/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all

codemod -m -d test/src/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all
codemod -m -d tests/testlib-simple/src/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all
codemod -m -d tests/testlib-simple/src/* --extensions java,groovy org.zstack.heder.storage.volume.backup org.cloud.heder.storage.volume.backup --accept-all

codemod -m -d test-premium/src/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all
codemod -m -d test-premium/src/* --extensions java,groovy org.zstack.heder.storage.volume.backup org.cloud.heder.storage.volume.backup --accept-all
codemod -m -d tests/testlib-premium/src/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all


codemod -m -d mevoco/src/* --extensions java org.zstack.sdk org.cloud.sdk --accept-all
codemod -m -d cloudformation/src/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all
codemod -m -d plugin-premium/externalapiadapter/src/* --extensions java,groovy org.zstack.sdk org.cloud.sdk --accept-all


## cd premium/conf/
## codemod -m -d cloudFormationTemplates/* --extensions json Zstack Cloud --accept-all

## modify mydql contain zstack

## mysql -uroot -p??? <<EOF
## use zstack;
## update StackTemplateVO set name = replace(name, 'ZStack', 'Cloud');
## update StackTemplateVO set content = replace(content, 'ZStack', 'Cloud');

## update PreconfigurationTemplateVO set name = replace(name, 'zstack', 'cloud');
## update PreconfigurationTemplateVO set description = replace(name, 'zstack', 'cloud');
## update PreconfigurationTemplateVO set distribution = replace(name, 'zstack', 'cloud');

## EOF