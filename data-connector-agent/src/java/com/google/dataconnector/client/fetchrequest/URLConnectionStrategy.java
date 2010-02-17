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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Logger;

import com.google.dataconnector.client.StrategyException;
import com.google.dataconnector.client.FetchRequestHandler.Strategy;
import com.google.dataconnector.protocol.proto.SdcFrame.FetchReply;
import com.google.dataconnector.protocol.proto.SdcFrame.FetchRequest;
import com.google.protobuf.ByteString;

/**
 * Simple strategy of getting all the bytes from a URLConnection and send back
 * to the cloud.
 * 
 * @author dchung
 *
 */
public class URLConnectionStrategy implements Strategy {

	private static Logger LOG = Logger.getLogger(URLConnectionStrategy.class);

	public URLConnectionStrategy() {
		// Default constructor.
	}
	
	/**
	 * Implements the strategy method of processing the request and filling in the
	 * reply with results of processing.
	 * 
	 * @param request The request.
	 * @param reply The reply to fill in.
	 */
	@Override
	public void process(FetchRequest request, FetchReply.Builder replyBuilder) 
		throws StrategyException {
		
		try {
			// Perform the actual fetch here.
			URL resource = new URL(request.getResource());
			
			// Connect to resource.
			URLConnection conn = resource.openConnection();
			
			// Copy the result to reply buffer.
			ByteArrayOutputStream contents = new ByteArrayOutputStream();
			byte[] buffer = new byte[2048];
			int bytesRead = 0;
			int totalRead = 0;
			while ((bytesRead = conn.getInputStream().read(buffer)) > 0) {
				contents.write(buffer, 0, bytesRead);
				totalRead += bytesRead;
			}
			LOG.info("Read resource " + resource + ", bytes=" + totalRead);
			// finally set the content in the reply, if we have data
			if (totalRead > 0) {
				replyBuilder.setContents(ByteString.copyFrom(contents.toByteArray()));
			}
			replyBuilder.setStatus(0);
			
		} catch (MalformedURLException e) {
			throw new StrategyException(request.getId() + ": bad url.", e);
		} catch (IOException e) {
			throw new StrategyException(request.getId() + ": io exception.", e);
		}
	}
}
