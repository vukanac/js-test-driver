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
package com.google.jstestdriver.idea;

import com.google.inject.Guice;
import com.google.jstestdriver.ActionFactory;
import com.google.jstestdriver.ActionFactoryModule;
import com.google.jstestdriver.ActionRunner;
import com.google.jstestdriver.ConfigurationParser;
import com.google.jstestdriver.IDEPluginActionBuilder;
import com.google.jstestdriver.ResponseStreamFactory;
import com.google.jstestdriver.idea.ui.ToolPanel;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings;
import com.intellij.execution.configurations.RunnableState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.util.concurrency.SwingWorker;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class TestRunnerState implements RunnableState {

  private final JSTestDriverConfiguration jsTestDriverConfiguration;
  private final Project project;

  public TestRunnerState(JSTestDriverConfiguration jsTestDriverConfiguration, Project project) {
    this.jsTestDriverConfiguration = jsTestDriverConfiguration;
    this.project = project;
  }

  @Nullable
  public ExecutionResult execute() throws ExecutionException {
    File path = new File("/usr/local/google/alexeagle/js-test-driver/JsTestDriver");
    ToolWindow window =
        ToolWindowManager.getInstance(project).getToolWindow(JSTestDriverToolWindow.TOOL_WINDOW_ID);
    String serverURL = "http://localhost:" + jsTestDriverConfiguration.getServerPort();
    Content content = window.getContentManager().getContent(0);

    final ToolPanel toolPanel = (ToolPanel) content.getComponent();
    toolPanel.clearTestResults();
    toolPanel.setTestRunner(this);
    ResponseStreamFactory responseStreamFactory = toolPanel.createResponseStreamFactory();
    final ActionRunner dryRunRunner =
        makeActionBuilder(path, serverURL, responseStreamFactory)
          .dryRunFor(Arrays.asList("all")).build();
    final ActionRunner testRunner =
        makeActionBuilder(path, serverURL, responseStreamFactory)
            .addAllTests().build();
    final ActionRunner resetRunner =
        makeActionBuilder(path, serverURL, responseStreamFactory)
            .resetBrowsers().build();
    toolPanel.setResetRunner(resetRunner);
    final SwingWorker worker = new SwingWorker() {
      @Override
      public Object construct() {
        dryRunRunner.runActions();
        toolPanel.dryRunComplete();
        testRunner.runActions();
        return null;
      }
    };
    window.show(new Runnable() {
      public void run() {
        worker.start();
      }
    });

    return null;
  }

  private IDEPluginActionBuilder makeActionBuilder(File path,
                                                   String serverURL,
                                                   ResponseStreamFactory responseStreamFactory)
      throws ExecutionException {
    try {
      FileReader fileReader = new FileReader(jsTestDriverConfiguration.getSettingsFile());
      ConfigurationParser configurationParser = new ConfigurationParser(path, fileReader);
      return new IDEPluginActionBuilder(configurationParser, serverURL, responseStreamFactory);
    } catch (FileNotFoundException e) {
      throw new ExecutionException("Failed to read settings file " +
                                   jsTestDriverConfiguration.getSettingsFile(), e);
    }
  }

  public RunnerSettings getRunnerSettings() {
    return null;
  }

  public ConfigurationPerRunnerSettings getConfigurationSettings() {
    return null;
  }

  public Module[] getModulesToCompile() {
    return new Module[0];
  }
}