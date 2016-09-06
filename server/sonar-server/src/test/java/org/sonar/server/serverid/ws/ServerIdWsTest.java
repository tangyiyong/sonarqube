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

package org.sonar.server.serverid.ws;

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.server.platform.ServerIdGenerator;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ServerIdWsTest {

  WsTester ws = new WsTester(new ServerIdWs(new ShowAction(mock(UserSession.class), mock(ServerIdGenerator.class), mock(DbClient.class))));
  WebService.Controller underTest = ws.controller("api/server_id");

  @Test
  public void definition() {
    assertThat(underTest.path()).isEqualTo("api/server_id");
    assertThat(underTest.since()).isEqualTo("6.1");
    assertThat(underTest.description()).isNotEmpty();
  }
}
