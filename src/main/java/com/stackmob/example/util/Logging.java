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

package com.stackmob.example.util;

import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.example.Util;
import com.stackmob.sdkapi.LoggerService;
import com.stackmob.sdkapi.SDKServiceProvider;
import com.stackmob.sdkapi.SMInt;
import com.stackmob.sdkapi.SMString;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.HttpURLConnection;
import java.util.*;

/**
 * This example will show a user how to use the logger to print the values
 * of the parameters passed in by the PUT/POST operation.
 */

public class Logging implements CustomCodeMethod {

  @Override
  public String getMethodName() {
    return "Logging";
  }

  @Override
  public List<String> getParams() {
    return Arrays.asList("model", "make", "year");
  }

  @Override
  public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
    // Initialize logger to "Classnamehere.class" when calling serviceProvider.getLoggerService()
    LoggerService logger = serviceProvider.getLoggerService(Logging.class);
    Map<String, String> errMap = new HashMap<String, String>();

    String model = "";
    String make  = "";
    String year  = "";

    JSONParser parser = new JSONParser();

    /* The following try/catch block shows how to properly fetch parameters for PUT/POST operations
     * from the JSON request body. In order to get parameters from GET/DELETE operations,
     * you must use request.getParams() (see DeleteObject.java)
     */
    try {
      Object obj = parser.parse(request.getBody());
      JSONObject jsonObject = (JSONObject) obj;
      model = (String) jsonObject.get("model");
      make  = (String) jsonObject.get("make");
      year  = (String) jsonObject.get("year");

      logger.debug("Model: " + model);
      logger.debug("Make: " + make);
      logger.debug("Year: " + year.toString());
    } catch (ParseException pe) {
      // error("Message", Throwable)
      logger.error(pe.getMessage(), pe);
      return Util.badRequestResponse(errMap, pe.getMessage());
    }

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("model", new SMString(model));
    map.put("make", new SMString(make));
    map.put("year", new SMInt(Long.parseLong(year)));

    return new ResponseToProcess(HttpURLConnection.HTTP_OK, map);
  }

}
