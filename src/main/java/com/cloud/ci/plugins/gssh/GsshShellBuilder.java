package com.cloud.ci.plugins.gssh;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;

import java.io.PrintStream;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.cloud.ci.plugins.gssh.client.SshClient;

/**
 * GSSH Builder extentation
 * 
 * @author Jerry Cai
 */
public class GsshShellBuilder extends Builder {
	public static final Logger LOGGER = Logger.getLogger(GsshShellBuilder.class
			.getName());
	private boolean disable;
	private String serverInfo;
	private String groupName;
	private String ip;
	private String shell;

	public GsshShellBuilder() {
	}

	@DataBoundConstructor
	public GsshShellBuilder(boolean disable ,String serverInfo, String shell) {
		this.disable = disable;
		this.serverInfo = serverInfo;
		this.shell = shell;
		this.ip = Server.parseIp(this.serverInfo);
		this.groupName = Server.parseServerGroupName(this.serverInfo);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean perform(AbstractBuild build, Launcher launcher,
			BuildListener listener) {
		PrintStream logger = listener.getLogger();
		GsshBuilderWrapper.printSplit(logger);
		if(isDisable()){
			logger.println("current step is disabled , skip to execute");
			return true;
		}
		// This is where you 'build' the project.
		SshClient sshHandler = GsshBuilderWrapper.DESCRIPTOR.getSshClient(
				getGroupName(), getIp());
		int exitStatus = sshHandler.executeShell(logger, shell);
		GsshBuilderWrapper.printSplit(logger);
		return exitStatus == SshClient.STATUS_SUCCESS;
	}

	// Overridden for better type safety.
	// If your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
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

	public String getShell() {
		return shell;
	}

	public void setShell(String shell) {
		this.shell = shell;
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

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		public String getDisplayName() {
			return Messages.SSHSHELL_DisplayName();
		}

		public Builder newInstance(StaplerRequest req, JSONObject formData)
				throws Descriptor.FormException {
			return req.bindJSON(this.clazz, formData);
		}

		public ListBoxModel doFillServerInfoItems() {
			ListBoxModel m = new ListBoxModel();
			for (Server server : GsshBuilderWrapper.DESCRIPTOR.getServers()) {
				m.add(server.getServerInfo());
			}
			return m;
		}
	}
}
