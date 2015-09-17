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

package org.exoplatform.forgotpassword.handler;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.forgotpassword.exception.UserNotFoundException;
import org.exoplatform.forgotpassword.service.ForgotPasswordService;
import org.exoplatform.services.organization.DisabledUserException;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.controller.QualifiedName;
import org.gatein.wci.security.Credentials;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class ForgotPasswordHandler extends WebRequestHandler {
  public static final String NAME = "forgot-password";

  public static final String ACTION_RECOVER_PASSWORD = "recoverPassword";

  public static final QualifiedName ACTION = QualifiedName.create("gtn", "action");
  public static final QualifiedName TOKEN = QualifiedName.create("gtn", "token");
  public static final QualifiedName LANG = QualifiedName.create("gtn", "lang");

  public static final String REQ_PARAM_LANG = "lang";
  public static final String REQ_PARAM_ACTION = "action";

  @Override
  public String getHandlerName() {
    return NAME;
  }

  @Override
  public boolean execute(ControllerContext context) throws Exception {
    HttpServletRequest req = context.getRequest();
    HttpServletResponse res = context.getResponse();
    PortalContainer container = PortalContainer.getCurrentInstance(req.getServletContext());
    ServletContext servletContext = container.getPortalContext();

    Locale locale = null;
    String lang = context.getParameter(LANG);
    if (lang != null && lang.length() > 0) {
      locale = I18N.parseTagIdentifier(lang);
    } else {
      locale = req.getLocale();
    }
    if (locale == null) {
      locale = Locale.ENGLISH;
    }

    ForgotPasswordService service = getService(ForgotPasswordService.class);
    ResourceBundleService bundleService = getService(ResourceBundleService.class);
    ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

    String action = context.getParameter(ACTION);
    String requestAction = req.getParameter(REQ_PARAM_ACTION);

    if (ACTION_RECOVER_PASSWORD.equalsIgnoreCase(action)) {
      String tokenId = context.getParameter(TOKEN);

      //. Check tokenID is expired or not
      Credentials credentials = service.verifyToken(tokenId);
      if (credentials == null) {
        //. TokenId is expired
        return dispatch("/forgotpassword/jsp/token_expired.jsp", servletContext, req, res);
      }
      final String username = credentials.getUsername();

      if ("resetPassword".equalsIgnoreCase(requestAction)) {
        String reqUser = req.getParameter("username");
        String password = req.getParameter("password");
        String confirmPass = req.getParameter("password2");

        List<String> errors = new ArrayList<String>();
        String success = "";

        if (reqUser == null || !reqUser.equals(username)) {
          // Username is changed
          String message = bundle.getString("exo.forgotPassword.usernameChanged");
          message = message.replace("{0}", username);
          errors.add(message);
        } else {
          if (password == null || password.isEmpty() || password.length() < 6 || password.length() > 30) {
            errors.add(bundle.getString("exo.forgotPassword.invalidPassword"));
          } else if (confirmPass == null || confirmPass.length() < 6 || confirmPass.length() > 30) {
            errors.add(bundle.getString("exo.forgotPassword.invalidConfirmPassword"));
          } else if (!password.equals(confirmPass)) {
            errors.add(bundle.getString("exo.forgotPassword.confirmPasswordNotMatch"));
          }
        }

        //
        if (errors.isEmpty()) {
          if (service.changePass(tokenId, username, password)) {
            success = bundle.getString("exo.forgotPassword.resetPasswordSuccess");
            password = "";
            confirmPass = "";
          } else {
            errors.add(bundle.getString("exo.forgotPassword.resetPasswordFailure"));
          }
        }
        req.setAttribute("password", password);
        req.setAttribute("password2", confirmPass);
        req.setAttribute("errors", errors);
        req.setAttribute("success", success);
      }

      req.setAttribute("tokenId", tokenId);
      req.setAttribute("username", username);

      return dispatch("/forgotpassword/jsp/reset_password.jsp", servletContext, req, res);

    } else {
      //.
      if ("send".equalsIgnoreCase(requestAction)) {
        String user = req.getParameter("username");
        if (user != null && !user.trim().isEmpty()) {
          User u;

          //
          try {
            u = service.getUserByNameOrEmail(user);
          } catch (UserNotFoundException ex) {
            req.setAttribute("error", bundle.getString("exo.forgotPassword.userNotExist"));
            u = null;
          } catch (DisabledUserException e) {
            req.setAttribute("error", bundle.getString("exo.forgotPassword.userDisabled"));
            u = null;
          } catch (Exception ex) {
            req.setAttribute("error", bundle.getString("exo.forgotPassword.loadUserError"));
            u = null;
          }

          //
          if (u != null) {
            if (service.sendRecoverPasswordEmail(u, locale, req)) {
              req.setAttribute("success", bundle.getString("exo.forgotPassword.emailSendSuccessful"));
              user = "";
            } else {
              req.setAttribute("error", bundle.getString("exo.forgotPassword.emailSendFailure"));
            }
          }

          req.setAttribute("username", user);
        }
      }

      return dispatch("/forgotpassword/jsp/forgot_password.jsp", servletContext, req, res);
    }
  }

  protected boolean dispatch(String path, ServletContext context, HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    RequestDispatcher dispatcher = context.getRequestDispatcher(path);
    if(dispatcher != null) {
      dispatcher.forward(req, res);
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }

  private <T> T getService(Class<T> clazz) {
    return ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(clazz);
  }
}
