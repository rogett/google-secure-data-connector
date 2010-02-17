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

import junit.framework.TestCase;

import com.google.dataconnector.client.fetchrequest.URLConnectionStrategy;
import com.google.dataconnector.protocol.proto.SdcFrame.FetchReply;
import com.google.dataconnector.protocol.proto.SdcFrame.FetchRequest;

/**
 * Test case for URLConnection.
 */
public class URLConnectionStrategyTest extends TestCase {

	/**
	 * This test requires network + availability. Therefore, it can be unreliable.
	 * @throws Exception
	 */
	public void testProcessSimple() throws Exception {
		FetchRequest ar = FetchRequest.newBuilder()
			.setId("test1").setStrategy("URLConnection").setResource("http://www.google.com")
			.build();
		
		FetchReply.Builder builder = FetchReply.newBuilder();
		
		URLConnectionStrategy s = new URLConnectionStrategy();
		s.process(ar, builder);

		assertFalse("Strategy shouldn't have to set the id.", builder.hasId());
		assertTrue("Must return a status.", builder.hasStatus());
		assertTrue("This url should have contents.", builder.hasContents());
	}
}
