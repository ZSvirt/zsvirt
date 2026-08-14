package org.zstack.utils.test;

import junit.framework.Assert;
import org.junit.Test;
import org.zstack.utils.ssh.SshResult;
import org.zstack.utils.ssh.SshShell;

import java.io.IOException;

/**
 * Created by frank on 8/11/2015.
 */
public class TestSsh {
    @Test
    public void test() throws IOException {
        SshShell ssh = new SshShell();
        ssh.setHostname("127.0.0.1");
        ssh.setPassword("password");
        ssh.setUsername("root");
        SshResult res = ssh.runCommand("ls");
        Assert.assertEquals(0, res.getReturnCode());
        //System.out.println(res.getStdout());

        res = ssh.runScript("ls\nls /tmp\n");
        Assert.assertEquals(0, res.getReturnCode());
        //System.out.println(res.getStdout());

        ssh.setPassword("abcd");
        res = ssh.runCommand("ls");
        Assert.assertEquals(5, res.getReturnCode());
        Assert.assertTrue(res.isSshFailure());

        ssh.setPassword("abcd");
        res = ssh.runScript("ls");
        Assert.assertEquals(5, res.getReturnCode());
        Assert.assertTrue(res.isSshFailure());
    }
}
