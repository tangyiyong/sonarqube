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
package org.sonar.server.organization;

import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.property.InternalProperties;

import static java.lang.String.valueOf;

public class OrganizationFeatureImpl implements OrganizationFeature {

  public static final String FAILURE_MESSAGE = "Organization feature is disabled";

  private final DbClient dbClient;

  public OrganizationFeatureImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public boolean isEnabled(DbSession dbSession) {
    Optional<String> value = dbClient.internalPropertiesDao().selectByKey(dbSession, InternalProperties.ORGANIZATION_ENABLED);
    return value.map(s -> s.equals("true")).orElse(false);
  }

  @Override
  public void checkEnabled(DbSession dbSession) {
    if (!isEnabled(dbSession)) {
      throw new IllegalStateException(FAILURE_MESSAGE);
    }
  }

  @Override
  public void enable(DbSession dbSession) {
    dbClient.internalPropertiesDao().save(dbSession, InternalProperties.ORGANIZATION_ENABLED, valueOf(true));
  }
}
