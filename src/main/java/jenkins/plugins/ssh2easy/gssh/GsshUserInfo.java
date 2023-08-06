package jenkins.plugins.ssh2easy.gssh;

import com.jcraft.jsch.UserInfo;
import java.util.logging.Logger;

public class GsshUserInfo implements UserInfo {
    private static final Logger LOGGER = Logger.getLogger(GsshUserInfo.class.getName());

    String password;
    String passphrase;

    public GsshUserInfo(String password) {
        this.password = password;
        this.passphrase = password;
    }

    @Override
    public String getPassphrase() {
        return passphrase;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean promptPassword(String message) {
        return false;
    }

    @Override
    public boolean promptPassphrase(String message) {
        return false;
    }

    @Override
    public boolean promptYesNo(String message) {
        return false;
    }

    @Override
    public void showMessage(String message) {
        LOGGER.info(message);
    }
}
