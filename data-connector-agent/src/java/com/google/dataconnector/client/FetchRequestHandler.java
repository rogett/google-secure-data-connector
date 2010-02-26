/* Copyright 2010 Google Inc.
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

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;
import com.google.dataconnector.client.fetchrequest.HttpFetchStrategy;
import com.google.dataconnector.client.fetchrequest.URLConnectionStrategy;
import com.google.dataconnector.protocol.Dispatchable;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.protocol.FramingException;
import com.google.dataconnector.protocol.proto.SdcFrame.FetchReply;
import com.google.dataconnector.protocol.proto.SdcFrame.FetchRequest;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.MessageHeader;
import com.google.dataconnector.util.ClockUtil;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Handler for the {@link FetchRequest} FrameInfo type.
 * @author dchung@google.com (David Chung)
 *
 */
public class FetchRequestHandler implements Dispatchable {

	/**
	 * Special header in the FetchRequest that is understood by the agent.
	 */
	public static final String DEBUG_HEADER = 
		  "x-sdc-agent-request-report-exception-stacktrace";

	/**
	 * Status code from processing the agent request.
	 */
	public enum StatusCode {
		OK(0),
		BAD_REQUEST(1),
		IO_EXCEPTION(2),
		STRATEGY_EXCEPTION(3),
		AGENT_ERROR(4);

		private int value;
		private StatusCode(int c) {
			value = c;
		}
	}
	
	/**
	 * Interface for handling specific type of FetchRequest.
	 */
	public interface Strategy {

		/**
		 * Given the request, fill in the results in the reply provided.
		 * 
		 * @param request The request.
		 * @param replyBuilder The reply to fill in.
		 * @throws StrategyException Any exception during processing.
		 */
		public void process(FetchRequest request, FetchReply.Builder replyBuilder) 
			  throws StrategyException;
	}

	private static Logger LOG = Logger.getLogger(FetchRequestHandler.class);

  // Injected Dependencies.
	private final ThreadPoolExecutor threadPoolExecutor;
  private final Injector injector;
  private final ClockUtil clock;
  
  // Runtime Dependencies.
	private FrameSender frameSender;
  
	/**
	 * Constructor with dependency on thread pool for asynchronous fetch and
	 * sending of replies.
	 * 
	 * @param threadPoolExecutor The thread pool.
	 * @param injector The injector.
	 */
	@Inject
	public FetchRequestHandler(ThreadPoolExecutor threadPoolExecutor, Injector injector,
			ClockUtil clock) {
		this.threadPoolExecutor = threadPoolExecutor;
		this.injector = injector;
		this.clock = clock;
	}

	public final void setFrameSender(FrameSender frameSender) {
		this.frameSender = frameSender;
	}

	/**
	 * Handles the request coming from the cloud.  In this case, fetch data from
	 * the requested resource in the {@link FetchRequest}.
	 * 
	 * @param frameInfo The container data frame.
	 */
	@Override
	public void dispatch(FrameInfo frameInfo) throws FramingException {
		Preconditions.checkNotNull(frameInfo);
		if (!frameInfo.hasPayload()) {
			LOG.info("No payload in received FrameInfo: " + frameInfo);
			return; // Nothing to do.
		}

		FetchRequest request = null;
		try {
			request = FetchRequest.parseFrom(frameInfo.getPayload());
		} catch (InvalidProtocolBufferException e) {
			throw new FramingException(e);
		}

		// Now we have the request.  Check the request:
		FetchReply.Builder replyBuilder = FetchReply.newBuilder().setId(request.getId());
		
		try {
			validate(request);
		} catch (IllegalArgumentException e) {
			logExceptionInReply(request, replyBuilder, e);
			sendReply(replyBuilder.setStatus(StatusCode.BAD_REQUEST.value).build());
			LOG.warn(request.getId() + ": Bad request: " + request, e);
			throw new FramingException(e);
		} catch (MalformedURLException e) {
			logExceptionInReply(request, replyBuilder, e);
			sendReply(replyBuilder.setStatus(StatusCode.BAD_REQUEST.value).build());
			LOG.warn("Bad request: " + request, e);
			throw new FramingException(e);
		}

		// Now execute work asynchronously.
		try {
			StrategyType strategyType = StrategyType.match(request.getStrategy());
			Strategy strategy = injector.getInstance(strategyType.strategyClz);
			ResourceFetcher fetcher = new ResourceFetcher(request, strategy);
			threadPoolExecutor.submit(fetcher);
		} catch (Exception e) {
			LOG.warn(request.getId() + ": Agent error: " + request, e);
			throw new FramingException(e);
		}
	}

	/**
	 * Simple enum defined to map the FetchRequest's scheme field to an enum
	 * and a strategy class.
	 */
	public enum StrategyType {
		
		URL_CONNECTION("URLConnection", URLConnectionStrategy.class),
		
