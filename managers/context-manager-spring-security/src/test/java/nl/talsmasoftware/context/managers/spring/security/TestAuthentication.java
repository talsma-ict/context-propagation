/*
 * Copyright 2016-2026 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.talsmasoftware.context.managers.spring.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;

public class TestAuthentication implements Authentication, Principal {
    private final String name;
    private boolean authenticated;

    public TestAuthentication(String name) {
        this(name, false);
    }

    public TestAuthentication(String name, boolean authenticated) {
        if (name == null) throw new NullPointerException("Name is <null>.");
        this.name = name;
        this.authenticated = authenticated;
    }

    public Object getPrincipal() {
        return this;
    }

    public String getName() {
        return name;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptySet();
    }

    public Object getCredentials() {
        return name;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    public Object getDetails() {
        return null; // not used
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object other) {
        return this == other || (other instanceof TestAuthentication
                && this.name.equals(((TestAuthentication) other).name)
        );
    }

    public String toString() {
        return getClass().getSimpleName() + "{name=" + name + ", authenticated=" + authenticated + ')';
    }
}
