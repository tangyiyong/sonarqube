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

import java.util.concurrent.atomic.AtomicBoolean;
import org.sonar.db.DbSession;

import static org.sonar.server.organization.OrganizationFeatureImpl.FAILURE_MESSAGE;

public class TestOrganizationFeature implements OrganizationFeature {

  private final AtomicBoolean enabled = new AtomicBoolean(false);

  private TestOrganizationFeature() {
  }

  @Override
  public boolean isEnabled(DbSession dbSession) {
    return enabled.get();
  }

  public TestOrganizationFeature setEnabled(boolean b) {
    this.enabled.set(b);
    return this;
  }

  @Override
  public void enable(DbSession dbSession) {
    setEnabled(true);
  }

  @Override
  public void checkEnabled(DbSession dbSession) {
    if (!isEnabled(dbSession)) {
      throw new IllegalStateException(FAILURE_MESSAGE);
    }
  }

  /**
   * By default organization feature is disabled
   */
  public static TestOrganizationFeature standalone() {
    return new TestOrganizationFeature();
  }
}
