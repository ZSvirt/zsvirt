package org.zstack.longjob;

import org.zstack.header.longjob.LongJob;

import java.util.TreeMap;

/**
 * Created by GuoYi on 11/25/17.
 */
public interface LongJobFactory {
    LongJob getLongJob(String jobName);
    TreeMap<String, String> getFullJobName();
    boolean supportCancel(String jobName);
    boolean supportResume(String jobName);
    boolean supportClean(String jobName);
}
