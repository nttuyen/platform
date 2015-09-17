<%--

    Copyright (C) 2009 eXo Platform SAS.
    
    This is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as
    published by the Free Software Foundation; either version 2.1 of
    the License, or (at your option) any later version.
    
    This software is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    Lesser General Public License for more details.
    
    You should have received a copy of the GNU Lesser General Public
    License along with this software; if not, write to the Free
    Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
    02110-1301 USA, or see the FSF site: http://www.fsf.org.

--%>

<%@ page import="org.exoplatform.container.PortalContainer"%>
<%@ page import="org.exoplatform.services.resources.ResourceBundleService"%>
<%@ page import="org.exoplatform.portal.resource.SkinService"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="org.exoplatform.services.organization.User"%>
<%@ page import="org.exoplatform.services.organization.impl.UserImpl" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="org.exoplatform.portal.resource.SkinConfig" %>
<%@ page import="java.util.Collection" %>
<%@ page language="java" %>
<%
    PortalContainer portalContainer = PortalContainer.getCurrentInstance(session.getServletContext());
    ResourceBundleService service = portalContainer.getComponentInstanceOfType(ResourceBundleService.class);
    ResourceBundle res = service.getResourceBundle(service.getSharedResourceBundleNames(), request.getLocale()) ;
    String contextPath = portalContainer.getPortalContext().getContextPath();

    SkinService skinService = PortalContainer.getCurrentInstance(session.getServletContext())
            .getComponentInstanceOfType(SkinService.class);

    Collection<SkinConfig> skins = skinService.getPortalSkins("Default");
    String loginCssPath = skinService.getSkin("portal/login", "Default").getCSSPath();

    String username = (String)request.getAttribute("username");
    String tokenId = (String)request.getAttribute("tokenId");

    String password = (String)request.getAttribute("password");
    String password2 = (String)request.getAttribute("password2");


    List<String> errors = (List<String>)request.getAttribute("errors");
    String success = (String)request.getAttribute("success");
    if (errors == null) {
        errors = new ArrayList<String>();
    }

    response.setCharacterEncoding("UTF-8");
    response.setContentType("text/html; charset=UTF-8");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Oauth register</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <link rel="shortcut icon" type="image/x-icon"  href="<%=contextPath%>/favicon.ico" />
    <% for (SkinConfig skin : skins) {
        if ("CoreSkin".equals(skin.getModule()) || "CoreSkin1".equals(skin.getModule())) {%>
    <link href="<%=skin.getCSSPath()%>" rel="stylesheet" type="text/css" test="<%=skin.getModule()%>"/>
    <%}%>
    <%}%>
    <link href="<%=loginCssPath%>" rel="stylesheet" type="text/css"/>
    <script type="text/javascript" src="/platform-extension/javascript/jquery-1.7.1.js"></script>
</head>
<body>

<div class="UIPopupWindow uiPopup modal uiOauthRegister UIDragObject NormalStyle">
    <div class="popupHeader ClearFix">
        <span class="popupTitle center"><%=res.getString("exo.forgotPassword.changePass")%></span>
    </div>
    <div class="popupContent">
        <% if (errors.size() > 0) { %>
        <div class="alert alert-error mgT0 mgB20">
            <ul>
                <% for (String error : errors) { %>
                <li><i class="uiIconError"></i><span><%=error%></span></li>
                <%}%>
            </ul>
        </div>
        <%} else if (success != null && !success.isEmpty()) {%>
            <div class="alert alert-success">
                <i class="uiIconSuccess"></i><%=success%>.
            </div>
        <%}%>
        <form name="registerForm" action="<%= contextPath + "/forgot-password/recoverPassword/" + tokenId%>" method="post" style="margin: 0px;">
            <div class="content">
                <div class="form-horizontal">
                    <div class="control-group">
                        <label class="control-label"><%=res.getString("portal.login.Username")%>:</label>
                        <div class="controls">
                            <input class="username" name="username" type="text" value="<%=username%>" readonly="readonly" />
                        </div>
                    </div>

                    <div class="control-group">
                        <label class="control-label"><%=res.getString("exo.forgotPassword.newPassword")%>:</label>
                        <div class="controls">
                            <input class="password" name="password" type="password" value="<%=(password != null ? password : "")%>" />
                        </div>
                    </div>
                    <div class="control-group">
                        <label class="control-label"><%=res.getString("exo.forgotPassword.confirmNewPassword")%></label>
                        <div class="controls">
                            <input class="password" name="password2" type="password" value="<%=(password2 != null ? password2 : "")%>" />
                        </div>
                    </div>
                    <input type="hidden" name="action" value="resetPassword"/>
                </div>
            </div>
            <div id="UIPortalLoginFormAction" class="uiAction">
                <button type="submit" class="btn btn-primary"><%=res.getString("exo.forgotPassword.save")%></button>
                <a href="<%=contextPath+ "/login"%>" class="btn ActionButton LightBlueStyle"><%=res.getString("exo.forgotPassword.close")%></a>
            </div>
        </form>
    </div>
</div>
</body>
</html>