		HTTP_CLIENT("HttpClient", HttpFetchStrategy.class);
		
		private String scheme;
		private Class<? extends Strategy> strategyClz;
		
		private StrategyType(String scheme, Class<? extends Strategy> clz) {
			this.scheme = scheme;
			this.strategyClz = clz;
		}
		
		/**
		 * Given the scheme expression, find the strategy that matches it.
		 * @param scheme The scheme from the FetchRequest.
		 * @return The strategy type.
		 */
		public static StrategyType match(String scheme) {
			if (scheme != null) {
				for (StrategyType st : StrategyType.values()) {
					if (st.scheme.matches(scheme)) {
						return st;
					}
				}
			}
			// Default is HttpClient
			return HTTP_CLIENT;
		}
	}
		
	/**
	 * Class that performs the actual fetching of the resource.
	 */
	class ResourceFetcher implements Callable<FetchReply> {

		private final FetchRequest request;
		private final Strategy strategy;
		private FetchReply reply;

		/**
		 * Constructs an instance to fetch the specified resource URL.
		 */
		ResourceFetcher(FetchRequest request, Strategy strategy) {
			this.request = request;
			this.strategy = strategy;
		}

		/**
		 * Fetch the resource specified at creation of the fetcher.
		 */
		@Override
		public FetchReply call() {
			// Initialize the reply, etc.
			StatusCode statusCode = StatusCode.OK;
			FetchReply.Builder replyBuilder = FetchReply.newBuilder();
			replyBuilder.setId(request.getId());

			Exception exception = null;
			try {

				long start = clock.currentTimeMillis();
				strategy.process(request, replyBuilder);
				replyBuilder.setLatency(clock.currentTimeMillis() - start);
				
				FetchReply reply = replyBuilder.setStatus(statusCode.value).build(); 
				sendReply(reply);
				return reply;
				
			} catch (StrategyException e) {
				exception = e;
				replyBuilder.setStatus(StatusCode.STRATEGY_EXCEPTION.value);
				logExceptionInReply(request, replyBuilder, e);
				sendReply(reply);
			} catch (Exception e) {
				// Do not send reply.
				exception = e;
			} 
			LOG.warn(request.getId() + ": Exception while fetching " + request, exception);
			FetchReply reply = replyBuilder.setStatus(statusCode.value).build(); 
			return reply;
		}

		@Override
		public String toString() {
			return String.format("ResourceFetcher(request=%s,reply=%s)", 
					this.request, this.reply);
		}
	}

	/**
	 * If the request contains a special header for logging exception, send the
	 * stacktrace back as a reply header.
	 * 
	 * @param request The request.
	 * @param replyBuilder Reply builder.
	 * @param ex The exception to log.
	 */
	void logExceptionInReply(FetchRequest request,
			FetchReply.Builder replyBuilder, Exception ex) {
		if (containsDebugHeader(request)) {
			CharArrayWriter cw = new CharArrayWriter();
			ex.printStackTrace(new PrintWriter(cw));
			cw.flush();
			// add a debug stack trace to reply header
			replyBuilder.addHeaders(MessageHeader.newBuilder()
					.setKey(DEBUG_HEADER)
					.setValue(cw.toString()).build());
		}
	}

	/**
	 * Asynchronously sends the reply to the cloud.
	 * @param reply The reply.
	 */
	void sendReply(FetchReply reply) {
		Preconditions.checkNotNull(frameSender);
		frameSender.sendFrame(FrameInfo.Type.FETCH_REQUEST, reply.toByteString());
	}

	/**
	 * Validates the incoming request.
	 * @param request The request.
	 * @throws IllegalArgumentException
	 * @throws MalformedURLException
	 */
	void validate(FetchRequest request) 
		throws IllegalArgumentException, MalformedURLException {
		// Sanity check for request id and resource.
		Preconditions.checkArgument(request.hasId() && request.getId().length() > 0);
		Preconditions.checkArgument(request.hasResource());
		Preconditions.checkArgument(request.getResource().length() > 0);
		// Now get the resource as URL:
		new URL(request.getResource());
	}

	/**
	 * Returns true if a debug header is in the request.
	 * @param request The request.
	 * @return True if request has debug header.
	 */
	boolean containsDebugHeader(FetchRequest request) {
		return headerMatchesValue(request, DEBUG_HEADER, "true");
	}

	/**
	 * Given the header key, returns true if the value exists and matches the 
	 * string provided.
	 * 
	 * @param key The key.
	 * @param matchExp The value/ expression to match.
	 * @return True if header exists AND matches the expression, case insensitive.
	 */
	boolean headerMatchesValue(FetchRequest request, String key, String matchExp) {
		for (MessageHeader h : request.getHeadersList()) {
			if (key.equals(h.getKey()) && 
					h.getValue().toLowerCase().matches(matchExp.toLowerCase())) {
				return true;
			}
		}
		return false;
	}
}
