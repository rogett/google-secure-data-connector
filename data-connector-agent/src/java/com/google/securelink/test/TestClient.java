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

package com.google.securelink.test;

import com.jcraft.jsch.*;
import jline.ConsoleReader;

import org.apache.commons.cli.*;
import org.apache.commons.logging.impl.*;
import org.apache.commons.logging.*;

import java.io.*;
import java.net.*;

/**
 * Implements a test SSH client to check 
 * @author rayc@google.com (Ray Colline)
 *
 */
public class TestClient {
  
  // Flags
  public static CommandLine flags;

  // woodstock user
  private static String WS_USER = "woodstock";
  
  // Logging instance
  private static Log log = new Jdk14Logger("com.google.enterprise");
  
  // SSH KEY
  private static String serverHostKey = 
  "AAAAB3NzaC1yc2EAAAABIwAAAQEAx+l7oHcTTt/5ATzb3ae1sHBE1zODmb6bc4Zn" +
  "X1dcbTg4WSZ7ADwzdHfKYoIXwOSWo8Pm8uEMaYPPD2XHYJNwtngHnyzYSMtuc0L+" +
  "2EumdCwjqhUA7OCJPAYJdX3VxIkySit8G41iwVnLPo2K1fYlD567rLgI9pvj+LIL" +
  "dD+RMASQCAmfO2qlBDtiD3NB6dA+MNlObqxR34nBZTJi8GNSztVkI+zcbKMd45E1" +
  "26xjpQ6uGSaZ3AdhdKy7pJlgHiHJ5IuBxFmvHZAGN0DVpPZrUy0SDjKWsoBEyFhx" +
  "8x/wi2+DzsuNJCQ5MS3zpZKN3xtkTFoJmVbVaDaTCw2Czs7uwQ==";

  /**
   * Creates test client ssh connection using JSCH.
   * 
   * @param host hostname to connect to
   * @param port ssh port to connect to
   * @param keyFilename private key file location on local file-system.
   */
  public TestClient(String host, int port, String keyFilename, 
      String keyPassword) throws IOException, JSchException {
    
//    String privateKey = FileUtils.readFileToString(new File(keyFilename));
    
    TestClientKnownHosts knownHostsRepo = new TestClientKnownHosts();
    JSch jsch = new JSch();
    JSch.setLogger(new TestSshClientLogger());
    jsch.setHostKeyRepository(knownHostsRepo);
    jsch.addIdentity(keyFilename, keyPassword);
    Session jschSession = jsch.getSession(WS_USER, host, port);
    jschSession.setConfig("cipher.s2c", "blowfish-cbc");
    jschSession.setConfig("cipher.c2s", "blowfish-cbc");
    jschSession.setConfig("StrictHostKeyChecking", "no");
    /* 
     * Connects to server and creates socketFactory that JSch 
     * can use to access this socket.
     */
    SshClientSocketFactory scsf = new SshClientSocketFactory(
        new Socket(host,port));
    jschSession.setSocketFactory(scsf);
    jschSession.connect();
    log.info("Connected to " + host + ":" + port); 
    jschSession.setPortForwardingR(2222, "127.0.0.1", 22);
    try {
      Thread.sleep(10000000);
    } catch (InterruptedException e) {
      log.fatal("Interrupted from sleep");
    }
    
    
  }
  
