/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.organization.ws;

import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.DefaultTemplates;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.organization.OrganizationFeature;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Organizations.CreateWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.min;
import static java.lang.String.format;
import static org.sonar.core.util.Slug.slugify;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.KEY_MAX_LENGTH;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.KEY_MIN_LENGTH;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_AVATAR_URL;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_DESCRIPTION;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_KEY;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_URL;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateAction implements OrganizationsAction {
  private static final String ACTION = "create";
  private static final String OWNERS_GROUP_NAME = "Owners";
  private static final String OWNERS_GROUP_DESCRIPTION_PATTERN = "Owners of organization %s";
  private static final String PERM_TEMPLATE_DESCRIPTION_PATTERN = "Default permission template of organization %s";

  private final Settings settings;
  private final UserSession userSession;
  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final OrganizationsWsSupport wsSupport;
  private final System2 system2;
  private final OrganizationFeature organizationFeature;

  public CreateAction(Settings settings, UserSession userSession, DbClient dbClient, UuidFactory uuidFactory,
    OrganizationsWsSupport wsSupport, System2 system2, OrganizationFeature organizationFeature) {
    this.settings = settings;
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.wsSupport = wsSupport;
    this.system2 = system2;
    this.organizationFeature = organizationFeature;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription("Create an organization.<br />" +
        "Requires 'Administer System' permission unless any logged in user is allowed to create an organization (see appropriate setting). Organization feature must be enabled.")
      .setResponseExample(getClass().getResource("example-create.json"))
      .setInternal(true)
      .setSince("6.2")
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(false)
      .setDescription("Key of the organization. <br />" +
        "The key is unique to the whole SonarQube. <br/>" +
        "When not specified, the key is computed from the name. <br />" +
        "Otherwise, it must be between 2 and 32 chars long. All chars must be lower-case letters (a to z), digits or dash (but dash can neither be trailing nor heading)")
      .setExampleValue("foo-company");

    wsSupport.addOrganizationDetailsParams(action, true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    if (settings.getBoolean(CorePropertyDefinitions.ORGANIZATIONS_ANYONE_CAN_CREATE)) {
      userSession.checkLoggedIn();
    } else {
      userSession.checkIsRoot();
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      organizationFeature.checkEnabled(dbSession);

      String name = wsSupport.getAndCheckMandatoryName(request);
      String requestKey = getAndCheckKey(request);
      String key = useOrGenerateKey(requestKey, name);
      wsSupport.getAndCheckDescription(request);
      wsSupport.getAndCheckUrl(request);
      wsSupport.getAndCheckAvatar(request);
      checkKeyIsNotUsed(dbSession, key, requestKey, name);

      OrganizationDto organization = createOrganizationDto(dbSession, request, name, key);
      GroupDto group = createOwnersGroup(dbSession, organization);
      createDefaultTemplate(dbSession, organization, group);
      addCurrentUserToGroup(dbSession, group);

      dbSession.commit();

      writeResponse(request, response, organization);
    }
  }

  private OrganizationDto createOrganizationDto(DbSession dbSession, Request request, String name, String key) {
    OrganizationDto res = new OrganizationDto()
      .setUuid(uuidFactory.create())
      .setName(name)
      .setKey(key)
      .setDescription(request.param(PARAM_DESCRIPTION))
      .setUrl(request.param(PARAM_URL))
      .setAvatarUrl(request.param(PARAM_AVATAR_URL));
    dbClient.organizationDao().insert(dbSession, res);
    return res;
  }

  private void createDefaultTemplate(DbSession dbSession, OrganizationDto organizationDto, GroupDto group) {
    Date now = new Date(system2.now());
    PermissionTemplateDto permissionTemplateDto = dbClient.permissionTemplateDao().insert(
      dbSession,
      new PermissionTemplateDto()
        .setOrganizationUuid(organizationDto.getUuid())
        .setUuid(uuidFactory.create())
        .setName("Default template")
        .setDescription(format(PERM_TEMPLATE_DESCRIPTION_PATTERN, organizationDto.getName()))
        .setCreatedAt(now)
        .setUpdatedAt(now));

    insertGroupPermission(dbSession, permissionTemplateDto, UserRole.ADMIN, group);
    insertGroupPermission(dbSession, permissionTemplateDto, UserRole.ISSUE_ADMIN, group);
    insertGroupPermission(dbSession, permissionTemplateDto, UserRole.USER, null);
    insertGroupPermission(dbSession, permissionTemplateDto, UserRole.CODEVIEWER, null);

    dbClient.organizationDao().setDefaultTemplates(
      dbSession,
      organizationDto.getUuid(),
      new DefaultTemplates().setProjectUuid(permissionTemplateDto.getUuid()));
  }

  private void insertGroupPermission(DbSession dbSession, PermissionTemplateDto template, String permission, @Nullable GroupDto group) {
    dbClient.permissionTemplateDao().insertGroupPermission(dbSession, template.getId(), group == null ? null : group.getId(), permission);
  }

  /**
   * Owners group has an hard coded name, a description based on the organization's name and has all global permissions.
   */
  private GroupDto createOwnersGroup(DbSession dbSession, OrganizationDto organization) {
    GroupDto group = dbClient.groupDao().insert(dbSession, new GroupDto()
      .setOrganizationUuid(organization.getUuid())
      .setName(OWNERS_GROUP_NAME)
      .setDescription(format(OWNERS_GROUP_DESCRIPTION_PATTERN, organization.getName())));
    GlobalPermissions.ALL.forEach(permission -> addPermissionToGroup(dbSession, group, permission));
    return group;
  }

  private void addPermissionToGroup(DbSession dbSession, GroupDto group, String permission) {
    dbClient.groupPermissionDao().insert(
      dbSession,
      new GroupPermissionDto()
        .setOrganizationUuid(group.getOrganizationUuid())
        .setGroupId(group.getId())
        .setRole(permission));
  }

  private void addCurrentUserToGroup(DbSession dbSession, GroupDto group) {
    dbClient.userGroupDao().insert(
      dbSession,
      new UserGroupDto().setGroupId(group.getId()).setUserId(userSession.getUserId().longValue()));
  }

  @CheckForNull
  private static String getAndCheckKey(Request request) {
    String rqstKey = request.param(PARAM_KEY);
    if (rqstKey != null) {
      checkArgument(rqstKey.length() >= KEY_MIN_LENGTH, "Key '%s' must be at least %s chars long", rqstKey, KEY_MIN_LENGTH);
      checkArgument(rqstKey.length() <= KEY_MAX_LENGTH, "Key '%s' must be at most %s chars long", rqstKey, KEY_MAX_LENGTH);
      checkArgument(slugify(rqstKey).equals(rqstKey), "Key '%s' contains at least one invalid char", rqstKey);
    }
    return rqstKey;
  }

  private static String useOrGenerateKey(@Nullable String key, String name) {
    if (key == null) {
      return slugify(name.substring(0, min(name.length(), KEY_MAX_LENGTH)));
    }
    return key;
  }

  private void checkKeyIsNotUsed(DbSession dbSession, String key, @Nullable String requestKey, String name) {
    boolean isUsed = checkKeyIsUsed(dbSession, key);
    checkArgument(requestKey == null || !isUsed, "Key '%s' is already used. Specify another one.", key);
    checkArgument(requestKey != null || !isUsed, "Key '%s' generated from name '%s' is already used. Specify one.", key, name);
  }

  private boolean checkKeyIsUsed(DbSession dbSession, String key) {
    return dbClient.organizationDao().selectByKey(dbSession, key).isPresent();
  }

  private void writeResponse(Request request, Response response, OrganizationDto dto) {
    writeProtobuf(
      CreateWsResponse.newBuilder().setOrganization(wsSupport.toOrganization(dto)).build(),
      request,
      response);
  }

}
