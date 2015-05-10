package jenkins.plugins.ssh2easy.gssh;

import org.kohsuke.stapler.DataBoundConstructor;


public class Server {
	public static final String INFO_SPLIT = "~~";
	
	private String serverGroupName;
	private String name;
	private String ip;
	
	public Server(){
		
	}
	
	@DataBoundConstructor
	public Server(String serverGroupName , String name , String ip){
		this.serverGroupName = serverGroupName;
		this.name = name;
		this.ip = ip;
	}
	

	public String getServerGroupName() {
		return serverGroupName;
	}

	public void setServerGroupName(String serverGroupName) {
		this.serverGroupName = serverGroupName;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getServerInfo(){
		return toString();
	}
	public String toString(){
		return this.serverGroupName +INFO_SPLIT+this.name +INFO_SPLIT+this.ip;
	}
	
	public static String parseIp(String serverInfo){
		return serverInfo.substring(serverInfo.lastIndexOf(INFO_SPLIT)+2);
	}
	public static String parseServerGroupName(String serverInfo){
		return serverInfo.substring(0, serverInfo.indexOf(INFO_SPLIT));
	}
}
