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

import com.google.dataconnector.client.FetchRequestHandler.StrategyType;
import com.google.dataconnector.protocol.proto.SdcFrame.FetchReply;
import com.google.dataconnector.protocol.proto.SdcFrame.FetchRequest;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.MessageHeader;
import com.google.dataconnector.util.ClockUtil;
import com.google.dataconnector.util.SdcKeysManager;
import com.google.dataconnector.util.SessionEncryption;
import com.google.inject.Injector;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import java.net.MalformedURLException;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Tests for the AgentRequest handler.
 * @author dchung
 */
public class FetchRequestHandlerTest extends TestCase {

	@Override
	protected void setUp() throws Exception {
	}
	
	public void testGetFromFrameInfo() throws Exception {
	  FetchRequest request = FetchRequest.newBuilder()
	    .setId("requestId").setStrategy("HttpClient")
	    .setResource("http://www.google.com").build();
	  
      String sessionId = UUID.randomUUID().toString();

      SdcKeysManager sm = new SdcKeysManager();
      sm.storeSessionKey(sessionId,
          SessionEncryption.JCE_ALGO, SessionEncryption.newKeyBytes());
	  
	  FrameInfo frameInfo = FrameInfo.newBuilder()
	    .setSessionId(sessionId)
	    .setPayload(sm.getSessionEncryption().encrypt(request.toByteString())).build();

	  FetchRequestHandler handler = new FetchRequestHandler(
	      sm,
	      EasyMock.createMock(ThreadPoolExecutor.class),
	      EasyMock.createMock(Injector.class),
	      EasyMock.createMock(ClockUtil.class));

	  FetchRequest parsed = sm.getSessionEncryption().getFrom(frameInfo,
	      new SessionEncryption.Parse<FetchRequest>() {
	    public FetchRequest parse(ByteString s) throws InvalidProtocolBufferException {
	      return FetchRequest.parseFrom(s);
	    }
	  });
	  assertEquals(request, parsed);

      FrameInfo frameInfo2 = FrameInfo.newBuilder()
        .setSessionId(UUID.randomUUID().toString())
        .setPayload(sm.getSessionEncryption().encrypt(request.toByteString())).build();

      FetchRequest parsed2 = sm.getSessionEncryption().getFrom(frameInfo2,
          new SessionEncryption.Parse<FetchRequest>() {
        public FetchRequest parse(ByteString s) throws InvalidProtocolBufferException {
          return FetchRequest.parseFrom(s);
        }
      });
      assertNull(parsed2);

	}

    public void testSendReply() throws Exception {
      String sessionId = UUID.randomUUID().toString();

      SdcKeysManager sm = new SdcKeysManager();
      sm.storeSessionKey(sessionId,
          SessionEncryption.JCE_ALGO, SessionEncryption.newKeyBytes());

      FetchReply reply = FetchReply.newBuilder()
        .setId(UUID.randomUUID().toString())
        .setStatus(0).build();
      
      FetchRequestHandler handler = new FetchRequestHandler(
          sm,
          EasyMock.createMock(ThreadPoolExecutor.class),
          EasyMock.createMock(Injector.class),
          EasyMock.createMock(ClockUtil.class));
      
      FrameInfo frame = sm.getSessionEncryption().toFrameInfo(
          FrameInfo.Type.FETCH_REQUEST, reply);
      
      assertTrue(frame.hasSessionId());
      assertEquals(reply.toByteString(), sm.getSessionEncryption().decrypt(frame.getPayload()));
    }

	public void testValidateFetchRequest() throws Exception {
		FetchRequest request = FetchRequest.newBuilder()
		 .setId("requestId").setStrategy("HttpClient").setResource("badUrl").build();
		
		FetchRequestHandler handler = new FetchRequestHandler(
		        EasyMock.createMock(SdcKeysManager.class),
				EasyMock.createMock(ThreadPoolExecutor.class),
				EasyMock.createMock(Injector.class),
				EasyMock.createMock(ClockUtil.class));
		
		Exception ex = null;
		try {
			handler.validate(request);
		} catch (MalformedURLException e) {
			ex = e;
		}
		assertNotNull(ex);

		// With debug header
		request = FetchRequest.newBuilder()
		.setId("requestId").setStrategy("HttpClient").setResource("http://www.google.com")
		.addHeaders(MessageHeader.newBuilder()
				.setKey("x-sdc-agent-request-report-exception-stacktrace")
				.setValue("true")).build();
		
		ex = null;
		try {
			handler.validate(request);
			assertTrue(handler.containsDebugHeader(request));
		} catch (Exception e) {
			fail("Should be ok.");
		}

		request = FetchRequest.newBuilder()
		 .setId("").setStrategy("HttpClient").setResource("badUrl").build();
		
		handler = new FetchRequestHandler(
		        EasyMock.createMock(SdcKeysManager.class),
				EasyMock.createMock(ThreadPoolExecutor.class),
				EasyMock.createMock(Injector.class),
				EasyMock.createMock(ClockUtil.class));
		
		ex = null;
		try {
			handler.validate(request);
		} catch (IllegalArgumentException e) {
			ex = e;
		}
		assertNotNull(ex);
	}

	public void testStrategyMatching() throws Exception {
		assertEquals(StrategyType.HTTP_CLIENT, StrategyType.match(null));
		assertEquals(StrategyType.HTTP_CLIENT, StrategyType.match("HttpClient"));
		assertEquals(StrategyType.HTTP_CLIENT, StrategyType.match("Unknown"));
		assertEquals(StrategyType.URL_CONNECTION, StrategyType.match("URLConnection"));
	}
	
}
