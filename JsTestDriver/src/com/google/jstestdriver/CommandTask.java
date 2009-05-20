/*
 * Copyright 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.jstestdriver;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.jstestdriver.JsonCommand.CommandType;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author jeremiele@google.com (Jeremie Lenfant-Engelmann)
 */
public class CommandTask {

  private static final List<String> EMPTY_ARRAYLIST = new ArrayList<String>();
  private static final long WAIT_INTERVAL = 500L;

  private final Gson gson = new Gson();

  private final ResponseStream stream;
  private final Set<FileInfo> fileSet;
  private final String baseUrl;
  private final Server server;
  private final Map<String, String> params;

  public CommandTask(ResponseStream stream, Set<FileInfo> fileSet, String baseUrl, Server server,
      Map<String, String> params) {
    this.stream = stream;
    this.fileSet = fileSet;
    this.baseUrl = baseUrl;
    this.server = server;
    this.params = params;
  }

  private boolean startSession() {
    while (!server.startSession(baseUrl, params.get("id"))) {
      try {
        System.out.println("Waiting for a slot...");
        Thread.sleep(WAIT_INTERVAL);
      } catch (InterruptedException e) {
        System.err.println("Could not create session for browser: " + params.get("id"));
        return false;
      }
    }
    return true;
  }

  private void stopSession() {
    server.stopSession(baseUrl, params.get("id"));
  }

  private boolean isBrowserAlive() {
    String alive = server.fetch(baseUrl + "/heartbeat?id=" + params.get("id"));

    if (!alive.equals("OK")) {
      System.err.println("The browser " + params.get("id") + " is not available anymore, " +
          "you might want to re-capture it");
      return false;
    }
    return true;
  }

  private void uploadFileSet() {
    Map<String, String> fileSetParams = new LinkedHashMap<String, String>();

    fileSetParams.put("id", params.get("id"));
    fileSetParams.put("fileSet", gson.toJson(fileSet));
    String postResult = server.post(baseUrl + "/fileSet", fileSetParams);

    if (postResult.length() > 0) {
      Collection<String> filesToUpload = gson.fromJson(postResult,
          new TypeToken<Collection<String>>() {}.getType());

      if (sameFiles(filesToUpload, fileSet)) {
        JsonCommand cmd = new JsonCommand(CommandType.RESET, EMPTY_ARRAYLIST);
        Map<String, String> resetParams = new LinkedHashMap<String, String>();

        resetParams.put("id", params.get("id"));
        resetParams.put("data", gson.toJson(cmd));
        server.post(baseUrl + "/cmd", resetParams);
        server.fetch(baseUrl + "/cmd?id=" + params.get("id"));
      }
      List<FileData> filesData = new ArrayList<FileData>();
      List<String> filesSrc = new ArrayList<String>();

      for (String file : filesToUpload) {
        String readFile = null;
        long timestamp = -1;
        if (file.startsWith("http://") || file.startsWith("https://")) {
          filesSrc.add(file);
          readFile = "none";
        } else {
          filesSrc.add("/test/" + file);
          readFile = readFile(file);
          timestamp = getTimestamp(file);
        }
        filesData.add(new FileData(file, readFile, timestamp));
      }
      Map<String, String> loadFileParams = new LinkedHashMap<String, String>();

      loadFileParams.put("id", params.get("id"));
      loadFileParams.put("data", gson.toJson(filesData));
      server.post(baseUrl + "/fileSet", loadFileParams);
      JsonCommand cmd = new JsonCommand(CommandType.LOADTEST, filesSrc);

      loadFileParams.put("data", gson.toJson(cmd));
      server.post(baseUrl + "/cmd", loadFileParams);
      String jsonResponse = server.fetch(baseUrl + "/cmd?id=" + params.get("id"));
      StreamMessage message = gson.fromJson(jsonResponse, StreamMessage.class);
      Response response = message.getResponse();
      LoadedFiles loadedFiles = gson.fromJson(response.getResponse(), LoadedFiles.class);
      String loadStatus = loadedFiles.getMessage();

      if (loadStatus.length() > 0) {
        System.err.println(loadStatus);
      }
    }
  }

  public void run() {
    try {
      if (!startSession() || !isBrowserAlive()) {
        return;
      }
      uploadFileSet();
      server.post(baseUrl + "/cmd", params);
      StreamMessage streamMessage = null;

      do {
        String response = server.fetch(baseUrl + "/cmd?id=" + params.get("id"));

        streamMessage = gson.fromJson(response, StreamMessage.class);
        stream.stream(streamMessage.getResponse());
      } while (streamMessage != null && !streamMessage.isLast());
    } finally {
      stream.finish();
      stopSession();
    }
  }

  private long getTimestamp(String file) {
    for (FileInfo info : fileSet) {
      if (info.getFileName().equals(file)) {
        return info.getTimestamp();
      }
    }
    return 0;
  }

  private boolean sameFiles(Collection<String> filesToUpload, Set<FileInfo> fileSet) {
    if (filesToUpload.size() != fileSet.size()) {
      return false;
    }
    for (FileInfo info : fileSet) {
      if (!filesToUpload.contains(info.getFileName())) {
        return false;
      }
    }
    return true;
  }

  private String readFile(String file) {
    BufferedInputStream bis = null;
    try {
      bis = new BufferedInputStream(new FileInputStream(file));
      StringBuilder sb = new StringBuilder();

      for (int c = bis.read(); c != -1; c = bis.read()) {
        sb.append((char) c);
      }
      return sb.toString();
    } catch (IOException e) {
      throw new RuntimeException("Impossible to read file: " + file, e);
    } finally {
      if (bis != null) {
        try {
          bis.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
  }
}
