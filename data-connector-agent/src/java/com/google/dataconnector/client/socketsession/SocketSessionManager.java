// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.dataconnector.client.socketsession;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.dataconnector.client.SocketSessionRequestHandler.Sink;
import com.google.dataconnector.protocol.proto.SdcFrame.SocketSessionData;
import com.google.dataconnector.protocol.proto.SdcFrame.SocketSessionReply;
import com.google.dataconnector.util.ClockUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages all the inflight socket sessions for this agent.
 * @author dchung@google.com (David Chung)
 */
@Singleton
public class SocketSessionManager {

  public static int DEFAULT_CONNECT_TIMEOUT = 60000;
  
  private static Logger logger = Logger.getLogger(SocketSessionManager.class);
  
  // Injected Dependencies.
  protected final ThreadPoolExecutor threadPoolExecutor;
  private final ClockUtil clock;
  private final Map<ByteString, Session> sessions = Maps.newHashMap();
  
  @Inject
  public SocketSessionManager(ThreadPoolExecutor threadPoolExecutor, ClockUtil clock) {
    this.threadPoolExecutor = threadPoolExecutor;
    this.clock = clock;
  }

  enum SessionState {
    EXCEPTION,
    CREATED,
    OPEN,
    CLOSED;
  }
  
  /**
   * Class that encapsulates the states of a socket session.
   */
  public class Session {
    private SessionState state;
    private final ByteString handle;
    private final InetSocketAddress endpoint;
    private final AtomicLong bytesReceived = new AtomicLong(0);
    private final Sink<SocketSessionData> receiver;
    private Socket socket;
    private final AtomicBoolean connectReplySent = new AtomicBoolean(false);
    
    Session(Sink<SocketSessionData> cloud, ByteString handle, InetSocketAddress endpoint) {
      this.handle = handle;
      this.endpoint = endpoint;
      this.state = SessionState.CREATED;
      this.receiver = cloud;
    }
    
    @Override
    public String toString() {
      return String.format("Session[%s@%s:%d]", handle.toStringUtf8(),
          endpoint.getHostName(), endpoint.getPort());
    }

    private Thread inputForwarder = new Thread() {
      byte[] buffer = new byte[1024 * 64];
      @Override
      public void run() {
        long start = SocketSessionManager.this.clock.currentTimeMillis();
        
        try {
          // Wait till connect reply is sent.
          while (!connectReplySent.get()) {
            Thread.sleep(50L);
          }
          logger.debug("Starting listener for input stream.");
          InputStream input = Session.this.socket.getInputStream();
          for (int read = 0; ; read = input.read(buffer)) {
            if (read > 0) {
              long offset = bytesReceived.getAndAdd(read);
              ByteString data = ByteString.copyFrom(buffer, 0, read);
              
              SocketSessionData m = SocketSessionData.newBuilder()
                .setSocketHandle(handle)
                .setData(data)
                .setStreamOffset(offset).build();
              
              receiver.receive(m);
              logger.debug( Session.this + ": [" + offset + "] received " + read + " bytes");
            }
          }
        } catch (SocketException e) {
          logger.warn(this + ": Socket closed.", e);
          synchronized (Session.this) {
            Session.this.state = SessionState.CLOSED;
          }
        } catch (Exception e) {
          logger.warn(this + ": Exception while reading input.", e);
          synchronized (Session.this) {
            Session.this.state = SessionState.EXCEPTION;
          }
        } finally {
          // Send a CLOSE back up the cloud to confirm.
          long offset = bytesReceived.getAndAdd(0);
          // Closing down.  Need to tell the cloud about it.
          SocketSessionData m = SocketSessionData.newBuilder()
          .setSocketHandle(handle)
          .setClose(true)
          .setStreamOffset(offset).build();
          receiver.receive(m);
          logger.debug( Session.this + ": [" + offset + "] sent CLOSE.");
        }
        logger.debug( Session.this + ": Stoped reading input after " + 
            (SocketSessionManager.this.clock.currentTimeMillis() - start) + " msec.");
      }
    };
    
