package com.nutritrack;

import java.nio.file.*;
import org.apache.catalina.startup.Tomcat;

public final class Launcher {
  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(System.getProperty("port", "8080"));
    Tomcat server = new Tomcat();
    server.setBaseDir("work/tomcat");
    server.setHostname("localhost");
    server.setPort(port);
    server.getConnector().setProperty("address", "127.0.0.1");
    var context = server.addWebapp("", Path.of("src/main/webapp").toAbsolutePath().toString());
    context.setParentClassLoader(Launcher.class.getClassLoader());
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    server.stop();
                    server.destroy();
                  } catch (Exception ignored) {
                  }
                }));
    server.start();
    System.out.println("NutriTrack ready at http://localhost:" + port);
    server.getServer().await();
  }
}
