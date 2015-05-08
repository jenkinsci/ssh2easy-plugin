package com.cloud.ci.plugins.acl;

import hudson.security.ACL;
import hudson.security.Permission;
import hudson.security.SidACL;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.acegisecurity.acls.sid.PrincipalSid;
import org.acegisecurity.acls.sid.Sid;

public class CloudProject {

	private transient SidACL acl = new CloudAclImpl();
	private final SortedMap<Project, Set<String>> projects;

	CloudProject() {
		this.projects = new TreeMap<Project, Set<String>>();
	}

	CloudProject(SortedMap<Project, Set<String>> projects) {
		this.projects = projects;
	}

	public static CloudProject newInstance() {
		return new CloudProject();
	}

	public static CloudProject newInstance(
			SortedMap<Project, Set<String>> projects) {
		return new CloudProject(projects);
	}

	private boolean hasPermission(String sid, Permission p) {
		for (Project role : listProjectByPermission(p)) {
			if (projects.get(role).contains(sid)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasProject(Project project) {
		return projects.containsKey(project);
	}

	public SidACL getACL() {
		return acl;
	}

	public void addProject(Project project) {
		if (null == getProject(project.getProjectName())) {
			projects.put(project, new HashSet<String>());
		}
	}

	public void addProjectMember(Project project, String userId) {
		if (hasProject(project)) {
			projects.get(project).add(userId);
		}
	}

	public void clearProjectMembers(Project project) {
		if (hasProject(project)) {
			projects.get(project).clear();
		}
	}

	public void clearAllProjectMembers() {
		for (Map.Entry<Project, Set<String>> entry : projects.entrySet()) {
			Project role = entry.getKey();
			clearProjectMembers(role);
		}
	}

	public Project getProject(String name) {
		for (Project project : getAllProject()) {
			if (project.getProjectName().equals(name)) {
				return project;
			}
		}
		return null;
	}

	public SortedMap<Project, Set<String>> getAllProjectsPlan() {
		return Collections.unmodifiableSortedMap(projects);
	}

	public Set<Project> getAllProject() {
		return Collections.unmodifiableSet(projects.keySet());
	}

	public SortedSet<String> getAllMembers() {
		return getAllMembers(false);
	}

	public SortedSet<String> getAllMembers(Boolean includeAnonymous) {
		TreeSet<String> members = new TreeSet<String>();
		for (Map.Entry<Project, Set<String>> entry : projects.entrySet()) {
			members.addAll((Set<String>) entry.getValue());
		}
		if (!includeAnonymous) {
			members.remove(((PrincipalSid) ACL.ANONYMOUS).getPrincipal());
		}
		return Collections.unmodifiableSortedSet(members);
	}

	public Set<String> listProjectMembers(String projectName) {
		Project project = getProject(projectName);
		if (project != null) {
			return Collections.unmodifiableSet(projects.get(project));
		}
		return null;
	}

	public CloudProject newViewAuthorizationStrategyCloudProject(String viewNamePattern) {
		Set<Project> matchedProjects = getMatchedViewsProjects(viewNamePattern);
		SortedMap<Project, Set<String>> projectsPlan = new TreeMap<Project, Set<String>>();
		for (Project project : matchedProjects) {
			projectsPlan.put(project, projects.get(project));
		}
		return CloudProject.newInstance(projectsPlan);
	}

	
	public CloudProject newAuthorizationStrategyCloudProject(String jobNamePattern) {
		Set<Project> matchedProjects = getMatchedProjects(jobNamePattern);
		SortedMap<Project, Set<String>> projectsPlan = new TreeMap<Project, Set<String>>();
		for (Project project : matchedProjects) {
			projectsPlan.put(project, projects.get(project));
		}
		return CloudProject.newInstance(projectsPlan);
	}

	private Set<Project> listProjectByPermission(final Permission permission) {
		final Set<Project> projects = new HashSet<Project>();
		final Set<Permission> permissions = new HashSet<Permission>();
		Permission p = permission;
		for (; p != null; p = p.impliedBy) {
			permissions.add(p);
		}
		new ProjectVisitor() {
			public void perform(Project current) {
				if (current.hasAnyPermission(permissions)) {
					projects.add(current);
				}
			}
		};
		return projects;
	}
	public Set<Project> getMatchedViewsProjects(final String viewNamePattern) {
		final Set<Project> projects = new HashSet<Project>();
		new ProjectVisitor() {
			public void perform(Project current) {
				Matcher viewNameMatcher = current.getViewNamePattern().matcher(
						viewNamePattern);
				if (viewNameMatcher.matches()) {
					projects.add(current);
				} 
			}
		};
		return projects;
	}
	
	private Set<Project> getMatchedProjects(final String jobNamePattern) {
		final Set<Project> projects = new HashSet<Project>();
		new ProjectVisitor() {
			public void perform(Project current) {

				Matcher jobNameMatcher = current.getJobNamePattern()
						.matcher(jobNamePattern);
				if (jobNameMatcher.matches()) {
					projects.add(current);
				}
			}
		};
		return projects;
	}

	private final class CloudAclImpl extends SidACL {

		protected Boolean hasPermission(Sid p, Permission permission) {
			if (CloudProject.this.hasPermission(toString(p), permission)) {
				return true;
			}
			return null;
		}
	}

	private abstract class ProjectVisitor {

		ProjectVisitor() {
			visit();
		}

		public void visit() {
			Set<Project> projects = getAllProject();
			Iterator<Project> iter = projects.iterator();
			while (iter.hasNext()) {
				Project current = (Project) iter.next();
				perform(current);
			}
		}

		abstract public void perform(Project current);
	}

}
