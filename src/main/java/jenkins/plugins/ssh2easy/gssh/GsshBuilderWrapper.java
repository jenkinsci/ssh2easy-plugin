package jenkins.plugins.ssh2easy.gssh;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import jenkins.plugins.ssh2easy.gssh.client.SshClient;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author Jerry Cai
 */
public final class GsshBuilderWrapper extends BuildWrapper {

    public static final Logger LOGGER = Logger.getLogger(GsshBuilderWrapper.class.getName());

    @Extension
    public static final GsshDescriptorImpl DESCRIPTOR = new GsshDescriptorImpl();

    private boolean disable;
    private String serverInfo;
    private String preScript;
    private String postScript;

    private String groupName;
    private String ip;

    public GsshBuilderWrapper() {}

    @DataBoundConstructor
    public GsshBuilderWrapper(boolean disable, String serverInfo, String preScript, String postScript) {
        this.disable = disable;
        this.serverInfo = serverInfo;
        initHook();
        this.preScript = preScript;
        this.postScript = postScript;
    }

    private void initHook() {
        this.groupName = Server.parseServerGroupName(this.serverInfo);
        this.ip = Server.parseIp(this.serverInfo);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        Environment env = new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener)
                    throws IOException, InterruptedException {
                executePostBuildScript(listener.getLogger());
                return super.tearDown(build, listener);
            }
        };
        executePreBuildScript(listener.getLogger());
        return env;
    }

    private boolean executePreBuildScript(PrintStream logger) {
        printSplit(logger);
        logger.println("execute on server -- " + getServerInfo());
        if (isDisable()) {
            logger.println("current step is disabled , skip to execute");
            return true;
        }
        initHook();
        log(logger, "executing pre build script as below :\n" + preScript);
        SshClient sshHandler = getSshClient();
        int exitStatus = -1;
        if (preScript != null && !preScript.trim().equals("")) {
            exitStatus = sshHandler.executeShellByFTP(logger, preScript);
        }
        printSplit(logger);
        return exitStatus == SshClient.STATUS_SUCCESS;
    }

    private boolean executePostBuildScript(PrintStream logger) {
        printSplit(logger);
        logger.println("execute on server -- " + getServerInfo());
        if (isDisable()) {
            logger.println("current step is disabled , skip to execute");
            return true;
        }
        initHook();
        log(logger, "executing post build script as below :\n" + postScript);
        SshClient sshHandler = getSshClient();
        int exitStatus = -1;
        if (postScript != null && !postScript.trim().equals("")) {
            exitStatus = sshHandler.executeShellByFTP(logger, postScript);
        }
        printSplit(logger);
        return exitStatus == SshClient.STATUS_SUCCESS;
    }

    public static void printSplit(PrintStream logger) {
        logger.println("##########################################################################");
    }

    public SshClient getSshClient() {
        return DESCRIPTOR.getSshClient(getGroupName(), getIp());
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    private void log(final PrintStream logger, final String message) {
        logger.println(StringUtils.defaultString(DESCRIPTOR.getShortName()) + message);
    }

    public boolean isDisable() {
        return disable;
    }

    public void setDisable(boolean disable) {
        this.disable = disable;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPreScript() {
        return preScript;
    }

    public void setPreScript(String preScript) {
        this.preScript = preScript;
    }

    public String getPostScript() {
        return postScript;
    }

    public void setPostScript(String postScript) {
        this.postScript = postScript;
    }

    public String getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(String serverInfo) {
        this.serverInfo = serverInfo;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        return this.groupName + " +++ " + this.ip + " +++ " + this.serverInfo;
    }

    public static class GsshDescriptorImpl extends BuildWrapperDescriptor {
        public static final Logger LOGGER = Logger.getLogger(GsshDescriptorImpl.class.getName());

        public GsshDescriptorImpl() {
            super(GsshBuilderWrapper.class);
            load();
        }

        public GsshDescriptorImpl(Class<? extends BuildWrapper> clazz) {
            super(clazz);
            load();
        }

        private final CopyOnWriteList<ServerGroup> serverGroups = new CopyOnWriteList<ServerGroup>();

        public ServerGroup[] getServerGroups() {
            Iterator<ServerGroup> it = serverGroups.iterator();
            int size = 0;
            while (it.hasNext()) {
                it.next();
                size++;
            }
            return serverGroups.toArray(new ServerGroup[size]);
        }

        private final CopyOnWriteList<Server> servers = new CopyOnWriteList<Server>();

        public Server[] getServers() {
            Iterator<Server> it = servers.iterator();
            int size = 0;
            while (it.hasNext()) {
                it.next();
                size++;
            }
            return servers.toArray(new Server[size]);
        }

        @Override
        public String getDisplayName() {
            return Messages.SSHSHELL_DisplayName();
        }

        public String getShortName() {
            return "[GSSH] ";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/ssh2easy/help.html";
        }

        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) {
            GsshBuilderWrapper pub = new GsshBuilderWrapper();
            req.bindParameters(pub, "gssh.wrapp.");
            return pub;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {
            serverGroups.replaceBy(req.bindParametersToList(ServerGroup.class, "gssh.sg.wrapper."));
            servers.replaceBy(req.bindParametersToList(Server.class, "gssh.s.wrapper."));
            save();
            return true;
        }

        public boolean doServerGroupSubmit(StaplerRequest req, StaplerResponse rsp) {
            serverGroups.replaceBy(req.bindParametersToList(ServerGroup.class, "gssh.sg.wrapper."));
            save();
            return true;
        }

        public boolean doServerSubmit(StaplerRequest req, StaplerResponse rsp) {
            servers.replaceBy(req.bindParametersToList(Server.class, "gssh.s.wrapper."));
            save();
            return true;
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        public ServerGroup getServerGroup(String groupName) {
            ServerGroup[] serverGroups = getServerGroups();

            for (ServerGroup servcerGroup : serverGroups) {
                if (servcerGroup.getGroupName().trim().equals(groupName.trim())) {
                    return servcerGroup;
                }
            }
            return null;
        }

        public Server getServer(String ip) {
            Server[] servers = getServers();

            for (Server server : servers) {
                if (server.getIp().equals(ip)) {
                    return server;
                }
            }
            return null;
        }

        public SshClient getSshClient(String groupName, String ip) {
            ServerGroup serverGroup = getServerGroup(groupName);
            return serverGroup.getSshClient(ip);
        }

        public FormValidation doCheckUsername(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set a name");
            }
            if (value.length() < 2) {
                return FormValidation.warning("Isn't the name too short?");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckPort(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set a port");
            }
            if (value.length() > 4) {
                return FormValidation.warning("Isn't the port too large?");
            }
            try {
                Integer.parseInt(value);
            } catch (Exception e) {
                return FormValidation.error("Please input the port as integer");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckGroupName(@QueryParameter String value) throws IOException, ServletException {
            if (null == value) {
                return FormValidation.error("Please input username");
            }

            if (value.length() == 0) {
                return FormValidation.error("Please input username");
            }

            if (value.indexOf(Server.INFO_SPLIT) > -1) {
                return FormValidation.error("Your input name contains '" + Server.INFO_SPLIT + "' that is forbidden");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckPassword(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please input password");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
            return doCheckGroupName(value);
        }

        public FormValidation doCheckIP(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please input server ip");
            }
            return FormValidation.ok();
        }
    }
}
