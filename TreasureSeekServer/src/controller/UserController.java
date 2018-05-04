package controller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONObject;

public class UserController {
	
	private static final String FACEBOOK_API_ADDRES = "https://graph.facebook.com/v2.11/"; 
	
	public static void loginUser(String token) {
		
		HttpURLConnection  facebookConneciton = null;
		String urlString = FACEBOOK_API_ADDRES + "me?fields=name,email&access_token=" + token; 
		
		try {
		    //Create connection
		    URL url = new URL(urlString);
		    facebookConneciton = (HttpURLConnection) url.openConnection();
		    facebookConneciton.setRequestMethod("GET");
 
//		    facebookConneciton.setUseCaches(false);
//		    facebookConneciton.setDoOutput(true);


		    //Get Response  
		    InputStream is = facebookConneciton.getInputStream();
		    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		    StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
		    
		    Scanner scanner = new Scanner(rd);
		    
		    String responseString = scanner.nextLine();
		    

		    rd.close();
		    
		    System.out.println(response);
		    
		    JSONObject userInfo = new JSONObject(responseString);
		    
		    System.out.println(userInfo.toString());
		    
		    scanner.close();
		    
		    
		  
		} catch (Exception e) {
		    e.printStackTrace();
	  } finally {
	    if (facebookConneciton != null) {
	    		facebookConneciton.disconnect();
	    }
	  }
	}
		
	
}
