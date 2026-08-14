package org.zstack.core.ansible;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static org.zstack.core.Platform.operr;

/**
 * Created by frank on 7/22/2015.
 */
public class PrepareAnsible {
    private final static CLogger logger = Utils.getLogger(PrepareAnsible.class);

    private String targetIp;

    private static List<String> hostIPs = new ArrayList<String>();
    private static File hostsFile = new File(AnsibleConstant.INVENTORY_FILE);

    private static ReentrantLock lock = new ReentrantLock();

    static {
        try {
            if (!hostsFile.exists() && !hostsFile.createNewFile()) {
                throw new OperationFailureException(operr("fail to create new File[%s]", hostsFile));
            }

            if (AnsibleGlobalProperty.KEEP_HOSTS_FILE_IN_MEMORY) {
                String ipStr = FileUtils.readFileToString(hostsFile);
                for (String ip : ipStr.split("\n")) {
                    ip = ip.trim();
                    ip = StringUtils.strip(ip, "\n\t\r");
                    if (ip.equals("")) {
                        continue;
                    }
                    hostIPs.add(ip);
                }
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public String getTargetIp() {
        return targetIp;
    }

    public PrepareAnsible setTargetIp(String targetIp) {
        this.targetIp = targetIp;
        return this;
    }

    private boolean findIpInHostFile() throws IOException {
        BufferedReader bf = new BufferedReader(new FileReader(AnsibleConstant.INVENTORY_FILE));
        String line;

        try {
            while ((line = bf.readLine()) != null) {
                line = StringUtils.strip(line.trim(), "\t\r\n");
                if (line.equals(targetIp.trim())) {
                    return true;
                }
            }

            return false;
        } finally {
            bf.close();
        }
    }

    private void setupHostsFile() throws IOException {
        lock.lock();
        try {
            if (AnsibleGlobalProperty.KEEP_HOSTS_FILE_IN_MEMORY) {
                if (!hostIPs.contains(targetIp)) {
                    hostIPs.add(targetIp);
                    FileUtils.writeStringToFile(hostsFile, StringUtils.join(hostIPs, "\n"), false);
                    logger.debug(String.format("add target ip[%s] to %s", targetIp, AnsibleConstant.INVENTORY_FILE));
                }
            } else {
                if (!findIpInHostFile()) {
                    FileUtils.writeStringToFile(hostsFile, String.format("%s\n", targetIp), true);
                    logger.debug(String.format("add target ip[%s] to %s", targetIp, AnsibleConstant.INVENTORY_FILE));
                } else {
                    logger.debug(String.format("found target ip[%s] in %s", targetIp, AnsibleConstant.INVENTORY_FILE));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void prepare() {
        DebugUtils.Assert(targetIp  != null, "targetIp cannot be null");

        try {
            setupHostsFile();
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }
    }
}

