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
package org.sonar.server.user;

import org.sonar.db.component.ComponentDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;

public abstract class AbstractUserSession implements UserSession {
  private static final String INSUFFICIENT_PRIVILEGES_MESSAGE = "Insufficient privileges";
  private static final ForbiddenException INSUFFICIENT_PRIVILEGES_EXCEPTION = new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
  private static final String AUTHENTICATION_IS_REQUIRED_MESSAGE = "Authentication is required";

  @Override
  public UserSession checkLoggedIn() {
    if (!isLoggedIn()) {
      throw new UnauthorizedException(AUTHENTICATION_IS_REQUIRED_MESSAGE);
    }
    return this;
  }


  @Override
  public UserSession checkIsRoot() {
    if (!isRoot()) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  @Override
  public UserSession checkOrganizationPermission(String organizationUuid, String permission) {
    if (isRoot()) {
      return this;
    }
    if (!hasOrganizationPermission(organizationUuid, permission)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  @Override
  public boolean hasComponentPermission(String permission, ComponentDto component) {
    return hasComponentUuidPermission(permission, component.projectUuid());
  }

  @Override
  public UserSession checkComponentPermission(String projectPermission, ComponentDto component) {
    if (!hasComponentPermission(projectPermission, component)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  @Override
  public UserSession checkComponentUuidPermission(String permission, String componentUuid) {
    if (!hasComponentUuidPermission(permission, componentUuid)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  public static ForbiddenException insufficientPrivilegesException() {
    return INSUFFICIENT_PRIVILEGES_EXCEPTION;
  }
}
