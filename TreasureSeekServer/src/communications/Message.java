package communications;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.naming.InvalidNameException;

import util.ParseMessageException;
import util.Utils.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import model.Model;
import model.Model.ModelType;

public class Message {
	
	public static final String RESOURCE_PATH_SEPARATOR = "/"; 

	public static enum MessageType {
		
		CREATE("CREATE"),
		UPDATE("UPDATE"),
		DELETE("DELETE"),
		RETRIEVE("RETRIEVE"),
		LOGIN("LOGIN"),
		LOGOUT("LOGOUT"),
		RETRIEVE_HOST("RETRIEVE_HOST"),
		NEW_SERVER("NEW_SERVER");
		
		static final ArrayList<String> types = new ArrayList<String>(Arrays.asList("CREATE","UPDATE","DELETE","RETRIEVE","LOGIN","LOGOUT","RETRIEVE_HOST","NEW_SERVER"));
		
		public String description;
		
		MessageType(String description) {
			this.description = description;
		}
		
		static MessageType type(String text) throws ParseMessageException {
			
			switch(types.indexOf(text)) {
			
				case 0:
					return MessageType.CREATE;
				case 1:
					return MessageType.UPDATE;
				case 2:
					return MessageType.DELETE;
				case 3:
					return MessageType.RETRIEVE;
				case 4:
					return MessageType.LOGIN;
				case 5:
					return MessageType.LOGOUT;
				case 6:
					return MessageType.RETRIEVE_HOST;
				case 7:
					return MessageType.NEW_SERVER;

				default:
					throw new ParseMessageException("Invalid Protocol Action");
			
			}
					
		}
				
	}
	
	public static class MessageHeader {
		
		private MessageType messageType;
		private ArrayList<Pair<Model.ModelType,Integer>> resourcePath;
		
		private MessageHeader(MessageType messageType) {
			this.messageType = messageType;
		}
		
		private MessageHeader(MessageType messageType, ArrayList<Pair<Model.ModelType, Integer>> resource) {
			this(messageType);
			this.resourcePath = resource;
		}
		
		
		public MessageType getMessageType() {
			return messageType;
		}
		public ArrayList<Pair<Model.ModelType, Integer>> getResource() {
			return resourcePath;
		}

		public void setResourcePath(ArrayList<Pair<Model.ModelType, Integer>> resourcePath) {
			this.resourcePath = resourcePath;
		}
		
		
		
	}
	
	private MessageHeader header;
	private JSONObject body;
		
	private Message(MessageHeader header) {
		this.header = header;
	}
	
	private Message(MessageHeader header, JSONObject body) {
		this(header);
		this.body = body;
	}
	
	public MessageHeader getHeader() {
		return header;
	}

	public JSONObject getBody() {
		return body;
	}
	
	@Override
	public String toString() {
		
		String res = "";
		
		res += "\nMessage Header:";
		res += ("\n\tMessage Type: " + this.header.getMessageType());
		res += "\n\tMessage Resource Path: \n";
		
		for(Pair<ModelType,Integer> pair : this.header.resourcePath) {
			
			res += "\t\t Resource: ";
			res += pair.key;
			res += ", id : ";
			res += (pair.value + "\n");
			
		}
		
		res += "Message Body: \n"; 
		
		res += this.body.toString();
		
		return res;
		
	}
	
	/**
	 * Parse a raw message and builds a message object with everything needed 
	 * to identify resource and action to perform
	 *
	 * @param raw String 
	 * @return Message Object
	 * @throws ParseMessageException - Something wrong with message header
	 * @throws JSONException - Something wrong with message body
	 */

	public static Message parseMessage(String raw) throws ParseMessageException, JSONException {
		
		Message message;
		
		ByteArrayInputStream inputStream = new ByteArrayInputStream(raw.getBytes());
		Scanner messageScanner = new Scanner(inputStream);
				
		String messageTypeString = null;
		String resourcePathString = null;
		
		try {
			messageTypeString = messageScanner.next();
			
		}
		
		catch(NoSuchElementException e) {
			
			messageScanner.close();
			throw new ParseMessageException("Missing message portions");
			
		}
		
		
		/**
		 * message must have at least 2 parts
		 */
					
		MessageType messageType = MessageType.type(messageTypeString);
		MessageHeader header = new MessageHeader(messageType);

		
		if(messageType == MessageType.RETRIEVE_HOST) {
			message = new Message(header);
		}
		
		else {
			resourcePathString = messageScanner.next();
			ArrayList<Pair<Model.ModelType,Integer>> pathToResource = parsePath(resourcePathString);
			header.setResourcePath(pathToResource);
			message = new Message(header);
			
			if(messageType == MessageType.CREATE || messageType == MessageType.UPDATE || messageType == MessageType.LOGIN || messageType == MessageType.LOGOUT) {
				
				String jsonString = null;
				
				try {
					jsonString = messageScanner.nextLine();
					
				}
				
				catch(NoSuchElementException e) {
					messageScanner.close();
					throw new ParseMessageException("Missing message portions");
				}
				
				
				JSONObject body = new JSONObject(jsonString);
				message = new Message(header, body);
				
				
			}
			
		}
		
			
		messageScanner.close();
		return message;
		
	}
	
	
	
	
	/**
	 * Parse path location of the message
	 * 
	 * @param path: String representing path location
	 * @return Array with pair resource/id
	 * @throws ParseMessageException
	 */
	
	private static ArrayList<Pair<Model.ModelType,Integer>> parsePath(String path) throws ParseMessageException {
		
		String[] pathPortions = path.split(RESOURCE_PATH_SEPARATOR);
		
		ArrayList<Pair<Model.ModelType,Integer>> result = new ArrayList<>();
		
		int numberPortions = pathPortions.length;
		ModelType modelLevel1 = null;
		ModelType modelLevel2 = null;
		
		try {
			modelLevel1 = ModelType.type(pathPortions[0]);
			modelLevel2 = ModelType.type(pathPortions[2]);
		}
		catch (InvalidNameException e) {
			throw new ParseMessageException("Invalid resource");
		}
		
		catch (ArrayIndexOutOfBoundsException e) {}
		
		
		switch(numberPortions) {
		
			case 1:
				
				result.add(new Pair<Model.ModelType, Integer>(modelLevel1, -1));
				break;
				
			case 2:
								
				result.add(new Pair<Model.ModelType, Integer>(modelLevel1, Integer.parseInt(pathPortions[1])));
				break;
				
			case 3:
				
				result.add(new Pair<Model.ModelType, Integer>(modelLevel1, Integer.parseInt(pathPortions[1])));
				result.add(new Pair<Model.ModelType, Integer>(modelLevel2, -1));
				break;
				
			case 4:
								
				result.add(new Pair<Model.ModelType, Integer>(modelLevel1, Integer.parseInt(pathPortions[1])));
				result.add(new Pair<Model.ModelType, Integer>(modelLevel2, Integer.parseInt(pathPortions[3])));
				break;
				
			default:
				throw new ParseMessageException("Invalid resource path: wrong number of levels");
			
		}
				
		return result;
		
		
	}


	
	
	
	
}
