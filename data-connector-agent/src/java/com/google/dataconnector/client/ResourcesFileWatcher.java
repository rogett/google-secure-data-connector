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
 */

package com.google.dataconnector.client;

import com.google.common.base.Preconditions;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.registration.v4.Registration;
import com.google.dataconnector.util.FileUtil;
import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.RegistrationException;
import com.google.dataconnector.util.SystemUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.log4j.Logger;

import java.io.File;

/**
 * starts a thread to watch the resources files
 * and if any changes, re-registers the changed resources with SDC Server
 *
 * @author vnori@google.com (Vasu Nori)
 */
@Singleton
public class ResourcesFileWatcher extends Thread {
  private static final Logger LOG = Logger.getLogger(ResourcesFileWatcher.class);

  // Dependencies.
  private final LocalConf localConf;
  private final Registration registration;
  private FrameSender frameSender;
  private FileUtil fileUtil;
  private SystemUtil systemUtil;

  /**
   * default constructor
   *
   * @param localConf the {@link LocalConf} object for this agent
   * @param registration the {@link Registration} object used by this thread to invoke registration
   * process, if required
   * @param fileUtil
   * @param systemUtil
   */
  @Inject
  public ResourcesFileWatcher(LocalConf localConf, Registration registration,
      FileUtil fileUtil, SystemUtil systemUtil) {
    this.localConf = localConf;
    this.registration = registration;
    this.fileUtil = fileUtil;
    this.systemUtil = systemUtil;
  }

  public void setFrameSender(FrameSender frameSender) {
    this.frameSender = frameSender;
  }

  @Override
  public void run() {
    Preconditions.checkNotNull(frameSender);
    File rulesFileHandle = fileUtil.openFile(localConf.getRulesFile());
    long lastModified = rulesFileHandle.lastModified();
    try {
      while (true) {
        long modified = rulesFileHandle.lastModified();
        if (modified != lastModified) {
          // file changed. re-register the resources.
          LOG.info("Detected change in modification date of Resources file: " +
              localConf.getRulesFile() + ". Re-registering resources..");
          registration.sendRegistrationInfo(frameSender);
          lastModified = modified;
        }

        // sleep for a FileWatcherThreadSleepTimer min and check again
        systemUtil.sleep(localConf.getFileWatcherThreadSleepTimer() * 60 * 1000L);
      }
    } catch (InterruptedException e) {
      // exit
    } catch (RegistrationException e) {
      LOG.fatal("re-registration of resources failed. exiting..", e);
      //TODO(mtp): maybe this should be retried
      System.exit(-1);
    } finally {
      LOG.info("FileWatcher thread exiting.." +
      "Any changes in Resources file will need Agent restart");
    }
  }
}
