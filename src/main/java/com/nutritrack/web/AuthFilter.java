package com.nutritrack.web;

import com.nutritrack.model.User;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public final class AuthFilter implements Filter {
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest r = (HttpServletRequest) req;
    HttpServletResponse s = (HttpServletResponse) res;
    r.setCharacterEncoding("UTF-8");
    s.setHeader("X-Content-Type-Options", "nosniff");
    s.setHeader("X-Frame-Options", "DENY");
    s.setHeader("Cache-Control", "no-store");
    String path = r.getServletPath();
    if (path.startsWith("/assets/")) {
      chain.doFilter(r, s);
      return;
    }
    HttpSession session = r.getSession();
    if (session.getAttribute("csrf") == null)
      session.setAttribute("csrf", UUID.randomUUID().toString());
    User u = (User) session.getAttribute("user");
    if (!path.equals("/login") && u == null) {
      s.sendRedirect(r.getContextPath() + "/login");
      return;
    }
    if (path.startsWith("/admin") && (u == null || !u.isAdmin())) {
      s.sendError(403, "Administrator access required");
      return;
    }
    if (r.getMethod().equals("POST")) {
      try {
        if (!Objects.equals(session.getAttribute("csrf"), r.getParameter("csrf"))) {
          s.sendError(403, "Session token expired. Reload the page.");
          return;
        }
      } catch (IllegalStateException tooLarge) {
        s.sendError(413, "Upload too large. Choose a food image under 5 MB or a CSV under 1 MB.");
        return;
      }
    }
    chain.doFilter(r, s);
  }
}
