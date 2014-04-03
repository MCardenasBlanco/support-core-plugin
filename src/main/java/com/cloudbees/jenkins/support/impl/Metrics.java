package com.cloudbees.jenkins.support.impl;

import com.cloudbees.jenkins.support.api.Component;
import com.cloudbees.jenkins.support.api.Container;
import com.cloudbees.jenkins.support.api.Content;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Node;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.security.Permission;
import hudson.util.IOException2;
import jenkins.model.Jenkins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

/**
 * Metrics from the different nodes.
 *
 * @author Stephen Connolly
 */
@Extension
public class Metrics extends Component {

    @Override
    @NonNull
    public String getDisplayName() {
        return "Metrics";
    }

    @Override
    public Set<Permission> getRequiredPermissions() {
        // TODO was originally no permissions, but that seems iffy
        return Collections.singleton(Jenkins.ADMINISTER);
    }

    @Override
    public void addContents(@NonNull Container result) {
        result.add(new MetricsContent("nodes/master/metrics.json", jenkins.metrics.api.Metrics.metricRegistry()));
        for (final Node node : Jenkins.getInstance().getNodes()) {
            result.add(new RemoteMetricsContent("nodes/slave/" + node.getDisplayName() + "/metrics.json", node));
        }
    }

    private static class RemoteMetricsContent extends Content {

        private final Node node;

        public RemoteMetricsContent(String name, Node node) {
            super(name);
            this.node = node;
        }

        @Override
        public void writeTo(OutputStream os) throws IOException {
            VirtualChannel channel = node.getChannel();
            if (channel == null) {
                os.write("\"N/A\"".getBytes("utf-8"));
            } else {
                try {
                    os.write(channel.call(new GetMetricsResult()));
                } catch (InterruptedException e) {
                    throw new IOException2(e);
                }
            }
        }

    }

    private static class GetMetricsResult implements Callable<byte[], RuntimeException> {
        public byte[] call() throws RuntimeException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                new MetricsContent("", null).writeTo(bos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return bos.toByteArray();
        }
    }

}
