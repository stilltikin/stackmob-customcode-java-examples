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
			if(end < 0) {
				end = -1;
			}
		} else {
			end = -1;
		}

		List<SMOrdering> p_order = Arrays.asList(new SMOrdering("taken", OrderingDirection.ASCENDING));
		List<String> p_fields = Arrays.asList("photos_id", "caption", "back", "width", "height", "photo", "taken");

		ResultFilters resultFilter = new ResultFilters(start, end, p_order, p_fields);

		List<SMCondition> p_query = new ArrayList<SMCondition>();
		List<SMCondition> s_query = new ArrayList<SMCondition>();
		List<SMCondition> u_query = new ArrayList<SMCondition>();
		DataService ds = serviceProvider.getDataService();
		List<SMObject> results;

		try {
			// get story info TODO: can't get photoCount for some reason??
			s_query.add(new SMEquals("stories_id", new SMString(sid)));
			s_query.add(new SMNotEqual("state", new SMString("D")));
			results = ds.readObjects("stories", s_query, Arrays.asList("last_updated", "name", "desc", "photo", "sm_owner"));
			
			SMString userid;
			if (results != null && results.size() > 0) {
				feedback.put("story", results);
				userid = (SMString) results.get(0).getValue().get("sm_owner");
			} else {
				return Util.internalErrorResponse("no matching story", new DatastoreException(sid), errMap);	// http 500 - internal server error
			}

			// get user info
			u_query.add(new SMEquals("sm_owner", userid));
			results = ds.readObjects("user", u_query, Arrays.asList("username"));
			
			if (results != null && results.size() > 0) {
				feedback.put("user", results);
			} else {
				return Util.internalErrorResponse("no matching user for story", new DatastoreException(userid.toString()), errMap);	// http 500 - internal server error
			}

			// Create a query condition to match all photo objects to the `sid` that was passed in
			p_query.add(new SMEquals("story_id", new SMString(sid)));
			p_query.add(new SMNotEqual("state", new SMString("D")));
			results = ds.readObjects("photos", p_query, 0, resultFilter);

			if (results != null && results.size() > 0) {
				feedback.put("photos", results);
			}

		} catch (InvalidSchemaException ise) {
			return Util.internalErrorResponse("invalid_schema", ise, errMap);	// http 500 - internal server error
		} catch (DatastoreException dse) {
			return Util.internalErrorResponse("datastore_exception", dse, errMap);	// http 500 - internal server error
		}

		return new ResponseToProcess(HttpURLConnection.HTTP_OK, feedback);
	}
}