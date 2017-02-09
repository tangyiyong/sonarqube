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
package org.sonar.db.organization;

import javax.annotation.Nullable;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.template.PermissionTemplateDto;

import static com.google.common.base.Preconditions.checkArgument;

public class OrganizationDbTester {
  private final DbTester dbTester;

  public OrganizationDbTester(DbTester dbTester) {
    this.dbTester = dbTester;
  }

  public OrganizationDbTester enable() {
    dbTester.properties().insertInternal("organization.enabled", "true");
    return this;
  }

  /**
   * Insert an {@link OrganizationDto} and commit the session
   */
  public OrganizationDto insert() {
    return insert(OrganizationTesting.newOrganizationDto());
  }

  public OrganizationDto insertForKey(String key) {
    return insert(OrganizationTesting.newOrganizationDto().setKey(key));
  }

  public OrganizationDto insertForUuid(String organizationUuid) {
    return insert(OrganizationTesting.newOrganizationDto().setUuid(organizationUuid));
  }

  /**
   * Insert the provided {@link OrganizationDto} and commit the session
   */
  public OrganizationDto insert(OrganizationDto dto) {
    DbSession dbSession = dbTester.getSession();
    dbTester.getDbClient().organizationDao().insert(dbSession, dto);
    dbSession.commit();
    return dto;
  }

  public void setDefaultTemplates(PermissionTemplateDto projectDefaultTemplate, @Nullable PermissionTemplateDto viewDefaultTemplate) {
    checkArgument(viewDefaultTemplate == null
      || viewDefaultTemplate.getOrganizationUuid().equals(projectDefaultTemplate.getOrganizationUuid()),
      "default template for project and view must belong to the same organization");

    DbSession dbSession = dbTester.getSession();
    dbTester.getDbClient().organizationDao().setDefaultTemplates(dbSession, projectDefaultTemplate.getOrganizationUuid(),
      new DefaultTemplates()
        .setProjectUuid(projectDefaultTemplate.getUuid())
        .setViewUuid(viewDefaultTemplate == null ? null : viewDefaultTemplate.getUuid()));
    dbSession.commit();
  }

  public void setDefaultTemplates(OrganizationDto defaultOrganization,
    String projectDefaultTemplateUuid, @Nullable String viewDefaultTemplateUuid) {
    DbSession dbSession = dbTester.getSession();
    dbTester.getDbClient().organizationDao().setDefaultTemplates(dbSession, defaultOrganization.getUuid(),
      new DefaultTemplates().setProjectUuid(projectDefaultTemplateUuid).setViewUuid(viewDefaultTemplateUuid));
    dbSession.commit();
  }

}
