package com.nutritrack.web;

import com.nutritrack.dao.*;
import com.nutritrack.service.*;
import javax.servlet.*;

public final class AppListener implements ServletContextListener {
  public void contextInitialized(ServletContextEvent event) {
    try {
      AppServices app = new AppServices(Database.local());
      event.getServletContext().setAttribute("app", app);
      app.jobs.start();
    } catch (Exception e) {
      throw new IllegalStateException("MySQL initialization failed: " + e.getMessage(), e);
    }
  }

  public void contextDestroyed(ServletContextEvent event) {
    Object app = event.getServletContext().getAttribute("app");
    if (app instanceof AppServices s) s.close();
  }
}
