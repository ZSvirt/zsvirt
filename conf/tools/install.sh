#!/bin/bash
current_folder=`pwd`
relative_script_parent_path=`dirname $0`
if [ ${relative_script_parent_path:0:1} = "/" ]; then
    cwd=$relative_script_parent_path
else
    cwd=$current_folder/$relative_script_parent_path
fi

pypi_path=file://$cwd/../../../static/pypi/simple

usage() {
    echo "usage:$0 [zstack-cli|zstack-ctl|zstack-dashboard|zstack-ui]"
    exit 1
}

tool=$1
force=$2

if [ -z $tool ]; then
  usage
fi

install_pip() {
    pip --version | grep 7.0.3 >/dev/null || easy_install -i $pypi_path --upgrade pip
}

install_virtualenv() {
    virtualenv --version | grep 12.1.1 >/dev/null || pip install -i $pypi_path --ignore-installed virtualenv==12.1.1
}

cd $cwd

install_pip
install_virtualenv
cd /tmp

if [ $tool = 'zstack-cli' ]; then
    CLI_VIRENV_PATH=/var/lib/zstack/virtualenv/zstackcli
    [ ! -z $force ] && rm -rf $CLI_VIRENV_PATH
    if [ ! -d "$CLI_VIRENV_PATH" ]; then
        virtualenv $CLI_VIRENV_PATH --python=python2.7
        if [ $? -ne 0 ]; then
            rm -rf $CLI_VIRENV_PATH
            exit 1
        fi
    fi
    . $CLI_VIRENV_PATH/bin/activate
    cd $cwd
    pip install -i $pypi_path --trusted-host localhost --ignore-installed zstackcli-*.tar.gz apibinding-*.tar.gz
    if [ $? -ne 0 ]; then
        rm -rf $CLI_VIRENV_PATH
        exit 1
    fi
    pip show zstacklib
    if [ $? -ne 0 ]; then
        # fresh install zstacklib
        echo "Installing zstacklib..."
        pip install -i $pypi_path --trusted-host localhost --ignore-installed zstacklib-*.tar.gz
        if [ $? -ne 0 ]; then
            rm -rf $CLI_VIRENV_PATH
            exit 1
        fi
    else
        # upgrade zstacklib
        echo "Upgrading zstacklib..."
        pip install -U -i $pypi_path --trusted-host localhost zstacklib-*.tar.gz
        if [ $? -ne 0 ]; then
            rm -rf $CLI_VIRENV_PATH
            exit 1
        fi
    fi
    chmod +x /usr/bin/zstack-cli

elif [ $tool = 'zstack-ctl' ]; then
    CTL_VIRENV_PATH=/var/lib/zstack/virtualenv/zstackctl
    rm -rf $CTL_VIRENV_PATH && virtualenv $CTL_VIRENV_PATH --python=python2.7 || exit 1
    . $CTL_VIRENV_PATH/bin/activate
    cd $cwd
    TMPDIR=/usr/local/zstack/ pip install -i $pypi_path --trusted-host localhost --ignore-installed zstackctl-*.tar.gz || exit 1
    TMPDIR=/usr/local/zstack/ pip install -i $pypi_path --trusted-host localhost --ignore-installed pycrypto==2.6.1 || exit 1
    chmod +x /usr/bin/zstack-ctl
    python $CTL_VIRENV_PATH/lib/python2.7/site-packages/zstackctl/generate_zstackctl_bash_completion.py

elif [ $tool = 'zstack-sys' ]; then
    SYS_VIRENV_PATH=/var/lib/zstack/virtualenv/zstacksys
    NEED_INSTALL=false
    if [ -d $SYS_VIRENV_PATH ]; then
        . $SYS_VIRENV_PATH/bin/activate
        if ! ansible --version | grep -q 'core 2.11.12.3'; then
          deactivate
          NEED_INSTALL=true
        fi
    else
        NEED_INSTALL=true
    fi
    if $NEED_INSTALL; then
        rm -rf $SYS_VIRENV_PATH && virtualenv $SYS_VIRENV_PATH --python=python2.7 || exit 1
        . $SYS_VIRENV_PATH/bin/activate
        cd $cwd
        TMPDIR=/usr/local/zstack/ pip install -i $pypi_path --trusted-host localhost --ignore-installed setuptools==39.2.0 || exit 1
        TMPDIR=/usr/local/zstack/ pip install -i $pypi_path --trusted-host localhost --ignore-installed ansible==4.10.0 || exit 1

        cat > /usr/bin/ansible << EOF
