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
package com.google.dataconnector.client.fetchrequest;

import java.io.OutputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.easymock.classextension.EasyMock;

import junit.framework.TestCase;

import com.google.dataconnector.client.StrategyException;
import com.google.dataconnector.client.fetchrequest.HttpFetchStrategy;
import com.google.dataconnector.protocol.proto.SdcFrame.FetchReply;
import com.google.dataconnector.protocol.proto.SdcFrame.FetchRequest;

/**
 * Test case for HttpClient fetch.
 */
public class HttpFetchStrategyTest extends TestCase {

	/**
	 * Tests processing of response.
	 * @throws Exception
	 */
	public void testProcess() throws Exception {
		FetchRequest ar = FetchRequest.newBuilder()
			.setId("test1").setStrategy("HttpClient").setResource("http://www.google.com")
			.build();
		
		FetchReply.Builder builder = FetchReply.newBuilder();
		
		// Mock the response so we don't make any network calls.
		final int status = 200;
		final Header[] headers = new Header[5];
		for (int i = 0 ; i < headers.length; i++) {
			headers[i] = EasyMock.createMock(Header.class);
			EasyMock.expect(headers[i].getName()).andReturn("key" + i);
			EasyMock.expect(headers[i].getValue()).andReturn("val" + i);
			EasyMock.replay(headers[i]);
		}
		
		final HttpResponse resp = EasyMock.createMock(HttpResponse.class);
		final HttpEntity ent = EasyMock.createMock(HttpEntity.class);
		final StatusLine st = EasyMock.createMock(StatusLine.class);
		ent.writeTo((OutputStream)EasyMock.anyObject());
		EasyMock.expect(st.getStatusCode()).andReturn(status);
		EasyMock.expect(resp.getStatusLine()).andReturn(st);
		EasyMock.expect(resp.getEntity()).andReturn(ent);
		EasyMock.expect(resp.getAllHeaders()).andReturn(headers);
		EasyMock.replay(st, resp);

		HttpFetchStrategy s = new HttpFetchStrategy() {
			@Override
			HttpResponse getHttpResponse(FetchRequest request) throws StrategyException {
				// Mock the response
				return resp;
			}
		};
		s.process(ar, builder);

		assertFalse("Strategy shouldn't have to set the id.", builder.hasId());
		assertTrue("Must return a status.", builder.hasStatus());
		assertEquals(status, builder.getStatus());
		assertFalse("The mock has no contents.", builder.hasContents());
		assertEquals(headers.length, builder.getHeadersCount());
	}
}
