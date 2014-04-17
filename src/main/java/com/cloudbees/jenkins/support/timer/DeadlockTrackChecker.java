package com.cloudbees.jenkins.support.timer;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.PeriodicWork;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class DeadlockTrackChecker extends PeriodicWork {
    static final File deadLockFolder = new File(Jenkins.getInstance().getRootDir(), "/support");

    @Override
    public long getRecurrencePeriod() {
        return TimeUnit.SECONDS.toMillis(15);
    }

    @Override
    protected void doRun() throws Exception {
        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        long[] deadLocks;
        try {
            deadLocks = mbean.findDeadlockedThreads();
        } catch (UnsupportedOperationException x) {
            deadLocks = null;
        }

        if (deadLocks != null && deadLocks.length != 0) {
            StringBuilder builder = new StringBuilder("Deadlock Found");
            builder.append("=============");
            ThreadInfo[] deadLockThreads = mbean.getThreadInfo(deadLocks);
            for (ThreadInfo threadInfo : deadLockThreads) {
                StackTraceElement[] elements = threadInfo.getStackTrace();
                for (StackTraceElement element : elements) {
                    builder.append(element.toString());
                }
            }

            FileUtils.writeStringToFile(
                    new File(deadLockFolder,
                             "DeadlockDetected-" + System.currentTimeMillis() + ".txt"),

                    builder.toString());
        }
    }
}
