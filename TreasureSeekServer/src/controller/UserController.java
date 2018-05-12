package controller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

import org.json.JSONObject;

import main.DBOperations;
import model.Treasure;
import model.User;
import util.Utils.Pair;

public class UserController {
	
	private static final String FACEBOOK_API_ADDRES = "https://graph.facebook.com/v2.11/"; 
	
	private DBOperations dbOperations;
	
	
	public UserController(DBOperations dbOperations) {
		this.dbOperations = dbOperations;
	}



	public User loginUser(String token) {
		
		HttpURLConnection  facebookConneciton = null;
		String urlString = FACEBOOK_API_ADDRES + "me?fields=name,email&access_token=" + token; 
		
		try {
		    //Create connection
		    URL url = new URL(urlString);
		    facebookConneciton = (HttpURLConnection) url.openConnection();
		    facebookConneciton.setRequestMethod("GET");
 

		    //Get Response  
		    InputStream is = facebookConneciton.getInputStream();
		    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		    //StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
		    
		    Scanner scanner = new Scanner(rd);
		    
		    String responseString = scanner.nextLine();
		    

		    rd.close();
		    
		    //System.out.println(response);
		    
		    JSONObject userInfo = new JSONObject(responseString);
		    
		    if(userInfo.has("error")) {
		    		
		    		scanner.close();
		    		return null;
		    		

		    }
		    
		    System.out.println(userInfo.toString());
		    
		    User user = dbOperations.getUser(true, Long.parseLong(userInfo.getString("id")));
		    
		    if(user == null)
		    		user = dbOperations.insertUser(true, userInfo.getLong("id"), userInfo.getString("email"),  token, userInfo.getString("name"));
		    else
		    		dbOperations.updateUser(true, (long)user.getValue("id"), token);
		    		    		    
		    scanner.close();
		    return user;
		    
		 
		} catch (Exception e) {
		 
			e.printStackTrace();
	  
		} finally {
		  
		    if (facebookConneciton != null) {
		    		facebookConneciton.disconnect();
		    }
	    
		}
		
		return null;
		
	}
		
	
	public boolean logoutUser(long id, String token) {
		
		try {
			
			User user = dbOperations.getUser(true, id);
			
			if(user == null)
				return false;
						
			if(!token.equals(user.getValue("token")))
				return false;
			
			return dbOperations.updateUser(true, id, "");
			
			
			
		} catch (RemoteException | SQLException e) {
			
			return false;
		}
				
	}
	
	public Pair<ArrayList<Treasure>,ArrayList<Treasure>> getAllTreasures(long userId) {
		
		try {
			return dbOperations.getAllTreasuresWithFoundInfo(userId);
		}
		catch(RemoteException | SQLException e) {
			return null;
		}
	}
	
	public boolean validateTreasure(int treasureId, String answer, String token, long userId) {
		
		try {
			
			User user = dbOperations.getUser(true, userId);
			
			if(user == null)
				return false;
			
						
			if(!token.equals(user.getValue("token")))
				return false;
			
			boolean isCorrectAnswer = dbOperations.validateTreasure(treasureId, answer);
			
			
			
			if(!isCorrectAnswer)
				return false;
						
			return dbOperations.insertFoundTreasure(treasureId, userId);
			
			
			
		} catch (RemoteException | SQLException e) {
			
			return false;
		}
		
		
		
	}
	
	
}
