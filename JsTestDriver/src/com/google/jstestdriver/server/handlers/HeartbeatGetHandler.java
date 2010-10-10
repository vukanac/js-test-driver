// Copyright 2010 Google Inc. All Rights Reserved.
package com.google.jstestdriver.server.handlers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.servlet.RequestParameters;
import com.google.jstestdriver.CapturedBrowsers;
import com.google.jstestdriver.SlaveBrowser;
import com.google.jstestdriver.annotations.ResponseWriter;
import com.google.jstestdriver.requesthandlers.RequestHandler;

/**
 * Used by the client to know if the browser is alive.
 *
 * @author jeremiele@google.com (Jeremie Lenfant-Engelmann)
 */
class HeartbeatGetHandler implements RequestHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      HeartbeatGetHandler.class);

  private final CapturedBrowsers capturedBrowsers;
  private final Map<String, String[]> parameters;
  private final Gson gson;
  private final PrintWriter writer;

  @Inject
  public HeartbeatGetHandler(
      CapturedBrowsers capturedBrowsers,
      @RequestParameters Map<String, String[]> parameters,
      Gson gson,
      @ResponseWriter PrintWriter writer) {
    this.capturedBrowsers = capturedBrowsers;
    this.parameters = parameters;
    this.gson = gson;
    this.writer = writer;
  }

  public void handleIt() throws IOException {
    String[] ids = parameters.get("id");
    if (ids != null && ids[0] != null) {
      String id = ids[0];
      SlaveBrowser browser = capturedBrowsers.getBrowser(id);
      if (browser != null) {
        LOGGER.debug("requesting " + browser);
        if (!browser.isAlive()) {
          capturedBrowsers.removeSlave(id);
          writer.write("DEAD:" + gson.toJson(browser.getBrowserInfo()));
        } else {
          writer.write("OK");
        }
      } else {
        LOGGER.debug("heartbeat " + id + "with no browser.");
        writer.write("DEAD: can't find browser.");
      }
    } else {
      LOGGER.debug("no heartbeat, no browser.");
      writer.write("DEAD");
    }
    writer.flush();
  }
}
