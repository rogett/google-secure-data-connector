/* Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id$
 */

package com.google.dataconnector.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.registration.v4.Registration;
import com.google.dataconnector.util.FileUtil;
import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.RegistrationException;
import com.google.dataconnector.util.ShutdownManager;
import com.google.dataconnector.util.Stoppable;
import com.google.dataconnector.util.SystemUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * <p>A watcher thread that keeps track of the resource rules message digest.</p>
 *
 * <p>If the value changes, the client re-registers with the server.</p>
 *
 * @author mtp@google.com (Matt T. Proud)
 */
@Singleton
public class ResourcesFileWatcher extends Thread implements Stoppable {
  private static final Logger LOG = Logger.getLogger(ResourcesFileWatcher.class);

  // Injected dependencies.
  private final LocalConf localConf;
  private final Registration registration;
  private final FileUtil fileUtil;
  private final SystemUtil systemUtil;
  private ShutdownManager shutdownManager;

  // Runtime dependencies.
  private FrameSender frameSender;


  /**
   * Standard constructor.
   *
   * @param localConf the {@link LocalConf} object for this agent
   * @param registration the {@link Registration} object used by this thread to invoke registration
   * process, if required
   * @param fileUtil
   * @param systemUtil
   */
  @Inject
  public ResourcesFileWatcher(final LocalConf localConf, final Registration registration,
      final FileUtil fileUtil, final SystemUtil systemUtil, final ShutdownManager shutdownManager) {
    this.localConf = localConf;
    this.registration = registration;
    this.fileUtil = fileUtil;
    this.systemUtil = systemUtil;
    this.shutdownManager = shutdownManager;
    
    // Set thread info
    this.setName(this.getClass().getName());
    this.setDaemon(true);
  }

  public void setFrameSender(final FrameSender frameSender) {
    this.frameSender = frameSender;
  }

  @Override
  public void run() {
    Preconditions.checkNotNull(frameSender);
    
    // Add this thread to the shutdown manager so it gets cleaned up.
    shutdownManager.addStoppable(this);

    byte[] lastDigest = null;

    try {
      final MessageDigest md5Digest = MessageDigest.getInstance("MD5");
      while (true) {
        final String resourcesFile = localConf.getRulesFile();
        final FileInputStream currentStream = fileUtil.getFileInputStream(resourcesFile);
        final DigestInputStream digestInputStream = new DigestInputStream(currentStream, md5Digest);

        while (digestInputStream.read() != -1) {
        }

        final byte[] currentDigest = digestInputStream.getMessageDigest().digest();

        /* There is no need to trigger a re-registration, since this will be automatically handled
         * in agent connection.
         */
        if (lastDigest == null) {
          lastDigest = currentDigest;
          continue;
        }

        if (!MessageDigest.isEqual(lastDigest, currentDigest)) {
          try {
            LOG.info("Detected change in the content of resources file " + resourcesFile +
            "; re-registering with server.");
            LOG.info("Last digest was " + lastDigest + "; new digest is " + currentDigest);
            registration.sendRegistrationInfo(frameSender);
            lastDigest = currentDigest;
          } catch (RegistrationException e) {
            LOG.error("Could not register new resources with server; will retry.", e);
          }
        }

        // sleep for a FileWatcherThreadSleepTimer min and check again
        systemUtil.sleep(localConf.getFileWatcherThreadSleepTimer() * 60 * 1000L);
      }
    } catch (InterruptedException e) {
      LOG.info("Shutting down.", e);
    } catch (FileNotFoundException e) {
      LOG.fatal("Could not read configuration.", e);
    } catch (IOException e) {
      LOG.fatal("IOException.", e);
    } catch (NoSuchAlgorithmException e) {
      LOG.fatal("NoSuchAlgorithmException.", e);
    } finally {
      LOG.info("FileWatcher thread exiting. " +
      "Any changes in resources file will require restarting agent manually.");
    }
  }

  /**
   * Shutdown the file watcher.
   */
  @Override
  public void shutdown() {
    this.interrupt(); 
  }
}
