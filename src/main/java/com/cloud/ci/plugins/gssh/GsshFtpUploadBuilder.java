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

import java.io.File;
import java.io.PrintStream;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.cloud.ci.plugins.gssh.client.SshClient;

/**
 * GSSH FTP Builder extentation
 * 
 * @author Jerry Cai
 */
public class GsshFtpUploadBuilder extends Builder {
	public static final Logger LOGGER = Logger.getLogger(GsshShellBuilder.class
			.getName());
	private boolean disable;
	private String serverInfo;
	private String groupName;
	private String ip;
	private String localFilePath;
	private String remoteLocation;
	private String fileName;

	public GsshFtpUploadBuilder() {
	}

	@DataBoundConstructor
	public GsshFtpUploadBuilder(boolean disable ,String serverInfo, String localFilePath,
			String remoteLocation, String fileName) {
		this.disable = disable;
		this.serverInfo = serverInfo;
		this.ip = Server.parseIp(this.serverInfo);
		this.groupName = Server.parseServerGroupName(this.serverInfo);
		this.localFilePath = localFilePath;
		this.remoteLocation = remoteLocation;
		this.fileName = fileName;
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
		logger.println("execute on server -- " + getServerInfo());
		// This is where you 'build' the project.
		SshClient sshClient = GsshBuilderWrapper.DESCRIPTOR.getSshClient(
				getGroupName(), getIp());
		int exitStatus = -1;
		try {
			File file = new File(getLocalFilePath());
			if (null == fileName || fileName.trim().equals("")) {
				fileName = file.getName();
			}
			exitStatus = sshClient.uploadFile(logger, fileName, file, remoteLocation);
			GsshBuilderWrapper.printSplit(logger);

		} catch (Exception e) {
			return false;
		}
		return exitStatus == SshClient.STATUS_SUCCESS;
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

	public String getIp() {
		return ip;
	}

	public boolean isDisable() {
		return disable;
	}

	public void setDisable(boolean disable) {
		this.disable = disable;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getLocalFilePath() {
		return localFilePath;
	}

	public void setLocalFilePath(String localFilePath) {
		this.localFilePath = localFilePath;
	}

	public String getRemoteLocation() {
		return remoteLocation;
	}

	public void setRemoteLocation(String remoteLocation) {
		this.remoteLocation = remoteLocation;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	// Overridden for better type safety.
	// If your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		public String getDisplayName() {
			return Messages.SSHFTPUPLOAD_DisplayName();
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