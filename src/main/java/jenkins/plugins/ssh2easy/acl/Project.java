package jenkins.plugins.ssh2easy.acl;

import hudson.security.Permission;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.collections.CollectionUtils;

public final class Project implements Comparable<Object> {

    private final String projectName;
    private final Pattern viewNamePattern;
    private final Pattern jobNamePattern;
    private final Set<Permission> permissions = new HashSet<Permission>();

    Project(String name, Set<Permission> permissions) {
        this(name, ".*", ".*", permissions);
    }

    Project(String name, String viewNamePattern, String jobNamePattern, Set<Permission> permissions) {
        this(name, Pattern.compile(viewNamePattern), Pattern.compile(jobNamePattern), permissions);
    }

    Project(String name, Pattern viewNamePattern, Pattern jobNamePattern, Set<Permission> permissions) {
        this.projectName = name;
        this.viewNamePattern = viewNamePattern;
        this.jobNamePattern = jobNamePattern;
        this.permissions.addAll(permissions);
    }

    public final String getProjectName() {
        return projectName;
    }

    public final Pattern getViewNamePattern() {
        return viewNamePattern;
    }

    public final Pattern getJobNamePattern() {
        return jobNamePattern;
    }

    public final Set<Permission> getPermissions() {
        return permissions;
    }

    public final Boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public final Boolean hasAnyPermission(Set<Permission> permissions) {
        return CollectionUtils.containsAny(this.permissions, permissions);
    }

    @Override
    public int compareTo(Object o) {
        return projectName.compareTo(((Project) o).getProjectName());
    }
}
