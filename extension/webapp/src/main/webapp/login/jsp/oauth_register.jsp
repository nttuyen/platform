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
<%@ page language="java" %>
<%
    PortalContainer portalContainer = PortalContainer.getCurrentInstance(session.getServletContext());
    ResourceBundleService service = portalContainer.getComponentInstanceOfType(ResourceBundleService.class);
    ResourceBundle res = service.getResourceBundle(service.getSharedResourceBundleNames(), request.getLocale()) ;
    String contextPath = portalContainer.getPortalContext().getContextPath();

    SkinService skinService = PortalContainer.getCurrentInstance(session.getServletContext())
            .getComponentInstanceOfType(SkinService.class);
    String loginCssPath = skinService.getSkin("portal/login", "Default").getCSSPath();

    User user = (User)request.getAttribute("portalUser");
    if (user == null) {
        user = new UserImpl();
    }

    List<String> errors = (List<String>)request.getAttribute("register_errors");
    Set<String> errorFields = (Set<String>)request.getAttribute("register_error_fields");
    if (errors == null) {
        errors = new ArrayList<String>();
    }
    if (errorFields == null) {
        errorFields = new HashSet<String>();
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
        <link href="/eXoSkin/skin/css/Core.css" rel="stylesheet" type="text/css"/>
        <link href="/eXoSkin/skin/css/sub-core.css" rel="stylesheet" type="text/css"/>
        <link href="<%=loginCssPath%>" rel="stylesheet" type="text/css"/>
        <script type="text/javascript" src="/platform-extension/javascript/jquery-1.7.1.js"></script>
        <script type="text/javascript" src="/platform-extension/javascript/switch-button.js"></script>

        <script type="text/javascript">
            $(document).ready(function() {
                var startlabelfooter = jQuery("#platformInfoDiv").data("labelfooter");
                var htmlContent = startlabelfooter +" eXo Platform ";
                var divContent = jQuery("#platformInfoDiv");
                var requestJsonPlatformInfo = jQuery.ajax({ type: "GET", url: "/portal/rest/platform/info", async: false, dataType: 'json' });
                if(requestJsonPlatformInfo.readyState == 4 && requestJsonPlatformInfo.status == 200){
                    //readyState 4: request finished and response is ready
                    //status 200: "OK"
                    var myresponseText = requestJsonPlatformInfo.responseText;
                    var jsonPlatformInfo = jQuery.parseJSON(myresponseText);
                    htmlContent += "v"
                    htmlContent += jsonPlatformInfo.platformVersion;
                    htmlContent += " - build "
                    htmlContent += jsonPlatformInfo.platformBuildNumber;
                }else{
                    htmlContent += "4.0"
                }
                divContent.text(htmlContent);
            });
        </script>
    </head>
    <body>
        <!--
        <% if (errors != null && errors.size() > 0) { %>
            <i class="uiIconError"></i>
            <ul>
                <% for(String s : errors) {%>
                    <li><%=s%></li>
                <% } %>
            </ul>
        <%}%>
        -->
        <div class="uiFormWithTitle uiBox uiOauthRegister">
            <h5 class="title">Register new account</h5>
            <div class="uiContentBox">
                <div class="content">
                    <form name="registerForm" action="<%= contextPath + "/login"%>" method="post" style="margin: 0px;">
                        <div class="form-horizontal">
                            <div class="control-group">
                                <label class="control-label">User Name:</label>
                                <div class="controls">
                                    <input class="username" name="username" type="text" value="<%=(user.getUserName() == null ? "" : user.getUserName())%>" placeholder="<%=res.getString("portal.login.Username")%>" onblur="this.placeholder = '<%=res.getString("portal.login.Username.blur")%>'" onfocus="this.placeholder = ''"/>
                                </div>
                            </div>

                            <div class="control-group">
                                <label class="control-label">Password:</label>
                                <div class="controls">
                                    <input class="password" name="password" type="password" placeholder="<%=res.getString("portal.login.Password")%>" onblur="this.placeholder = '<%=res.getString("portal.login.Password")%>'" onfocus="this.placeholder = ''"/>
                                </div>
                            </div>
                            <div class="control-group">
                                <label class="control-label">Confirm Password:</label>
                                <div class="controls">
                                    <input class="password" name="password2" type="password" placeholder="Re enter your password" />
                                </div>
                            </div>
                            <div class="control-group">
                                <label class="control-label">First Name:</label>
                                <div class="controls">
                                    <input type="text" name="firstName" value="<%=(user.getFirstName() == null ? "" : user.getFirstName())%>" placeholder="First Name"/>
                                </div>
                            </div>
                            <div class="control-group">
                                <label class="control-label">Las Name:</label>
                                <div class="controls">
                                    <input type="text" name="lastName" value="<%=(user.getLastName() == null ? "" : user.getLastName())%>" placeholder="Last Name"/>
                                </div>
                            </div>
                            <div class="control-group">
                                <label class="control-label">Display Name:</label>
                                <div class="controls">
                                    <input type="text" name="displayName" value="<%=(user.getDisplayName() == null ? "" : user.getDisplayName())%>" placeholder="Display name"/>
                                </div>
                            </div>
                            <div class="control-group">
                                <label class="control-label">Email:</label>
                                <div class="controls">
                                    <input type="email" name="email" value="<%=(user.getEmail() == null ? "" : user.getEmail())%>" placeholder="Email address" />
                                </div>
                            </div>

                            <input type="hidden" name="oauth_do_register_new" value="1"/>
                        </div>
                    </form>
                </div>
                <div id="UIPortalLoginFormAction" class="uiAction">
                    <button type="submit" class="btn btn-primary">Subscribe</button>
                    <button type="reset" class="btn ActionButton LightBlueStyle">Reset</button>
                    <a class="btn ActionButton LightBlueStyle" href="<%= contextPath + "/login?cancel_oauth=1"%>">Cancel</a>
                </div>
            </div>
        </div>
    </body>
</html>
