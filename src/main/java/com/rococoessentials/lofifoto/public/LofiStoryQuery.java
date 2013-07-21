package com.rococoessentials.lofifoto.public;

import com.stackmob.core.InvalidSchemaException;
import com.stackmob.core.DatastoreException;
import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;

import com.rococoessentials.lofifoto.Util;

import com.stackmob.sdkapi.SDKServiceProvider;
import com.stackmob.sdkapi.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.HttpURLConnection;
import java.util.*;

/**
 * Return related user and story info, along with photos and other user stories for a given `sid`
 * that match the server stories_id field
 */

public class LofiStoryQuery implements CustomCodeMethod {

	@Override
	public String getMethodName() {
		return "LofiStoryQuery";
	}

	@Override
	public List<String> getParams() {
		return Arrays.asList("sid", "start", "end");
	}

	@Override
	public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
		Map<String, List<SMObject>> feedback = new HashMap<String, List<SMObject>>();
		Map<String, String> errMap = new HashMap<String, String>();

		String sid;
		String startIn;
		String endIn;
		
		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse(request.getBody());
			JSONObject jsonObject = (JSONObject) obj;
			
			// Fetch the values passed in by the user from the body of JSON
			sid = (String) jsonObject.get("sid");
			startIn = (String) jsonObject.get("start");
			endIn = (String) jsonObject.get("end");
		} catch (ParseException pe) {
			return Util.badRequestResponse(errMap);
		}
		
		if (Util.hasNulls(sid)){
			return Util.badRequestResponse(errMap);
		}

		long start;
		if (!Util.hasNulls(startIn)) {
			try {
				start = Long.valueOf(startIn).longValue();
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
		if (!Util.hasNulls(endIn)) {
			try {
				end = Long.valueOf(endIn).longValue();
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
		ResultFilters p_resultFilter = new ResultFilters(start, end, p_order, p_fields);

		List<SMOrdering> sa_order = Arrays.asList(new SMOrdering("last_updated", OrderingDirection.DESCENDING));
		List<String> sa_fields = Arrays.asList("stories_id", "name", "desc", "photo", "last_updated", "photocount");
		ResultFilters sa_resultFilter = new ResultFilters(0, -1, sa_order, sa_fields);

		List<SMCondition> p_query = new ArrayList<SMCondition>();
		List<SMCondition> s_query = new ArrayList<SMCondition>();
		List<SMCondition> sa_query = new ArrayList<SMCondition>();
		List<SMCondition> u_query = new ArrayList<SMCondition>();
		DataService ds = serviceProvider.getDataService();
		List<SMObject> results;

		try {
			s_query.add(new SMEquals("stories_id", new SMString(sid)));
			s_query.add(new SMEquals("state", new SMString("N")));
			results = ds.readObjects("stories", s_query, Arrays.asList("last_updated", "name", "desc", "photo", "sm_owner", "photocount"));
			
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

			// Create a query condition to match all story objects except for the `sid` that was passed in
			sa_query.add(new SMNotEqual("story_id", new SMString(sid)));
			sa_query.add(new SMEquals("state", new SMString("N")));
			sa_query.add(new SMEquals("sm_owner", userid));
			results = ds.readObjects("stories", sa_query, 0, sa_resultFilter);

			if (results != null && results.size() > 0) {
				feedback.put("stories", results);
			}

			// Create a query condition to match all photo objects to the `sid` that was passed in
			p_query.add(new SMEquals("story_id", new SMString(sid)));
			p_query.add(new SMEquals("state", new SMString("N")));
			results = ds.readObjects("photos", p_query, 0, p_resultFilter);

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