#! /bin/sh
VIRTUAL_ENV=/var/lib/zstack/virtualenv/zstacksys
if [ ! -d $VIRTUAL_ENV ]; then
    echo "Need to install zstacksys before using it"
    exit 1
fi

LANG=en_US.UTF-8
LC_ALL=en_US.utf8
export LANG LC_ALL
. ${VIRTUAL_ENV}/bin/activate

ansible \$@
EOF
        chmod +x /usr/bin/ansible

        cat > /usr/bin/ansible-playbook << EOF
#! /bin/sh
VIRTUAL_ENV=/var/lib/zstack/virtualenv/zstacksys
if [ ! -d $VIRTUAL_ENV ]; then
    echo "Need to install zstacksys before using it"
    exit 1
fi

LANG=en_US.UTF-8
LC_ALL=en_US.utf8
export LANG LC_ALL
. ${VIRTUAL_ENV}/bin/activate

ansible-playbook \$@
EOF
        chmod +x /usr/bin/ansible-playbook
    fi

elif [ $tool = 'zstack-dashboard' ]; then
    UI_VIRENV_PATH=/var/lib/zstack/virtualenv/zstack-dashboard
    [ ! -z $force ] && rm -rf $UI_VIRENV_PATH
    if [ ! -d "$UI_VIRENV_PATH" ]; then
        virtualenv $UI_VIRENV_PATH --python=python2.7
        if [ $? -ne 0 ]; then
            rm -rf $UI_VIRENV_PATH
            exit 1
        fi
    fi
    . $UI_VIRENV_PATH/bin/activate
    cd $cwd
    pip show versiontools
    if [ $? -ne 0 ]; then
        # fresh install versiontools
        echo "Installing versiontools..."
        pip install -i $pypi_path --trusted-host localhost versiontools
        if [ $? -ne 0 ]; then
            rm -rf $UI_VIRENV_PATH
            exit 1
        fi
    else
        # upgrade versiontools
        echo "Upgrading versiontools..."
        pip install -U -i $pypi_path --trusted-host localhost versiontools
        if [ $? -ne 0 ]; then
            rm -rf $UI_VIRENV_PATH
            exit 1
        fi
    fi
    pip show zstack-dashboard
    if [ $? -ne 0 ]; then
        #fresh install zstack_dashboard
        echo "Installing zstack_dashboard..."
        pip install -i $pypi_path --trusted-host localhost --upgrade zstack_dashboard-*.tar.gz
        if [ $? -ne 0 ]; then
            rm -rf $UI_VIRENV_PATH
            exit 1
        fi
    else
        #upgrae zstack_dashboard
        echo "Upgrading zstack_dashboard..."
        pip install -U -i $pypi_path --trusted-host localhost --upgrade zstack_dashboard-*.tar.gz
        if [ $? -ne 0 ]; then
            rm -rf $UI_VIRENV_PATH
            exit 1
        fi
    fi

    chmod +x /etc/init.d/zstack-dashboard

elif [ x"$tool" = x"zstack-ui" ]; then
    cd "$cwd"
    default_zstack_home='/usr/local/zstack/'
    default_ui_home="$default_zstack_home"/zstack-ui/
    zstack_home=$(echo ~zstack)
    zstack_home=${zstack_home%/}/
    ui_home="$zstack_home"/zstack-ui/
    mkdir -p "$ui_home"

    # Assume:
    # - zstack installed in /usr/local/zstacktest
    # - zstack-ui installed in /usr/local/zstack/zstack-ui
    # After upgrade, zstack-ui will be installed in /usr/local/zstacktest/zstack-ui.
    # We need to copy old ui config and certification files back
    if [ ! -f "$ui_home"/zstack-ui.war -a -f "$default_ui_home"/zstack-ui.war ]; then
      cp -rf "$default_ui_home"/* "$ui_home"
      sed -i "s|$default_zstack_home|$zstack_home|g" "$ui_home"/zstack.ui.properties
    fi

    cp -f zstack-ui.war "$ui_home"
    rm -rf "$ui_home"/tmp
    unzip zstack-ui.war -d "$ui_home"/tmp
    cp -f zstack-ui /etc/init.d/
    chmod a+x /etc/init.d/zstack-ui

    chown -R zstack:zstack "$ui_home"
else
    usage
fi

