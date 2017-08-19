package jenkins.plugins.ssh2easy.gssh.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import jenkins.plugins.ssh2easy.gssh.GsshPluginException;
import jenkins.plugins.ssh2easy.gssh.GsshUserInfo;
import jenkins.plugins.ssh2easy.gssh.ServerGroup;

import org.apache.commons.lang.StringEscapeUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import java.io.FileNotFoundException;
import com.jcraft.jsch.SftpException;
import java.io.IOException;

/**
 * This is Ssh handler , user for handling SSH related event and requirments
 *
 * @author Jerry Cai
 *
 */
public class DefaultSshClient extends AbstractSshClient {

	public static final String SSH_BEY = "\nexit $?";

	private String ip;
	private int port;
	private String username;
	private String password;

	public DefaultSshClient(String ip, int port, String username,
			String password) {
		this.ip = ip;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	public DefaultSshClient(ServerGroup serverGroup, String ip) {
		this.port = serverGroup.getPort();
		this.username = serverGroup.getUsername();
		this.password = serverGroup.getPassword();
		this.ip = ip;
	}

	public static SshClient newInstance(String ip, int port, String username,
			String password) {
		return new DefaultSshClient(ip, port, username, password);
	}

	public static SshClient newInstance(ServerGroup group, String ip) {
		return new DefaultSshClient(group, ip);
	}

	public Session createSession(PrintStream logger) {
		JSch jsch = new JSch();

		Session session = null;
		try {
			session = jsch.getSession(username, ip, port);
			session.setPassword(password);

			UserInfo ui = new GsshUserInfo(password);
			session.setUserInfo(ui);

			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setDaemonThread(false);
			session.connect();
			logger.println("create ssh session success with ip=[" + ip
			 + "],port=[" + port + "],username=[" + username
			 + "],password=[*******]");
		} catch (Exception e) {
			logger.println("create ssh session failed with ip=[" + ip
					+ "],port=[" + port + "],username=[" + username
					+ "],password=[*******]");
			e.printStackTrace(logger);
			throw new GsshPluginException(e);
		}
		return session;
	}

	@Override
	public int uploadFile(PrintStream logger, String fileName,
			InputStream fileContent, String serverLocation) {
		Session session = null;
		ChannelSftp sftp = null;
		OutputStream out = null;
		try {
			session = createSession(logger);
			Channel channel = session.openChannel("sftp");
			channel.setOutputStream(logger, true);
			channel.setExtOutputStream(logger, true);
			channel.connect();
			Thread.sleep(2000);
			sftp = (ChannelSftp) channel;
			sftp.setFilenameEncoding("UTF-8");
			prepareUpload(sftp, serverLocation, false);
			sftp.cd(serverLocation);
			out = sftp.put(fileName, 777);
			Thread.sleep(2000);
			byte[] buffer = new byte[2048];
			int n = -1;
			while ((n = fileContent.read(buffer, 0, 2048)) != -1) {
				out.write(buffer, 0, n);
			}
			out.flush();
			logger.println("upload file [" + fileName + "] to remote ["
					+ serverLocation + "]success");
			return STATUS_SUCCESS;
		} catch (Exception e) {
			logger.println("[GSSH - SFTP]  Exception:" + e.getMessage());
			e.printStackTrace(logger);
			throw new GsshPluginException(e);
		} finally {
			if (sftp != null) {
				logger.println("[GSSH]-SFTP exit status is " + sftp.getExitStatus());
			}
			if (null != out) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
			closeSession(session, sftp);
		}
	}

	@Override
	public int downloadFile(PrintStream logger, String remoteFile,
			String localFolder, String fileName) {
		Session session = null;
		ChannelSftp sftp = null;
		OutputStream out = null;
		try {
			session = createSession(logger);
			Channel channel = session.openChannel("sftp");
			channel.connect();
			Thread.sleep(2000);
			sftp = (ChannelSftp) channel;
			sftp.setFilenameEncoding("UTF-8");
			sftp.get(remoteFile, localFolder + "/" + fileName);
			logger.println("download remote file [" + remoteFile
					+ "] to local [" + localFolder + "] with file name ["
					+ fileName + "]");
			return STATUS_SUCCESS;
		} catch (Exception e) {
			logger.println("[GSSH - SFTP]  Exception:" + e.getMessage());
			e.printStackTrace(logger);
			throw new GsshPluginException(e);
		} finally {
			if (sftp != null) {
				logger.println("[GSSH]-SFTP exit status is " + sftp.getExitStatus());
			}
			if (null != out) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
			closeSession(session, sftp);
		}
	}

	@Override
	public int executeShell(PrintStream logger, String shell) {
		return executeCommand(logger,shell);
	}

	@Override
	public int executeCommand(PrintStream logger, String command) {

		Session session = null;
		ChannelExec channel = null;
		InputStream in = null;
		try {
			String wrapperCommand = wrapperInput(command);
			logger.write("execute below commands:".getBytes());
			logger.write(wrapperCommand.getBytes());
			logger.flush();
			session = createSession(logger);
			channel = (ChannelExec) session.openChannel("exec");
			channel.setOutputStream(logger, true);
			channel.setExtOutputStream(logger, true);
			channel.setPty(Boolean.FALSE);
			channel.setCommand(wrapperCommand);
			channel.connect();
			Thread.sleep(1000);
			while (true) {
				byte[] buffer = new byte[2048];
				int len = -1;
				in = channel.getInputStream();
				while (-1 != (len = in.read(buffer))) {
					logger.write(buffer, 0, len);
					logger.flush();
				}
				if(channel.isEOF()){
					break;
				}
				if(!channel.isConnected()){
					break;
				}
				if (channel.isClosed()) {
					break;
				}
				Thread.sleep(1000);
			}
			int status = channel.getExitStatus();
			logger.println("shell exit status code -->"+ status);
			return status;
		} catch (Exception e) {
			logger.println("[GSSH]-cmd Exception:" + e.getMessage());
			e.printStackTrace(logger);
			closeSession(session, channel);
			throw new GsshPluginException(e);

		} finally {
			if (null != in) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			closeSession(session, channel);
		}
	}

	@Override
	public boolean testConnection(PrintStream logger) {
		try {
			Session session = createSession(logger);
			closeSession(session, null);
			return true;
		} catch (Exception e) {
			logger.println("test ssh connection failed !");
			e.printStackTrace(logger);
			return false;
		}
	}

	private void closeSession(Session session, Channel channel) {
		if (channel != null) {
			channel.disconnect();
			channel = null;
		}
		if (session != null) {
			session.disconnect();
			session = null;
		}
	}

	protected String wrapperInput(String input) {
		String output = fixIEIssue(input);
//		return SSH_PROFILE + output + SSH_BEY;
		return output +SSH_BEY;
//		return  output;
	}

	/**
	 * this is fix the IE issue that it's input shell /command auto add '<br>
	 * ' if \n
	 *
	 * @param input
	 * @return
	 */
	private String fixIEIssue(String input) {
		return StringEscapeUtils.unescapeHtml(input);
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
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

	public boolean prepareUpload(
	  ChannelSftp sftpChannel,
	  String path,
	  boolean overwrite)
	  throws SftpException, IOException, FileNotFoundException {

	  boolean result = false;

	  // Build romote path subfolders inclusive:
	  String[] folders = path.split("/");
	  for (String folder : folders) {
	    if (folder.length() > 0) {
	      // This is a valid folder:
	      try {
			    System.out.println("Current Folder path before cd:" + folder);
	        sftpChannel.cd(folder);
	      } catch (SftpException e) {
	        // No such folder yet:
					System.out.println("Inside create folders: ");
	        sftpChannel.mkdir(folder);
	        sftpChannel.cd(folder);
	      }
	    }
	  }

	  // Folders ready. Remove such a file if exists:
	  if (sftpChannel.ls(path).size() > 0) {
	    if (!overwrite) {
	      System.out.println(
	        "Error - file " + path + " was not created on server. " +
	        "It already exists and overwriting is forbidden.");
	    } else {
	      // Delete file:
	      sftpChannel.ls(path); // Search file.
	      sftpChannel.rm(path); // Remove file.
	      result = true;
	    }
	  } else {
	    // No such file:
	    result = true;
	  }

	  return result;
	}


	@Override
	public String toString() {
		return "Server Info [" + this.ip + " ," + this.port + ","
				+ this.username + "," + this.password + "]";
	}
}
