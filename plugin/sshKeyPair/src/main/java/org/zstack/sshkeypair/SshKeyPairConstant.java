package org.zstack.sshkeypair;

public interface SshKeyPairConstant {
    String OPERATE_SSH_KEY_PAIR_THREAD_NAME = "create-update-delete-attach-detach-ssh-key";

    String SSH_KEY_PAIR_ATTACH_TO_VM = "/sshkeypair/attach";

    String SSH_KEY_PAIR_DETACH_FROM_VM = "/sshkeypair/detach";

    String SSH_KEY_PAIR_NAME_REGEX = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\s()（）【】@._+-]+$";
}
