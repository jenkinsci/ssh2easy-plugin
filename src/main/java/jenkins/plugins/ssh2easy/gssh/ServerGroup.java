package jenkins.plugins.ssh2easy.gssh;

import jenkins.plugins.ssh2easy.gssh.client.JenkinsSshClient;
import jenkins.plugins.ssh2easy.gssh.client.SshClient;

import org.kohsuke.stapler.DataBoundConstructor;

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
