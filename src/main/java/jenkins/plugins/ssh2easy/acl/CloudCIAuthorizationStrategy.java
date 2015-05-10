package jenkins.plugins.ssh2easy.acl;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.View;
import hudson.security.ACL;
import hudson.security.AuthorizationStrategy;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.SidACL;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.acegisecurity.Authentication;
import org.acegisecurity.acls.sid.PrincipalSid;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class CloudCIAuthorizationStrategy extends AuthorizationStrategy {

	public static final String GLOBAL = AclType.GLOBAL.getType();
	public static final String PROJECT = AclType.PROJECT.getType();

	private enum AclType {
		GLOBAL("globalACL"), PROJECT("projectACL");
		private String type;

		private AclType(String type) {
			this.type = type;
		}

		public String getType() {
			return type;
		}

		public String toString() {
			return getType();
		}

		public static AclType parseAclType(String type) {
			for (AclType aclType : values()) {
				if (aclType.getType().equals(type)) {
					return aclType;
				}
			}
			// in order to avoid to block other features , plugin shouldn't
			// throw this error
			throw new RuntimeException(
					"Read type from persistent can't recongized from config.xml");
			// return null;
		}
	}

	private final Map<AclType, CloudProject> cloudProjects = new HashMap<AclType, CloudProject>();

	@Override
	public SidACL getRootACL() {
		CloudProject root = getCloudProjectByType(AclType.GLOBAL);
		return root.getACL();
	}

	@Override
	public ACL getACL(final View item) {

		return new ACL() {
			@Override
			public boolean hasPermission(Authentication a, Permission permission) {
				ACL base = item.getOwner().getACL();

				CloudProject cloudProject = cloudProjects.get(AclType.PROJECT);
				Set<Project> matchedViewProjects = cloudProject
						.getMatchedViewsProjects(item.getViewName());
				String currentUserId = DescriptorImpl.getCurrentUser();
				for (Project project : matchedViewProjects) {
					Set<String> members = cloudProject
							.listProjectMembers(project.getProjectName());
					if (members.contains(currentUserId)) {
						if (permission == View.READ) {
							return base.hasPermission(a, View.CONFIGURE)
									|| !item.getItems().isEmpty();
						}
						return base.hasPermission(a, permission);
					}
				}
				return false;
			}
		};
	}

	@Override
	public ACL getACL(Job<?, ?> project) {
		SidACL acl;
		CloudProject cloudProject = cloudProjects.get(AclType.PROJECT);
		if (cloudProject == null) {
			acl = getRootACL();
		} else {
			acl = cloudProject
					.newAuthorizationStrategyCloudProject(project.getName())
					.getACL().newInheritingACL(getRootACL());
		}
		return acl;
	}

	@Override
	public Collection<String> getGroups() {
		Set<String> sids = new HashSet<String>();
		for (Map.Entry<AclType, CloudProject> entry : cloudProjects.entrySet()) {
			CloudProject cloudProject = (CloudProject) entry.getValue();
			sids.addAll(cloudProject.getAllMembers(true));
		}
		return sids;
	}

	public SortedMap<Project, Set<String>> getProjectPlanMap(String typeString) {
		AclType type = AclType.parseAclType(typeString);
		CloudProject cloudProject = getCloudProjectByType(type);
		if (cloudProject != null) {
			return cloudProject.getAllProjectsPlan();
		}
		return null;
	}

	public Set<String> getSIDs(String typeString) {
		AclType type = AclType.parseAclType(typeString);
		CloudProject cloudProject = getCloudProjectByType(type);
		if (cloudProject != null) {
			return cloudProject.getAllMembers();
		}
		return null;
	}

	private CloudProject getCloudProjectByType(AclType type) {
		CloudProject cloudProject;
		if (cloudProjects.containsKey(type)) {
			cloudProject = cloudProjects.get(type);
		} else {
			cloudProject = new CloudProject();
			cloudProjects.put(type, cloudProject);
		}
		return cloudProject;
	}

	private Map<AclType, CloudProject> getCloudProjectMaps() {
		return cloudProjects;
	}

	private void addProject(AclType type, Project project) {
		CloudProject cloudProject = cloudProjects.get(type);
		if (cloudProject != null) {
			cloudProject.addProject(project);
		} else {
			cloudProject = new CloudProject();
			cloudProject.addProject(project);
			cloudProjects.put(type, cloudProject);
		}
	}

	private void assignProjectMember(AclType type, Project role, String userId) {
		CloudProject roleMap = cloudProjects.get(type);
		if (roleMap != null && roleMap.hasProject(role)) {
			roleMap.addProjectMember(role, userId);
		}
	}

	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public static final String ACL_MAP = "aclMap";
	public static final String TYPE = "type";
	public static final String CLOUD_PROJECT = "project";
	public static final String PROJECT_NAME = "projectName";
	public static final String PROJECT_VIEW_NAME_PATTERN = "viewNamePattern";
	public static final String PROJECT_JOB_NAME_PATTERN = "jobNamePattern";
	public static final String PERMISSIONS = "permissions";
	public static final String ASSIGNED_MEMBER_IDS = "assignedMembers";
	public static final String PERMISSION = "permission";
	public static final String MEMBER_ID = "userID";

	public static class ConverterImpl implements Converter {
		@SuppressWarnings("rawtypes")
		public boolean canConvert(Class type) {
			return type == CloudCIAuthorizationStrategy.class;
		}

		public void marshal(Object source, HierarchicalStreamWriter writer,
				MarshallingContext context) {
			CloudCIAuthorizationStrategy strategy = (CloudCIAuthorizationStrategy) source;
			Map<AclType, CloudProject> maps = strategy.getCloudProjectMaps();
			for (Map.Entry<AclType, CloudProject> map : maps.entrySet()) {
				CloudProject cloudProject = map.getValue();
				writer.startNode(ACL_MAP);
				writer.addAttribute(TYPE, map.getKey().getType());

				for (Map.Entry<Project, Set<String>> projectMap : cloudProject
						.getAllProjectsPlan().entrySet()) {
					Project project = projectMap.getKey();
					if (project != null) {
						writer.startNode(CLOUD_PROJECT);
						writer.addAttribute(PROJECT_NAME,
								project.getProjectName());
						writer.addAttribute(PROJECT_VIEW_NAME_PATTERN, project
								.getViewNamePattern().pattern());
						writer.addAttribute(PROJECT_JOB_NAME_PATTERN, project
								.getJobNamePattern().pattern());

						writer.startNode(PERMISSIONS);
						for (Permission permission : project.getPermissions()) {
							writer.startNode(PERMISSION);
							writer.setValue(permission.getId());
							writer.endNode();
						}
						writer.endNode();

						writer.startNode(ASSIGNED_MEMBER_IDS);
						for (String sid : projectMap.getValue()) {
							writer.startNode(MEMBER_ID);
							writer.setValue(sid);
							writer.endNode();
						}
						writer.endNode();

						writer.endNode();
					}
				}
				writer.endNode();
			}
		}

		public Object unmarshal(HierarchicalStreamReader reader,
				final UnmarshallingContext context) {
			CloudCIAuthorizationStrategy strategy = create();

			while (reader.hasMoreChildren()) {
				reader.moveDown();
				if (reader.getNodeName().equals(ACL_MAP)) {
					String type = reader.getAttribute(TYPE);
					CloudProject map = new CloudProject();
					while (reader.hasMoreChildren()) {
						reader.moveDown();
						String name = reader.getAttribute(PROJECT_NAME);
						String viewNamePattern = reader
								.getAttribute(PROJECT_VIEW_NAME_PATTERN);
						String jobNamePattern = reader
								.getAttribute(PROJECT_JOB_NAME_PATTERN);
						Set<Permission> permissions = new HashSet<Permission>();

						String next = reader.peekNextChild();
						if (next != null && next.equals(PERMISSIONS)) {
							reader.moveDown();
							while (reader.hasMoreChildren()) {
								reader.moveDown();
								permissions.add(Permission.fromId(reader
										.getValue()));
								reader.moveUp();
							}
							reader.moveUp();
						}

						Project role = new Project(name, viewNamePattern,
								jobNamePattern, permissions);
						map.addProject(role);

						next = reader.peekNextChild();
						if (next != null && next.equals(ASSIGNED_MEMBER_IDS)) {
							reader.moveDown();
							while (reader.hasMoreChildren()) {
								reader.moveDown();
								map.addProjectMember(role, reader.getValue());
								reader.moveUp();
							}
							reader.moveUp();
						}
						reader.moveUp();
					}
					strategy.cloudProjects.put(AclType.parseAclType(type), map);
				}
				reader.moveUp();
			}
			return strategy;
		}

		protected CloudCIAuthorizationStrategy create() {
			return new CloudCIAuthorizationStrategy();
		}
	}

	/**
	 * Descriptor used to bind the strategy to the Web forms.
	 */
	public static final class DescriptorImpl extends
			GlobalMatrixAuthorizationStrategy.DescriptorImpl {

		@Override
		public String getDisplayName() {
			return Messages.CloudCIAuthorizationStrategy_DisplayName();
		}

		public void doProjectsSubmit(StaplerRequest req, StaplerResponse rsp)
				throws UnsupportedEncodingException, ServletException,
				FormException, IOException {
			Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);

			req.setCharacterEncoding("UTF-8");
			JSONObject json = req.getSubmittedForm();
			AuthorizationStrategy strategy = this.newInstance(req, json);
			Jenkins.getInstance().setAuthorizationStrategy(strategy);
			Jenkins.getInstance().save();
		}

		@SuppressWarnings("unchecked")
		public void doAssignSubmit(StaplerRequest req, StaplerResponse rsp)
				throws UnsupportedEncodingException, ServletException,
				FormException, IOException {
			Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);

			req.setCharacterEncoding("UTF-8");
			JSONObject json = req.getSubmittedForm();
			AuthorizationStrategy oldStrategy = Jenkins.getInstance()
					.getAuthorizationStrategy();

			if (json.has(AclType.GLOBAL.getType())
					&& json.has(AclType.PROJECT.getType())
					&& oldStrategy instanceof CloudCIAuthorizationStrategy) {
				CloudCIAuthorizationStrategy strategy = (CloudCIAuthorizationStrategy) oldStrategy;
				Map<AclType, CloudProject> maps = strategy
						.getCloudProjectMaps();

				for (Map.Entry<AclType, CloudProject> map : maps.entrySet()) {
					CloudProject roleMap = map.getValue();
					roleMap.clearAllProjectMembers();
					JSONObject projects = json.getJSONObject(map.getKey()
							.getType());
					Set<Map.Entry<String, JSONObject>> projectDataSet = (Set<Map.Entry<String, JSONObject>>) projects
							.getJSONObject("data").entrySet();
					for (Map.Entry<String, JSONObject> r : projectDataSet) {
						String sid = r.getKey();
						for (Map.Entry<String, Boolean> e : (Set<Map.Entry<String, Boolean>>) r
								.getValue().entrySet()) {
							if (e.getValue()) {
								Project role = roleMap.getProject(e.getKey());
								if (role != null && sid != null
										&& !sid.equals("")) {
									roleMap.addProjectMember(role, sid);
								}
							}
						}
					}
				}
				Jenkins.getInstance().save();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public AuthorizationStrategy newInstance(StaplerRequest req,
				JSONObject formData) throws FormException {
			AuthorizationStrategy oldStrategy = Jenkins.getInstance()
					.getAuthorizationStrategy();
			CloudCIAuthorizationStrategy strategy;

			// If the form contains data, it means the method has been called by
			// plugin
			// specifics forms, and we need to handle it.
			if (formData.has(AclType.GLOBAL.getType())
					&& formData.has(AclType.PROJECT.getType())
					&& oldStrategy instanceof CloudCIAuthorizationStrategy) {
				strategy = new CloudCIAuthorizationStrategy();

				JSONObject globalRoles = formData.getJSONObject(AclType.GLOBAL
						.getType());
				for (Map.Entry<String, JSONObject> r : (Set<Map.Entry<String, JSONObject>>) globalRoles
						.getJSONObject("data").entrySet()) {
					String roleName = r.getKey();
					Set<Permission> permissions = new HashSet<Permission>();
					for (Map.Entry<String, Boolean> e : (Set<Map.Entry<String, Boolean>>) r
							.getValue().entrySet()) {
						if (e.getValue()) {
							Permission p = Permission.fromId(e.getKey());
							permissions.add(p);
						}
					}

					Project project = new Project(roleName, permissions);
					strategy.addProject(AclType.GLOBAL, project);
					CloudProject roleMap = ((CloudCIAuthorizationStrategy) oldStrategy)
							.getCloudProjectByType(AclType.GLOBAL);
					if (roleMap != null) {
						Set<String> sids = roleMap.listProjectMembers(roleName);
						if (sids != null) {
							for (String sid : sids) {
								strategy.assignProjectMember(AclType.GLOBAL,
										project, sid);
							}
						}
					}
				}

				JSONObject projectRoles = formData
						.getJSONObject(AclType.PROJECT.getType());
				for (Map.Entry<String, JSONObject> r : (Set<Map.Entry<String, JSONObject>>) projectRoles
						.getJSONObject("data").entrySet()) {
					String roleName = r.getKey();
					Set<Permission> permissions = new HashSet<Permission>();

					String viewNamePattern = r.getValue().getString(
							PROJECT_VIEW_NAME_PATTERN);
					if (viewNamePattern != null) {
						r.getValue().remove(PROJECT_VIEW_NAME_PATTERN);
					} else {
						viewNamePattern = ".*";
					}

					String jobNamePattern = r.getValue().getString(
							PROJECT_JOB_NAME_PATTERN);
					if (jobNamePattern != null) {
						r.getValue().remove(PROJECT_JOB_NAME_PATTERN);
					} else {
						jobNamePattern = ".*";
					}
					for (Map.Entry<String, Boolean> e : (Set<Map.Entry<String, Boolean>>) r
							.getValue().entrySet()) {
						if (e.getValue()) {
							Permission p = Permission.fromId(e.getKey());
							permissions.add(p);
						}
					}

					Project project = new Project(roleName, viewNamePattern,
							jobNamePattern, permissions);
					strategy.addProject(AclType.PROJECT, project);

					CloudProject roleMap = ((CloudCIAuthorizationStrategy) oldStrategy)
							.getCloudProjectByType(AclType.PROJECT);
					if (roleMap != null) {
						Set<String> userIds = roleMap
								.listProjectMembers(roleName);
						if (userIds != null) {
							for (String userId : userIds) {
								strategy.assignProjectMember(AclType.PROJECT,
										project, userId);
							}
						}
					}
				}
			} else if (oldStrategy instanceof CloudCIAuthorizationStrategy) {
				strategy = (CloudCIAuthorizationStrategy) oldStrategy;
			} else {
				strategy = new CloudCIAuthorizationStrategy();
				Project adminRole = createAdminRole();
				strategy.addProject(AclType.GLOBAL, adminRole);
				strategy.assignProjectMember(AclType.GLOBAL, adminRole,
						getCurrentUser());
			}
			return strategy;
		}

		public static final String getCurrentUser() {
			PrincipalSid currentUser = new PrincipalSid(
					Jenkins.getAuthentication());
			return currentUser.getPrincipal();
		}

		private Project createAdminRole() {
			Set<Permission> permissions = new HashSet<Permission>();
			for (PermissionGroup group : getGroups(AclType.GLOBAL.getType())) {
				for (Permission permission : group) {
					permissions.add(permission);
				}
			}
			Project role = new Project("admin", permissions);
			return role;
		}

		public List<PermissionGroup> getGroups(String typeString) {
			AclType type = AclType.parseAclType(typeString);
			List<PermissionGroup> groups;
			switch (type) {
			case GLOBAL: {
				groups = new ArrayList<PermissionGroup>(
						PermissionGroup.getAll());
				groups.remove(PermissionGroup.get(Permission.class));
				break;
			}
			case PROJECT: {
				groups = new ArrayList<PermissionGroup>(
						PermissionGroup.getAll());
				groups.remove(PermissionGroup.get(Permission.class));
				groups.remove(PermissionGroup.get(Jenkins.class));
				groups.remove(PermissionGroup.get(Computer.class));
				groups.remove(PermissionGroup.get(View.class));
				break;
			}
			default:
				groups = null;
			}
			return groups;
		}

		public boolean showPermission(String typeString, Permission p) {
			AclType type = AclType.parseAclType(typeString);
			switch (type) {
			case GLOBAL:
				return showPermission(p);
			case PROJECT:
				return p != Item.CREATE && p.getEnabled();
			default:
				return false;
			}
		}

	}

}
