package au.gov.nsw.records.digitalarchive.service;

import org.apache.commons.lang3.StringUtils;

public class PathProcessor {

	public static String getStringAfter(String input){
		return input;
	}
	
	public static String getStringAfterLast(String input, String lastString){
		return input;
	}
	
	public static String removeIfEndWith(String input, String remove){
		return input.replaceAll("[/]$", "");
	}
	
	public static String removeIfStartWith(String input){
		return input.replaceAll("^/+", "");
	}
	
	public static String getPathToNode(String urlPath){
		
		String subURL = StringUtils.removeStart(urlPath, "/");
		
		if (!StringUtils.isEmpty(subURL)){
			return StringUtils.substringAfter(subURL, "/");
  	}
		return "";
	}
	
	public static String getRepositoryName(String urlPath){
		String subURL = StringUtils.removeStart(urlPath, "/");
		if (!StringUtils.isEmpty(subURL)){
			return StringUtils.substringBefore(subURL, "/");
  	}
		return "";
	}

	public static String getNodeName(String urlPath){
		urlPath = StringUtils.removeEnd(urlPath, "/");
		return StringUtils.substringAfterLast(urlPath, "/");
	}
}
