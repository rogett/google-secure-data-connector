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

package com.google.securelink.testserver;

import com.google.securelink.testserver.ServerException.Reason;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.Jdk14Logger;

/**
 * Static class to store server wide variables like flags and loggers.
 * 
 * @author rayc@google.com (Ray Colline)
 *
 */
public class ServerConf {
  
  // Flags place holder.
  public static CommandLine flags;
  
  // Logging instance
  public static Log log = new Jdk14Logger("com.google.enterprise");

  /**
   * Checks flags and throws an {@link ServerException} should there be any
   * errors.  New flags should implement their checking here.
   * 
   * @throws ServerException if any flags are invalid.
   */
  protected static void checkFlags() throws ServerException {
    //check listen port.
    int listenPort = 0;
    try {
      listenPort = new Integer(flags.getOptionValue("p"));
      listenPort = new Integer(flags.getOptionValue("localSocksPort"));
    } catch (NumberFormatException e) {
      log.error("Invalid port number: " + e.getMessage());
      throw new ServerException(Reason.FLAGS_ERROR, "Invalid port number: " + 
          e.getMessage());
    }

  }
  
  /**
   * Define all flags here
   * 
   * @return all configured command-line options.
   */
  protected static Options defineFlags() {
    Options options = new Options();
    
    Option port = new Option("p", "port", true, "Port");
    port.setRequired(true);
    options.addOption(port);
    
    Option key = new Option("key", "private_key", true, "SSH Private Key");
    key.setRequired(true);
    options.addOption(key);
    
    Option localSocksPort = new Option("localSocksPort", "localSocksPort", true, 
        "Port to allocate on localside that forwards " + 
        "requests to remote SOCKS proxy");
    options.addOption(localSocksPort);

    return options;
  }
  
  /**
   * Adds options and parses command-line flags.
   *  
   * @param args argv string from invocation.
   * @throws ServerException if there are flag issues.
   */
  protected static void parseFlags(String [] args) throws ServerException {

    // Get flag definitions
    Options options = defineFlags();
    // Parse command line flags
    CommandLineParser parser = new GnuParser();
    try {
      flags = parser.parse(options, args);
      checkFlags();
    } catch (ParseException e) {
      new HelpFormatter().printHelp("Server", options);
      System.err.println("Usage error: " + e.getMessage());
      throw new ServerException(Reason.FLAGS_ERROR, "Command line error");
    } catch (NumberFormatException e) {
      new HelpFormatter().printHelp("Server", options);
      System.err.println("Arg error: " + e.getMessage());
      throw new ServerException(Reason.FLAGS_ERROR, "Command line error");
    }
  }
}
