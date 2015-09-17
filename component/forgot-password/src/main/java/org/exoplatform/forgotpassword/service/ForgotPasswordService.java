/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.forgotpassword.service;

import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.forgotpassword.exception.UserNotFoundException;
import org.exoplatform.forgotpassword.handler.ForgotPasswordHandler;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.organization.DisabledUserException;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.security.Token;
import org.exoplatform.web.security.security.RemindPasswordTokenService;
import org.gatein.wci.security.Credentials;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class ForgotPasswordService {
  private final OrganizationService orgService;
  private final MailService mailService;
  private final SettingService settingService;
  private final ResourceBundleService bundleService;
  private final RemindPasswordTokenService remindPasswordTokenService;
  private final WebAppController webController;

  public ForgotPasswordService(OrganizationService orgService, MailService mailService, SettingService settingService, ResourceBundleService bundleService, RemindPasswordTokenService remindPasswordTokenService, WebAppController controller) {
    this.orgService = orgService;
    this.mailService = mailService;
    this.bundleService = bundleService;
    this.remindPasswordTokenService = remindPasswordTokenService;
    this.webController = controller;
    this.settingService = settingService;
  }

  public Credentials verifyToken(String tokenId) {
    Token token = remindPasswordTokenService.getToken(tokenId);
    if (token == null || token.isExpired()) {
      return null;
    }
    return token.getPayload();
  }

  public boolean changePass(final String tokenId, final String username, final String password) {
    try {
      User user = orgService.getUserHandler().findUserByName(username);
      user.setPassword(password);
      orgService.getUserHandler().saveUser(user, true);

      try {
        remindPasswordTokenService.deleteToken(tokenId);
      } catch (Exception ex) {
        //Ignore any exception delete token
      }

      return true;
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  public User getUserByNameOrEmail(String user) throws DisabledUserException, UserNotFoundException, Exception {
    if (user == null || user.isEmpty()) {
      return null;
    }
    UserHandler uHandler = orgService.getUserHandler();

    User u = uHandler.findUserByName(user, UserStatus.ANY);
    if (u == null && user.contains("@")) {
      Query query = new Query();
      query.setEmail(user);
      ListAccess<User> list = uHandler.findUsersByQuery(query, UserStatus.ANY);
      if (list.getSize() > 0) {
        u = list.load(0, 1)[0];
      }
    }

    if (u == null) {
      throw new UserNotFoundException();
    }

    if (!u.isEnabled()) {
      throw new DisabledUserException(u.getUserName());
    }

    return u;
  }

  public boolean sendRecoverPasswordEmail(User user, Locale locale, HttpServletRequest req) throws UserNotFoundException, DisabledUserException, Exception {
    if (user == null) {
      throw new IllegalArgumentException("User or Locale must not be null");
    }

    PortalContainer container = PortalContainer.getCurrentInstance(req.getServletContext());

    ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

    Credentials credentials = new Credentials(user.getUserName(), "");
    String tokenId = remindPasswordTokenService.createToken(credentials);

    Router router = webController.getRouter();
    Map<QualifiedName, String> params = new HashMap<QualifiedName, String>();
    params.put(WebAppController.HANDLER_PARAM, ForgotPasswordHandler.NAME);
    params.put(ForgotPasswordHandler.ACTION, ForgotPasswordHandler.ACTION_RECOVER_PASSWORD);
    params.put(ForgotPasswordHandler.TOKEN, tokenId);
    params.put(ForgotPasswordHandler.LANG, locale.toLanguageTag());

    StringBuilder url = new StringBuilder();
    url.append(req.getScheme()).append("://").append(req.getServerName());
    if (req.getServerPort() != 80) {
      url.append(':').append(req.getServerPort());
    }
    url.append(container.getPortalContext().getContextPath());
    url.append(router.render(params));
    //url.append("?").append(ForgotPasswordHandler.REQ_PARAM_LANG).append("=").append(locale.toString());

    String emailBody = buildEmailBody(user, bundle, url.toString());
    String emailSubject = getEmailSubject(user, bundle);

    Message message = new Message();
    message.setFrom(getSenderEmail());
    message.setTo(user.getEmail());
    message.setSubject(emailSubject);
    message.setBody(emailBody);
    message.setMimeType("text/html");
    mailService.sendMessage(message);

    return true;
  }

  private String getEmailSubject(User user, ResourceBundle bundle) {
    return bundle.getString("exo.forgotPassword.email.subject");
  }

  private String buildEmailBody(User user, ResourceBundle bundle, String link) {
    String content;
    InputStream input = this.getClass().getClassLoader().getResourceAsStream("conf/forgot_password_email_template.html");
    if (input == null) {
      content = "";
    } else {
      content = resolve(input, bundle);
    }

    content = content.replaceAll("\\$\\{FIRST_NAME\\}", user.getFirstName());
    content = content.replaceAll("\\$\\{USERNAME\\}", user.getUserName());
    content = content.replaceAll("\\$\\{RESET_PASSWORD_LINK\\}", link);

    return content;
  }

  private static final Pattern PATTERN = Pattern.compile("&\\{([a-zA-Z0-9\\.]+)\\}");
  private String resolve(InputStream input, ResourceBundle bundle) {
    // Read from input string
    StringBuffer content = new StringBuffer();
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(input));
      String line = null;
      while ((line = reader.readLine()) != null) {
        if (content.length() > 0) {
          content.append("\n");
        }
        resolveLanguage(content, line, bundle);
      }
    } catch (IOException ex) {
      //TODO: log exception here
    }
    return content.toString();
  }
  private void resolveLanguage(StringBuffer sb, String input, ResourceBundle bundle) {
    Matcher matcher = PATTERN.matcher(input);
    while(matcher.find()) {
      String key = matcher.group(1);
      String resource;
      try {
        resource = bundle.getString(key);
      } catch (MissingResourceException ex) {
        resource = key;
      }
      matcher.appendReplacement(sb, resource);
    }
    matcher.appendTail(sb);
  }

  private String getSenderName() {
    SettingValue value = settingService.get(org.exoplatform.commons.api.settings.data.Context.GLOBAL, Scope.GLOBAL, NotificationPluginUtils.NOTIFICATION_SENDER_NAME);
    if (value == null) {
      return System.getProperty("exo.notifications.portalname", "eXo");
    } else {
      return (String)value.getValue();
    }
  }
  private String getSenderEmail() {
    SettingValue value = settingService.get(org.exoplatform.commons.api.settings.data.Context.GLOBAL, Scope.GLOBAL, NotificationPluginUtils.NOTIFICATION_SENDER_EMAIL);
    if (value == null) {
      return System.getProperty("gatein.email.smtp.from", "noreply@exoplatform.com");
    } else {
      return (String)value.getValue();
    }
  }
}
