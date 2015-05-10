package jenkins.plugins.ssh2easy.gssh;

public class GsshPluginException extends RuntimeException {

	public static final String EXCEPTION_FROM = "[GSSH - SFTP]  Exception ";

	private static final long serialVersionUID = 1L;

	public GsshPluginException(String message) {
		super(EXCEPTION_FROM + message);
	}

	public GsshPluginException(String message, Throwable cause) {
		super(EXCEPTION_FROM + message, cause);
	}

	public GsshPluginException(Throwable cause) {
		super(EXCEPTION_FROM + cause.getMessage(), cause);
	}
}
