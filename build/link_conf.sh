#!/bin/bash
src_dir=`pwd`
src_dir=`dirname $src_dir`
if [ $# -eq 1 ]; then
    src_dir=$1
fi

echo_fail(){
    echo -e "$(tput setaf 1) $1\n$(tput sgr0)"
    exit 1
}


if [ ! -d $src_dir ]; then
    echo_fail "$src_dir is not a valid folder"
fi

conf_dir=$src_dir/conf
t_conf_dir=$src_dir/test/src/test/resources/

if [ ! -d $conf_dir/globalConfig ]; then
    echo_fail "did not find source $conf_dir/globalConfig"
fi

if [ ! -d $conf_dir/springConfigXml ]; then
    echo_fail "did not find source $conf_dir/springConfigXml"
fi

if [ ! -d $t_conf_dir/globalConfig ]; then
    echo_fail "did not find target $t_conf_dir/globalConfig"
fi

if [ ! -d $t_conf_dir/springConfigXml ]; then
    echo_fail "did not find target $t_conf_dir/springConfigXml"
fi

src_relative_folder=../../../../../conf

for file in `ls $conf_dir/globalConfig`;do
    cd $t_conf_dir/globalConfig
    /bin/rm -rf $file
    ln -s ${src_relative_folder}/globalConfig/$file $file
done

#for file in `ls $conf_dir/springConfigXml`;do
#    cd $t_conf_dir/springConfigXml
#    /bin/rm -rf $file
#    ln -s ${src_relative_folder}/springConfigXml/$file $file
#done
