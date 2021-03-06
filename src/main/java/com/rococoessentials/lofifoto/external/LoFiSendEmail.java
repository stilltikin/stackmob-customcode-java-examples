
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

package com.rococoessentials.lofifoto.external;

import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.sdkapi.SDKServiceProvider;
import com.stackmob.sdkapi.*;

import com.stackmob.sdkapi.http.HttpService;
import com.stackmob.sdkapi.http.request.HttpRequest;
import com.stackmob.sdkapi.http.request.GetRequest;
import com.stackmob.sdkapi.http.response.HttpResponse;
import com.stackmob.core.ServiceNotActivatedException;
import com.stackmob.sdkapi.http.exceptions.AccessDeniedException;
import com.stackmob.sdkapi.http.exceptions.TimeoutException;
import com.stackmob.core.InvalidSchemaException;
import com.stackmob.core.DatastoreException;

import java.net.MalformedURLException;
import com.stackmob.sdkapi.http.request.PostRequest;
import com.stackmob.sdkapi.http.Header;
import com.stackmob.sdkapi.LoggerService;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

// Added JSON parsing to handle JSON posted in the body
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

import com.rococoessentials.lofifoto.Util;

public class LoFiSendEmail implements CustomCodeMethod {
	
	//Create your SendGrid Acct at sendgrid.com
    static String API_USER = "stackmob_fqQLkk3y55cZQqxKWcuUCQ@stackmob.com";
    static String API_KEY = "mzlpoms5";
	
	@Override
	public String getMethodName() {
		return "LoFiSendEmail";
	}
    
    
	@Override
	public List<String> getParams() {
		return Arrays.asList();
	}
    
	
	@Override
	public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
		int responseCode = 0;
		String responseBody = "";
		String username = "";
		String subject = "";
		String html = "";
		String from = "";
		String to = "";
		String toname = "";
		String body = "";
		String url = "";
		
		LoggerService logger = serviceProvider.getLoggerService(LoFiSendEmail.class);
		//Log the JSON object passed to the StackMob Logs
		//logger.debug(request.getBody());
		
		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse(request.getBody());
			JSONObject jsonObject = (JSONObject) obj;
			
			//We use the username passed to query the StackMob datastore
			//and retrieve the user's name and email address
			username = (String) request.getLoggedInUser();//jsonObject.get("username");

			// The following values could be static or dynamic
			subject = (String) jsonObject.get("subject");
			html = (String) jsonObject.get("html");
			from = (String) jsonObject.get("from");
		} catch (ParseException e) {
			logger.error("Failed to parse arguments: " + e.getMessage(), e);
			responseCode = -1;
			responseBody = "Failed to parse arguments: " + e.getMessage();
		}
		
		if (username == null || username.isEmpty()) {
			HashMap<String, String> errParams = new HashMap<String, String>();
			errParams.put("error", "the username passed was empty or null");
			return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, errParams); // http 400 - bad request
		}
    	
		// get the StackMob datastore service and assemble the query
		DataService dataService = serviceProvider.getDataService();
		
		// build a query
		List<SMCondition> query = new ArrayList<SMCondition>();
		query.add(new SMEquals("username", new SMString(username)));
		
		SMObject userObject;
		List<SMObject> result;
		try {
			// return results from user query
			result = dataService.readObjects("user", query);
			if (result != null && result.size() == 1) {
				userObject = result.get(0);
				
				if(userObject.getValue().get("email") == null) {
					logger.error("Missing email for " + username);
					HashMap<String, String> errParams = new HashMap<String, String>();
					errParams.put("error", "missing email address");
					return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, errParams); // http 400 - bad request
				} else {
					to = userObject.getValue().get("email").toString();
				}
				
				if(userObject.getValue().get("fullname") == null) {
					logger.info("Missing fullname for " + username);
				} else {
					toname = userObject.getValue().get("fullname").toString();
				}
			} else {
				logger.error("Failed to retrieve data for " + username);
				HashMap<String, String> errMap = new HashMap<String, String>();
				errMap.put("error", "Query failed");
				errMap.put("detail", "Failed to retrieve data for " + username);
				return new ResponseToProcess(HttpURLConnection.HTTP_OK, errMap); // http 500 - internal server error
			}
			
		} catch (InvalidSchemaException e) {
			HashMap<String, String> errMap = new HashMap<String, String>();
			errMap.put("error", "invalid_schema");
			errMap.put("detail", e.toString());
			return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
		} catch (DatastoreException e) {
			HashMap<String, String> errMap = new HashMap<String, String>();
			errMap.put("error", "datastore_exception");
			errMap.put("detail", e.toString());
			return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
		} catch(Exception e) {
			HashMap<String, String> errMap = new HashMap<String, String>();
			errMap.put("error", "unknown");
			errMap.put("detail", e.toString());
			return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, errMap); // http 500 - internal server error
		}
		
		if (subject == null || subject.equals("")) {
			logger.error("Subject is missing");
		}
		
		//Encode any parameters that need encoding (i.e. subject, toname, html)
		try {
			subject = URLEncoder.encode(subject, "UTF-8");
			html = URLEncoder.encode(html, "UTF-8");
			toname = URLEncoder.encode(toname, "UTF-8");
			
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		}
		
		body = "api_user=" + API_USER + "&api_key=" + API_KEY + "&to=" + to + "&toname=" + toname + "&subject=" + subject + "&html=" + html + "&from=" + from;
		
		url =  "https://www.sendgrid.com/api/mail.send.json";
		
		Header accept = new Header("Accept-Charset", "utf-8");
		Header content = new Header("Content-Type", "application/x-www-form-urlencoded");
		
		Set<Header> set = new HashSet();
		set.add(accept);
		set.add(content);
		
		try {
			HttpService http = serviceProvider.getHttpService();
			
			PostRequest req = new PostRequest(url,set,body);
			
			HttpResponse resp = http.post(req);
			responseCode = resp.getCode();
			responseBody = resp.getBody();
			
		} catch(TimeoutException e) {
			logger.error(e.getMessage(), e);
			responseCode = -1;
			responseBody = e.getMessage();
			
		} catch(AccessDeniedException e) {
			logger.error(e.getMessage(), e);
			responseCode = -1;
			responseBody = e.getMessage();
			
		} catch(MalformedURLException e) {
			logger.error(e.getMessage(), e);
			responseCode = -1;
			responseBody = e.getMessage();
			
		} catch(ServiceNotActivatedException e) {
			logger.error(e.getMessage(), e);
			responseCode = -1;
			responseBody = e.getMessage();
		}
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("response_body", responseBody);

		logger.debug("Sent welcome email to " + username);

		return new ResponseToProcess(responseCode, map);
	}
}