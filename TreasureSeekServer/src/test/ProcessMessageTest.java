package test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import communications.Message;
import communications.Message.MessageType;
import model.Model.ModelType;
import util.ParseMessageException;

public class ProcessMessageTest {

	@Test
	public void processMessageInvalidAction() {
		
		String message = "GET users";
		
		try {
			Message.parseMessage(message.getBytes());
		}
		
		catch(ParseMessageException | JSONException e) {
			
			assertTrue(e instanceof ParseMessageException);
			assertEquals("Invalid Protocol Action",e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageCreateActionWithNoBody() {
		
		String message = "CREATE users";
		
		try {
			Message.parseMessage(message.getBytes());
		}
		
		catch(ParseMessageException | JSONException e) {
			
			assertTrue(e instanceof ParseMessageException);
			assertEquals("Missing message portions",e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageUpdateActionWithNoBody() {
		
		String message = "UPDATE users";
		
		try {
			Message.parseMessage(message.getBytes());
		}
		
		catch(ParseMessageException | JSONException e) {
			
			assertTrue(e instanceof ParseMessageException);
			assertEquals("Missing message portions",e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageInvalidResourcePath() {
		
		String message = "RETRIEVE users/1/foundTreasures/2/players";
		
		try {
			Message.parseMessage(message.getBytes());
		}
		
		catch(ParseMessageException | JSONException e) {
			
			assertTrue(e instanceof ParseMessageException);
			assertEquals("Invalid resource path: wrong number of levels",e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageInvalidResource() {
		
		String message = "RETRIEVE players/1";
		
		try {
			Message.parseMessage(message.getBytes());
		}
		
		catch(ParseMessageException | JSONException e) {
			
			assertTrue(e instanceof ParseMessageException);
			assertEquals("Invalid resource",e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageRetrieveAllUsers() {
		
		String message = "RETRIEVE users";
		
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(), MessageType.RETRIEVE);
			assertEquals(msg.getHeader().getResource().get(0).key, ModelType.USER);
			assertEquals(msg.getHeader().getResource().get(0).value, new Integer(-1));
				
		}
		
		catch(ParseMessageException | JSONException e) {
			
			System.out.println(e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageRetrieveSpecificUser() {
		
		String message = "RETRIEVE users/23";
		
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(), MessageType.RETRIEVE);
			assertEquals(msg.getHeader().getResource().get(0).key, ModelType.USER);
			assertEquals(msg.getHeader().getResource().get(0).value, new Integer(23));
				
		}
		
		catch(ParseMessageException | JSONException e) {
			
			System.out.println(e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageRetrieveHost() {
		
		String message = "RETRIEVE_HOST";
		
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(), MessageType.RETRIEVE_HOST);
				
		}
		
		catch(ParseMessageException | JSONException e) {
			
			System.out.println(e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageNewServer() {
		
		String message = "NEW_SERVER";
		
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(), MessageType.NEW_SERVER);
				
		}
		
		catch(ParseMessageException | JSONException e) {
			
			System.out.println(e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageCreateUser() {
		
		String message = "CREATE users {username:fump, email:joaopedrofump@gmail.com, token:fdsfsdg, admin:true}";
			
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(), MessageType.CREATE);
			assertEquals(msg.getHeader().getResource().get(0).key, ModelType.USER);
			assertEquals(msg.getHeader().getResource().get(0).value, new Integer(-1));
			
			JSONObject body = msg.getBody();
			
			assertEquals(body.get("username"), "fump");
			assertEquals(body.get("email"), "joaopedrofump@gmail.com");
			assertEquals(body.get("token"), "fdsfsdg");
			assertEquals(body.get("admin"), new Boolean(true));
					
		}
		
		catch(ParseMessageException | JSONException e) {
			
			System.out.println(e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageGetAllTreasures() {
		
		String message = "RETRIEVE treasures";
			
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(), MessageType.RETRIEVE);
			assertEquals(msg.getHeader().getResource().get(0).key, ModelType.TREASURE);
			assertEquals(msg.getHeader().getResource().get(0).value, new Integer(-1));
								
		}
		
		catch(ParseMessageException | JSONException e) {
			
			System.out.println(e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageRetrieveSpecificTreasure() {
		
		String message = "RETRIEVE treasures/5";
		
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(), MessageType.RETRIEVE);
			assertEquals(msg.getHeader().getResource().get(0).key, ModelType.TREASURE);
			assertEquals(msg.getHeader().getResource().get(0).value, new Integer(5));
				
		}
		
		catch(ParseMessageException | JSONException e) {
			
			System.out.println(e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageCreateTreasure() {
		
		String message = "CREATE treasures {name:mar de minas, latitude: 41.178273, longitude: -8.5977863, description: A good treasure, userCreatorId: 12}";
			
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(), MessageType.CREATE);
			assertEquals(msg.getHeader().getResource().get(0).key, ModelType.TREASURE);
			assertEquals(msg.getHeader().getResource().get(0).value, new Integer(-1));
			
			JSONObject body = msg.getBody();
			
			assertEquals(body.get("name"), "mar de minas");
			assertEquals(body.get("latitude"), new Double(41.178273));
			assertEquals(body.get("longitude"), new Double(-8.5977863));
			assertEquals(body.get("description"), "A good treasure");
			assertEquals(body.get("userCreatorId"), new Integer(12));
					
		}
		
		catch(ParseMessageException | JSONException e) {
			
			System.out.println(e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageGetAllFoundTreasuresFromSpecificUser() {
		
		String message = "RETRIEVE users/10/foundTreasures";
			
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(), MessageType.RETRIEVE);
			assertEquals(msg.getHeader().getResource().get(0).key, ModelType.USER);
			assertEquals(msg.getHeader().getResource().get(0).value, new Integer(10));
			assertEquals(msg.getHeader().getResource().get(1).key, ModelType.FOUND_TREASURE);
			assertEquals(msg.getHeader().getResource().get(1).value, new Integer(-1));
								
		}
		
		catch(ParseMessageException | JSONException e) {
			
			System.out.println(e.getMessage());
			
		}
		
	}
	
	@Test 
	public void processMessageGetSpecificFoundTreasuresFromSpecificUser() {
		
		String message = "RETRIEVE users/10/foundTreasures/10";
			
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(), MessageType.RETRIEVE);
			assertEquals(msg.getHeader().getResource().get(0).key, ModelType.USER);
			assertEquals(msg.getHeader().getResource().get(0).value, new Integer(10));
			assertEquals(msg.getHeader().getResource().get(1).key, ModelType.FOUND_TREASURE);
			assertEquals(msg.getHeader().getResource().get(1).value, new Integer(10));
								
		}
		
		catch(ParseMessageException | JSONException e) {
			
			System.out.println(e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageCreateFoundTreasure() {
		
		String message = "CREATE users/10/foundTreasures {id: 20}";
			
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(), MessageType.CREATE);
			assertEquals(msg.getHeader().getResource().get(0).key, ModelType.USER);
			assertEquals(msg.getHeader().getResource().get(0).value, new Integer(10));
			assertEquals(msg.getHeader().getResource().get(1).key, ModelType.FOUND_TREASURE);
			
			JSONObject body = msg.getBody();
			
			assertEquals(body.get("id"), new Integer(20));
					
		}
		
		catch(ParseMessageException | JSONException e) {
			
			System.out.println(e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageUpdateTreasure() {
		
		String message = "UPDATE treasures/10 {name:bar de minas, latitude: 41.178274}";
			
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(), MessageType.UPDATE);
			assertEquals(msg.getHeader().getResource().get(0).key, ModelType.TREASURE);
			assertEquals(msg.getHeader().getResource().get(0).value, new Integer(10));
			
			JSONObject body = msg.getBody();
			
			assertEquals(body.get("name"), "bar de minas");
			assertEquals(body.get("latitude"), new Double(41.178274));
					
		}
		
		catch(ParseMessageException | JSONException e) {
			
			System.out.println(e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageDeleteTreasure() {
		
		String message = "DELETE treasures/11";
			
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(), MessageType.DELETE);
			assertEquals(msg.getHeader().getResource().get(0).key, ModelType.TREASURE);
			assertEquals(msg.getHeader().getResource().get(0).value, new Integer(11));
					
		}
		
		catch(ParseMessageException | JSONException e) {
			
			System.out.println(e.getMessage());
			
		}
		
	}
	
	@Test
	public void processMessageLogin() {
		
		String message = "LOGIN  {token:af546cda34fc}";
		
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(),MessageType.LOGIN);
			assertEquals(msg.getBody().get("token"), "af546cda34fc");
			
			
		} catch (ParseMessageException | JSONException e) {}
		
	}
	
	@Test
	public void processMessageLogout() {
		
		String message = "LOGOUT  {token:af546cda34fc}";
		
		try {
			
			Message msg = Message.parseMessage(message.getBytes());
			
			assertEquals(msg.getHeader().getMessageType(),MessageType.LOGOUT);
			assertEquals(msg.getBody().get("token"), "af546cda34fc");
			
			
		} catch (ParseMessageException | JSONException e) {}
		
	}
	
	
	
	
		
}