    /**
     * Connects the socket.  Returns true iff connect succeeds.
     * @return True if connected.
     */
    synchronized boolean connect() {
      if (this.state != SessionState.CREATED) {
        logger.warn(this + ": Invalid state when connect = " + this.state);
        return false;
      }
      try {
        this.socket = new Socket();
        this.socket.connect(endpoint, DEFAULT_CONNECT_TIMEOUT);
        // Also submit a listener to the queue for execution
        inputForwarder.start();
        this.state = SessionState.OPEN;
        return true;
      } catch (IOException e) {
        logger.warn(this + ": Exception on connect.", e);
        this.state = SessionState.EXCEPTION;
      } catch (Exception e) {
        logger.warn(this + ": Exception on connect.", e);
        this.state = SessionState.EXCEPTION;
      }
      return false;
    }
    
    /**
     * Writes the data to the output stream of the socket.
     * @param data The data to write.
     * @return True if written.
     */
    synchronized boolean write(byte[] data, long streamOffset) {
      if (this.state != SessionState.OPEN) {
        logger.warn(this + ": Invalid state when write = " + this.state);
        return false;
      }
      try {
        logger.debug( this + ": Writing " + data.length + " bytes: " + 
            new String(data));
        
        socket.getOutputStream().write(data);
        return true;
      } catch (IOException e) {
        logger.warn(this + ": Exception on write.", e);
        this.state = SessionState.EXCEPTION;
      }
      return false;
    }

    /**
     * Closes the connection of this session.
     * @return True if close succeeded.
     */
    synchronized boolean close() {
      if (socket != null) {
        try {
          // TODO:  Need to flush buffers and close off 
          // the input and output streams
          logger.debug( this + ": Closing socket.");
          socket.close();
          this.state = SessionState.CLOSED;
          return true;
        } catch (IOException e) {
          logger.warn(this + ": Exception on close.", e);
          this.state = SessionState.EXCEPTION;
        } finally {
          logger.debug( "Removing session " + handle.toStringUtf8());
          SocketSessionManager.this.sessions.remove(this.handle);
        }
      }
      return false;
    }
    
    void notifyCreateReplySent(SocketSessionReply reply) {
      
    }
    void notifyConnectReplySent(SocketSessionReply reply) {
      logger.debug( "Connect reply SENT. OK to READ INPUTSTREAM.");
      this.connectReplySent.set(true);
    }
    void notifyCloseReplySent(SocketSessionReply reply) {
      
    }
  }
  
  public boolean createSession(Sink<SocketSessionData> receiver,
      ByteString handle, InetSocketAddress endpoint) {
    Preconditions.checkArgument(!endpoint.isUnresolved());
    synchronized (sessions) {
      if (!sessions.containsKey(handle)) {
        Session session = new Session(receiver, handle, endpoint);
        sessions.put(handle, session);
      }
    }
    return true;
  }

  /**
   * Connects the socket identified by the handle.
   * @param handle The socket handle.
   * @return True if connect succeeded.
   */
  public boolean connect(ByteString handle) {
    Session session = sessions.get(handle);
    if (session != null) {
      return session.connect();
    }
    return false;
  }
  
  public boolean close(ByteString handle) {
    Session session = sessions.get(handle);
    if (session != null) {
      return session.close();
    }
    return false;
  }
  
  public void notifySent(ByteString handle, SocketSessionReply reply) {
    Session session = sessions.get(handle);
    if (session != null) {
      switch (reply.getVerb()) {
        case CREATE:
          session.notifyCreateReplySent(reply);
          break;
        case CONNECT:
          session.notifyConnectReplySent(reply);
          break;
        case CLOSE:
          session.notifyCloseReplySent(reply);
          break;
        default:
          break;
      }
    }
  }
  
  public boolean write(ByteString handle, byte[] data, long streamOffset) {
    Session session = sessions.get(handle);
    if (session != null) {
      return session.write(data, streamOffset);
    }
    return false;
  }
}
