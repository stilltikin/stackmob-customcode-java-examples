/**
 * Copyright 2012-2013 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stackmob.example.crud;

import com.stackmob.core.InvalidSchemaException;
import com.stackmob.core.DatastoreException;
import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.example.Util;
import com.stackmob.sdkapi.SDKServiceProvider;
import com.stackmob.sdkapi.*;

import java.net.HttpURLConnection;
import java.util.*;

/**
 * This example will show a user how to write a custom code method
 * with one parameter `sid` that queries the `photos` schema for all objects
 * that match the given story_id field
 */

public class QueryByField implements CustomCodeMethod {

	@Override
	public String getMethodName() {
		return "CRUD_Query_By_Field";
	}

	@Override
	public List<String> getParams() {
		return Arrays.asList("sid", "start", "end");
	}

	@Override
	public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
		Map<String, List<SMObject>> feedback = new HashMap<String, List<SMObject>>();
		Map<String, String> errMap = new HashMap<String, String>();
		
		String sid = request.getParams().get("sid"); // get story ID
		if (Util.hasNulls(sid)){
			return Util.badRequestResponse(errMap);
		}
		
		long start;
		String val = request.getParams().get("start");  // get photo start offset (optional)
		if (!Util.hasNulls(val)) {
			try {
				start = Long.valueOf(val).longValue();
			} catch(NumberFormatException nfe) {
				return Util.internalErrorResponse("invalid start value", nfe, errMap);	// http 500 - internal server error
			}
			if(start < 0) {
				start = 0;
			}
		} else {
			start = 0;
		}
		
		long end;
		val = request.getParams().get("end");  // get photo end (optional)
		if (!Util.hasNulls(val)) {
			try {
				end = Long.valueOf(val).longValue();
			} catch(NumberFormatException nfe) {
				return Util.internalErrorResponse("invalid end value", nfe, errMap);	// http 500 - internal server error
			}
			if(end < 1) {
				end = -1;
			}
		} else {
			end = -1;
		}

		feedback.put("start", start);
		feedback.put("end", end);

		
		List<SMOrdering> qorder = Arrays.asList(new SMOrdering("taken", OrderingDirection.DESCENDING));
		List<String> qfields = Arrays.asList("photos_id", "caption", "back", "width", "height", "photo", "taken");

		ResultFilters resultFilter = new ResultFilters(start, end, qorder, qfields);
	
		List<SMCondition> query = new ArrayList<SMCondition>();
		DataService ds = serviceProvider.getDataService();
		List<SMObject> results;
		
		try {
			// Create a query condition to match all photo objects to the `sid` that was passed in
			query.add(new SMEquals("story_id", new SMString(sid)));
			query.add(new SMNotEqual("state", new SMString("D")));
			results = ds.readObjects("photos", query, 0, resultFilter);
			
			if (results != null && results.size() > 0) {
				feedback.put(sid, results);
			}
			
		} catch (InvalidSchemaException ise) {
			return Util.internalErrorResponse("invalid_schema", ise, errMap);	// http 500 - internal server error
		} catch (DatastoreException dse) {
			return Util.internalErrorResponse("datastore_exception", dse, errMap);	// http 500 - internal server error
		}
		
		return new ResponseToProcess(HttpURLConnection.HTTP_OK, feedback);
	}

}