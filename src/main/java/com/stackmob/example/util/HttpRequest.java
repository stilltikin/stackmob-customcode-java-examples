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

import com.stackmob.core.ServiceNotActivatedException;
import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.sdkapi.SDKServiceProvider;
import com.stackmob.sdkapi.*;
import com.stackmob.sdkapi.http.Header;
import com.stackmob.sdkapi.http.HttpService;
import com.stackmob.sdkapi.http.exceptions.AccessDeniedException;
import com.stackmob.sdkapi.http.exceptions.TimeoutException;
import com.stackmob.sdkapi.http.request.GetRequest;
import com.stackmob.sdkapi.http.response.HttpResponse;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.*;

/**
 * This example will show a user how to write a custom code method
 * that will perform an external http request to the httpbin.org
 * website.
 */

public class HttpRequest implements CustomCodeMethod {

  @Override
  public String getMethodName() {
    return "Http_Request";
  }

  @Override
  public List<String> getParams() {
    return new ArrayList<String>();
  }

  @Override
  public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
    LoggerService logger = serviceProvider.getLoggerService(HttpRequest.class);
    int responseCode = 0;
    String responseBody = "";

    // The service you're going to be using
    String url = "http://www.httpbin.org/get";
    // Formulate request headers
    Header accept = new Header("Accept-Charset", "utf-8");
    Header content = new Header("Content-Type", "application/x-www-form-urlencoded");

    Set<Header> set = new HashSet();
    set.add(accept);
    set.add(content);

    try {
      HttpService http = serviceProvider.getHttpService();

      /* In this Example we are going to be making a GET request
       * but PUT/POST/DELETE requests are also possible.
       */
      GetRequest req = new GetRequest(url,set);
      HttpResponse resp = http.get(req);

      responseCode = resp.getCode();
      responseBody = resp.getBody();

    } catch (ServiceNotActivatedException e) {
      logger.error(e.getMessage(), e);
      responseCode = HttpURLConnection.HTTP_UNAVAILABLE;
      responseBody = e.getMessage();
    } catch (MalformedURLException e) {
      logger.error(e.getMessage(), e);
      responseCode = HttpURLConnection.HTTP_NOT_FOUND;
      responseBody = e.getMessage();
    } catch (AccessDeniedException e) {
      logger.error(e.getMessage(), e);
      responseCode = HttpURLConnection.HTTP_UNAUTHORIZED;
      responseBody = e.getMessage();
    } catch (TimeoutException e) {
      logger.error(e.getMessage(), e);
      responseCode = HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
      responseBody = e.getMessage();
    }

    Map<String, String> feedback = new HashMap<String, String>();
    feedback.put("response_body", responseBody);

    return new ResponseToProcess(responseCode, feedback);
  }

}