package org.zstack.utils.ssh;

interface SshRunner {
    SshResult run();

    String getCommand();
}
