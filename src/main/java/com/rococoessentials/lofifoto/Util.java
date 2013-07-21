package com.rococoessentials.lofifoto;

import com.stackmob.core.rest.ResponseToProcess;

import java.net.HttpURLConnection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: sid
 * Date: 3/12/13
 * Time: 12:09 PM
 */

public class Util {

  public static Boolean isEmpty(String str) {
    return (str == null || str.isEmpty());
  }

  public static Boolean hasNulls(String... strings){
    for (String s : strings){
      if(isEmpty(s)){
        return true;
      }
    }
    return false;
  }

  public static ResponseToProcess badRequestResponse(Map<String, String> map){
    map.put("error", "Please fill in all parameters correctly");
    return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, map);
  }

  public static ResponseToProcess badRequestResponse(Map<String, String> map, String message){
    map.put("error", message);
    return new ResponseToProcess(HttpURLConnection.HTTP_BAD_REQUEST, map);
  }

  public static ResponseToProcess internalErrorResponse(String message, Exception e, Map<String, String> map){
    map.put("error", message);
    map.put("detail", e.toString());
    return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, map);
  }

}
