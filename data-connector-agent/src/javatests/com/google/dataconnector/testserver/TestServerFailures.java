/* Copyright 2008 Google Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.google.dataconnector.testserver;

import com.google.dataconnector.client.SecureDataConnection.KillProcessShutdownHook;
import com.google.dataconnector.client.SecureDataConnection.RedirectStreamToLog;
import com.google.feedserver.util.BeanCliHelper;
import com.google.feedserver.util.ConfigurationBeanException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.Properties;


/**
 * Tests to make sure client exits when the SDCServer is down.
 *
 * @author vnori@google.com (Vasu Nori)
 */
public class TestServerFailures {
  private static final Logger LOG = Logger.getLogger(TestServerFailures.class);
  
  public static void main(String[] args) throws IOException, InterruptedException, 
      ConfigurationBeanException {
    PropertyConfigurator.configure(getBootstrapLoggingProperties(false));

    // parse args
    TestConf testConf = new TestConf();
    BeanCliHelper beanCliHelper = new BeanCliHelper();
    beanCliHelper.register(testConf);
    beanCliHelper.parse(args);
    PropertyConfigurator.configure(getBootstrapLoggingProperties(testConf.isVerbose()));

    // start the server - thread
    Thread serverThread = new Server(testConf);
    serverThread.setName("serverThread");
    serverThread.start();

    // start the client - process. not a thread, because client currently does System.exit()
    // on error. 
    String procId = "Client Process";
    LOG.info("Starting " + procId);
    ProcessBuilder processBuilder = new ProcessBuilder("java", 
        "com.google.dataconnector.client.Client",
        "-nouseSsl", "true",
        "-sdcServerHost", "localhost",
        "-sdcServerPort", String.valueOf(testConf.getTestServerListenPort()),
        "-localConfigFile", testConf.getLocalConfigFile(),
        "-rulesFile", testConf.getRulesFile());
    Process clientProcess = processBuilder.start();
    Runtime.getRuntime().addShutdownHook(new KillProcessShutdownHook(clientProcess));
    if (testConf.isVerbose()) {
      new RedirectStreamToLog(clientProcess.getInputStream(), LOG, procId).start();
      new RedirectStreamToLog(clientProcess.getErrorStream(), LOG, procId).start();
    }
    
    // wait for the server thread to die - at most N sec
    int N = 60;
    for (int i = 0; i < N; i++) {
      if (!serverThread.isAlive()) {
        break;
      }
      // server is still alive. sleep for 1 sec and try again
      Thread.sleep(1000);
    }
    if (serverThread.isAlive()) {
      LOG.fatal("\n\n\n ***** server is STILL alive. TEST FAILED");
      System.exit(-1);
    }
    
    // server thread is gone. make sure it is gone at the right place
    if (testConf.getActualExitLabel() != testConf.getExitLabel()) {
      LOG.fatal("\n\n\n ***** Server didn't exit where it should have. TEST FAILED");
      LOG.fatal("Server exited at " + testConf.getActualExitLabel() + 
          ". should have exited at " + testConf.getExitLabel());
      System.exit(-1);
    }
    
    // make sure the client is gone - wait for N sec
    boolean clientGone = false;
    for (int i = 0; i < N; i++) {
      try {
        int exitVal = clientProcess.exitValue();
        // client should dxit with different exit values for different errors
        clientGone = true;
        break;
      } catch (IllegalThreadStateException e) {
        // client is still alive. continue waiting for it
        Thread.sleep(1000);
      }
    }
    if (!clientGone) {
      LOG.fatal("\n\n\n ***** Client is STILL alive. TEST FAILED");
      System.exit(-1);
    }
    
    // client and server terminated just the way I like it
    System.out.println("***** SUCCESS: Client detects when " + testConf.getExitLabel().getDesc());
  }

  public static Properties getBootstrapLoggingProperties(boolean verbose) {
    Properties props = new Properties();
    props.setProperty("log4j.rootLogger", "INFO, A");
    props.setProperty("log4j.appender.A", "org.apache.log4j.ConsoleAppender");
    props.setProperty("log4j.appender.A.layout", "org.apache.log4j.PatternLayout");
    props.setProperty("log4j.appender.A.layout.ConversionPattern", "[%t] %m%n");
    String level = (verbose) ? "INFO" : "WARN";
    props.setProperty("log4j.logger.com.google.dataconnector.testserver", level);
    return props;
  }
}
