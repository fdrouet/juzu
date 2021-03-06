/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package juzu.test.protocol.mock;

import juzu.request.SecurityContext;

import java.security.Principal;
import java.util.Set;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class MockSecurityContext implements SecurityContext {

  /** . */
  private String remoteUser;

  /** . */
  private Set<String> roles;

  /** . */
  private Principal principal;

  public String getRemoteUser() {
    return remoteUser;
  }

  public void setRemoteUser(String remoteUser) {
    if (remoteUser == null) {
      this.remoteUser = null;
      this.principal = null;
    }
    else {
      this.remoteUser = remoteUser;
      this.principal = new Principal() {
        public String getName() {
          return MockSecurityContext.this.remoteUser;
        }
      };
    }
  }

  public Principal getUserPrincipal() {
    return principal;
  }

  public boolean isUserInRole(String role) {
    return roles.contains(role);
  }

  public void addRole(String role) {
    roles.add(role);
  }

  public void clearRoles() {
    roles.clear();
  }
}
