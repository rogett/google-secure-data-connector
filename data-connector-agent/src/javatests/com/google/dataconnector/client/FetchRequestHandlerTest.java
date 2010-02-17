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

import java.net.MalformedURLException;
import java.util.concurrent.ThreadPoolExecutor;

import org.easymock.classextension.EasyMock;

import junit.framework.TestCase;

import com.google.dataconnector.client.FetchRequestHandler.StrategyType;
import com.google.dataconnector.protocol.proto.SdcFrame.FetchRequest;
import com.google.dataconnector.protocol.proto.SdcFrame.MessageHeader;
import com.google.dataconnector.util.ClockUtil;
import com.google.inject.Injector;

/**
 * Tests for the AgentRequest handler.
 * @author dchung
 */
public class FetchRequestHandlerTest extends TestCase {

	@Override
	protected void setUp() throws Exception {
	}

	public void testValidateFetchRequest() throws Exception {
		FetchRequest request = FetchRequest.newBuilder()
		 .setId("requestId").setStrategy("HttpClient").setResource("badUrl").build();
		
		FetchRequestHandler handler = new FetchRequestHandler(
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
