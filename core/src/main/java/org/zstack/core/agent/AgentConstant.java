package org.zstack.core.agent;

import org.zstack.core.ansible.AnsibleConstant;
import org.zstack.utils.path.PathUtil;

/**
 * Created by frank on 12/5/2015.
 */
public class AgentConstant {
    public static final String SERVICE_ID = "agent";

    public static final String ANSIBLE_PLAYBOOK_NAME = "agent.yaml";
    public static final String ANSIBLE_MODULE_PATH = "ansible/zstack-agent";

    public static final String SRC_ANSIBLE_ROOT = PathUtil.join(AnsibleConstant.ROOT_DIR, "zstack-agent");
    public static final String DST_ANSIBLE_ROOT = "/var/lib/zstack/ansible/zstack-agent";

    public static final int AGENT_PORT = 10001;

    public static final String CONFIG_COMMAND_URL = "commandUrl";

    public static final String CANCEL_JOB = "/job/cancel";

}
