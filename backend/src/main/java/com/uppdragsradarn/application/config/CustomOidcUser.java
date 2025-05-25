package com.uppdragsradarn.application.config;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

public class CustomOidcUser extends DefaultOidcUser {

  public CustomOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken) {
    super(authorities, idToken);
  }

  public CustomOidcUser(
      Collection<? extends GrantedAuthority> authorities,
      OidcIdToken idToken,
      String nameAttributeKey) {
    super(authorities, idToken, nameAttributeKey);
  }
}
