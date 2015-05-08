package com.cloud.ci.plugins.gssh.client;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

public interface SshClient {
	public static final int STATUS_SUCCESS = 0;
	public static final int STATUS_FAILED = -1;

	int executeCommand(PrintStream logger, String command);

	int executeShell(PrintStream logger, String shell);

	int executeShellByFTP(PrintStream logger, String shell);

	int uploadFile(PrintStream logger, String fileName, String fileContent,
			String serverLocation);

	int uploadFile(PrintStream logger, String fileName,
			InputStream fileContent, String serverLocation);

	int uploadFile(PrintStream logger, String fileName, File file,
			String serverLocation);
	
	int downloadFile(PrintStream logger, String remoteFile , String localFolder , String fileName);

	int downloadFile(PrintStream logger, String remoteFile , String localFolder);

	int chmod(PrintStream logger, int mode, String path);

	int chown(PrintStream logger, String own, String path);

	int mv(PrintStream logger, String source, String dest);

	int rm_Rf(PrintStream logger, String path);

	boolean testConnection(PrintStream logger);

}