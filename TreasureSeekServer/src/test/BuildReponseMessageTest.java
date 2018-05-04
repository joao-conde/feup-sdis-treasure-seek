package test;


import static org.junit.Assert.assertEquals;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;

import communications.ReplyMessage;
import communications.ReplyMessage.ReplyMessageStatus;

public class BuildReponseMessageTest {

	@Test
	public void createOkMessage() {
		
		String msg =  ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK);
		assertEquals("OK", msg);
		
	}
	
	@Test
	public void createNotAuthorizedMessage() {
		
		String msg =  ReplyMessage.buildResponseMessage(ReplyMessageStatus.UNAUTHORIZED);
		assertEquals("UNAUTHORIZED", msg);
		
	}
	
	@Test
	public void createResourceNotFoundMessage() {
		
		String msg =  ReplyMessage.buildResponseMessage(ReplyMessageStatus.RESOURCE_NOT_FOUND);
		assertEquals("RESOURCE_NOT_FOUND", msg);
		
	}
	
	@Test
	public void createBadRequestMessage() {
		
		String msg =  ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);
		assertEquals("BAD_REQUEST", msg);
		
	}
	
	@Test
	public void createOkMessageWithBody() throws JSONException {
		
		JSONArray json = new JSONArray("[{id:1, username: fump}, {id:2, username: cristianoronaldo}]");
		
		String msg =  ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK, json);
		assertEquals("OK [{\"id\":1,\"username\":\"fump\"},{\"id\":2,\"username\":\"cristianoronaldo\"}]", msg);
		
	}
	
}
