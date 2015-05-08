package com.cloud.ci.plugins.gssh;

import org.kohsuke.stapler.DataBoundConstructor;

import com.cloud.ci.plugins.gssh.client.JenkinsSshClient;
import com.cloud.ci.plugins.gssh.client.SshClient;

public class ServerGroup {
	private String groupName;
	private int port;
	private String username;
	private String password;

	public ServerGroup() {
	}

	@DataBoundConstructor
	public ServerGroup(String groupName, int port, String username,
			String password) {
		this.groupName = groupName;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	public SshClient getSshClient(Server server) {
		return JenkinsSshClient.newInstance(this, server.getIp());
	}

	public SshClient getSshClient(String ip) {
		return JenkinsSshClient.newInstance(this, ip);
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