  /**
   * Parses command line flags using apache commons CLI
   * 
   * @param args unprocessed command-line args.
   */
  private static void parseFlags(String [] args) throws ParseException {
    // Parse command line flags

    Options options = new Options();
    {
      Option privateKey = new Option(
          "key", "private_key", true, "SSH Private Key");
      options.addOption(privateKey);
      
      Option host = new Option("h", "host", true, "Hostname");
      options.addOption(host);
      
      Option port = new Option("p", "port", true, "SSH Port");
      options.addOption(port);    
    }
    
    CommandLineParser parser = new GnuParser();
    flags = parser.parse(options, args);

    try {
      flags = parser.parse(options, args);
    } catch (ParseException e) {
      new HelpFormatter().printHelp("TestClient", options);
      System.err.println("Usage error: " + e.getMessage());
      System.exit(1);
    } catch (NumberFormatException e) {
      System.err.println("Arg error: " + e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Entry point.
   * 
   * @param args unprocessed command-line args.
   */
  public static void main(String[] args) throws ParseException, JSchException {
    // TODO Auto-generated method stub

    parseFlags(args);
    int sshPort = 0;
    try {
      sshPort = new Integer(flags.getOptionValue("p"));
    } catch (NumberFormatException e) {
      System.err.println("Invalid port number: " + e.getMessage());
      System.exit(1);
    }
    
    String password = "";
    try {
      System.out.print("Enter SSH key password:");
      password = new ConsoleReader().readLine('*');
    } catch (IOException e) {
      System.err.println("Error reading password: " + e.getMessage());
      System.exit(1);
    }
    
    try {
      TestClient client = new TestClient(
          flags.getOptionValue("host"),
          sshPort,
          flags.getOptionValue("private_key"),
          password);
    } catch (JSchException e) {
      log.error("JSch Exception : " + e.getMessage());
      throw e;
    } catch (IOException e) {
      log.fatal("TestClient error: " + e.getMessage());
    }
   
  }
  
  /**
   * JSCH {@link SocketFactory} class that allows one to use an already
   * connected socket with JSCH.
   * 
   * @author rayc@google.com (Ray Colline)
   */
  private class SshClientSocketFactory implements SocketFactory {
    
    private Socket connectedSocket;
    
    /**
     * Used to create a {@link SocketFactory} with an already connected
     * socket.
     * 
     * @param s and already connected socket.
     */
    public SshClientSocketFactory(Socket s) {
      this.connectedSocket = s;
    }
    
    /**
     * Returns the socket this factory was constructed with.
     * 
     * @param host no-op only to satisfy wonky interface
     * @param port no-op only to satisfy wonky interface
     */
    public Socket createSocket(String host, int port) {
      return connectedSocket;
    }
    
    /**
     * Calls the supplied socket's getInputStream() method.  This is a 
     * no-op to satisfy interface requirement.
     * 
     * @param s the socket return an InputStream from.
     */
    public InputStream getInputStream(Socket s) throws IOException {
      return s.getInputStream();
    }

    /**
     * Calls the supplied socket's getOutputStream() method.  This is a 
     * no-op to satisfy interface requirement.
     * 
     * @param s the socket return an OutputStream from.
     */
    public OutputStream getOutputStream(Socket s) throws IOException {
      return s.getOutputStream();
    }
    
  }
  
  public class TestClientKnownHosts implements HostKeyRepository {
    
    private String KNOWN_HOSTS_ID = "Woodstock Server";
    
    private HostKey hostKey;
    
    public TestClientKnownHosts() throws JSchException {
      hostKey = new HostKey("localhost", HostKey.SSHRSA, 
          serverHostKey.getBytes());
    }
    
    public void add(HostKey hostkey, UserInfo ui) {
      //pass
    }
    
    public int check(String host, byte[] key) {
      String keyString = new String(key);
      if (hostKey.getHost().equalsIgnoreCase(host)) {
        if (hostKey.getKey().equals(keyString)) {
          return HostKeyRepository.OK;
        } else {
          return HostKeyRepository.CHANGED;
        }
      }
      return HostKeyRepository.NOT_INCLUDED;
    }
    
    public HostKey[] getHostKey() {
      HostKey[] keys = new HostKey[1];
      keys[0] = hostKey;
      return keys;
    }

    public HostKey[] getHostKey(String host, String type) {
      return getHostKey();
    }
    
    public String getKnownHostsRepositoryID() {
      return KNOWN_HOSTS_ID;
    }

    public void remove(String host, String type) {
      //pass
    }
    
    public void remove(String host, String type, byte[] key) {
      //pass
    }
  }
  
  public class TestSshClientLogger implements Logger {
    
    public void log (int level, String message) {
      switch (level) {
        case Logger.DEBUG:
          log.info(message);
          break;
        case Logger.INFO:
          log.info(message);
          break;
        case Logger.ERROR:
          log.info(message);
          break;
        case Logger.FATAL:
          log.info(message);
          break;
        default:
          log.info(message);
      }
    }
    
    public boolean isEnabled(int level) {
      return true;
    }
  }

}
