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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import main.DBOperations;
import model.Treasure;
import model.User;
import util.NotAuthorizedException;
import util.ResourceNotFoundException;
import util.Utils.Pair;

public class Controller {
	
	private static final String FACEBOOK_API_ADDRES = "https://graph.facebook.com/v2.11/"; 
	private ArrayList<String> dbServerHostAddresses = new ArrayList<>();

	public Controller(ArrayList<String> dbServerHostAddresses) {
		this.dbServerHostAddresses = dbServerHostAddresses;
	}


	public User loginUser(JSONObject msgBody, DBOperations remoteObject) throws JSONException {
		
		HttpURLConnection  facebookConneciton = null;
		
		String ipAddress = msgBody.getString("address");
		String token = msgBody.getString("token");
		String urlString = FACEBOOK_API_ADDRES + "me?fields=name,id,email&access_token=" + token; 
		
		try {
		    //Create connection
		    URL url = new URL(urlString);
		    facebookConneciton = (HttpURLConnection) url.openConnection();
		    facebookConneciton.setRequestMethod("GET");
 

		    //Get Response  
		    InputStream is = facebookConneciton.getInputStream();
		    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		    
		    Scanner scanner = new Scanner(rd);
		    
		    String responseString = scanner.nextLine();
		    

		    rd.close();
		    		    
		    JSONObject userInfo = new JSONObject(responseString);
		    
		    if(userInfo.has("error")) {
		    		
		    		scanner.close();
		    		return null;
		    		

		    }
		    		    
		    User user = remoteObject.getUser(Long.parseLong(userInfo.getString("id")));
		    
		    if(user == null)
		    		user = remoteObject.insertUser(userInfo.getLong("id"), userInfo.getString("email"), token, userInfo.getString("name"), ipAddress, dbServerHostAddresses);
		    else
		    		remoteObject.updateUser((long)user.getValue("id"), token, ipAddress, dbServerHostAddresses);
		    		    	
		    
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
		
	
	public boolean logoutUser(long id, String token, DBOperations remoteObject) {
		
		try {
			
			if(!validateUser(id, token, remoteObject))
				return false;
			
			return remoteObject.updateUser(id, "", "", dbServerHostAddresses);
			
			
			
		} catch (RemoteException | SQLException e) {
			
			return false;
		}
				
	}


	private boolean validateUser(long id, String token, DBOperations remoteObject)
			throws RemoteException, SQLException {
		
	
		User user = remoteObject.getUser(id);
		
		if(user == null)
			return false;
					
		if(!token.equals(user.getValue("token")))
			return false;
		
		return true;
	}
	
	public Pair<ArrayList<Treasure>,ArrayList<Treasure>> getAllTreasures(long userId, DBOperations remoteObject) {
		
		try {
			return remoteObject.getAllTreasuresWithFoundInfo(userId);
		}
		catch(RemoteException | SQLException e) {
			System.out.println(e.getLocalizedMessage());
			return null;
		}
	}

	public ArrayList<Pair<User,Integer>> getRanking(long userId, DBOperations remoteObject, String token) throws RemoteException, SQLException {
		
		if(!validateUser(userId, token, remoteObject))
			return null;

		try {
			return remoteObject.getRanking();
		}
		catch(RemoteException | SQLException e) {
			System.out.println(e.getLocalizedMessage());
			return null;
		}
	}
	
	public Pair<ArrayList<Treasure>,ArrayList<Treasure>> getAllTreasures(long userId, DBOperations remoteObject, String token) throws RemoteException, SQLException {
		
		if(!validateUser(userId, token, remoteObject))
			return null;
		
		return getAllTreasures(userId, remoteObject);
		
	}
	
	public Pair<Boolean,Treasure> validateTreasure(int treasureId, String answer, String token, long userId, DBOperations remoteObject) throws ResourceNotFoundException, NotAuthorizedException, RemoteException, SQLException {
		
			
			User user = remoteObject.getUser(userId);
			
			if(user == null)
				throw new ResourceNotFoundException();
			
						
			if(!token.equals(user.getValue("token")))
				throw new NotAuthorizedException();
			
			Treasure treasure = remoteObject.getTreasure(treasureId);
			
			if(treasure == null)
				throw new ResourceNotFoundException();
			
			String correctAnswer = (String)treasure.getValue("answer");
			
			String regex = ".*" + correctAnswer + ".*"; 
			Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(answer);
			
			boolean isCorrectAnswer = matcher.matches(); 
			
			if(!isCorrectAnswer)
				return new Pair<Boolean,Treasure>(false,treasure);
						
			return new Pair<Boolean,Treasure>(remoteObject.insertFoundTreasure(treasureId, userId, dbServerHostAddresses), treasure);
			
	}
	
	public boolean createTreasure(JSONObject msgBody, DBOperations remoteObject) throws RemoteException, SQLException, JSONException, ResourceNotFoundException, NotAuthorizedException{
		
		User user = remoteObject.getUser(msgBody.getLong("userCreatorId"));
		
		if(user == null) throw new ResourceNotFoundException();
		
		if(!msgBody.getString("token").equals(user.getValue("token"))) throw new NotAuthorizedException();
		
		boolean inserted = remoteObject.insertTreasure(msgBody.getDouble("latitude"), 
														msgBody.getDouble("longitude"), 
														msgBody.getLong("userCreatorId"), 
														msgBody.getString("description"),
														msgBody.getString("challenge"),
														msgBody.getString("answer"), dbServerHostAddresses);

		
		return inserted;
	}
	
	
	public ArrayList<String> getSubscribedUsersAddresses(DBOperations remoteObject) throws SQLException, RemoteException {
		return remoteObject.getSubscribedUsersAddress();	
	}
	
}
