package communications;

import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;

import util.ParseMessageException;

public class ReplyMessage {

	public static enum ReplyMessageStatus {
		
		OK("OK"),
		UNAUTHORIZED("UNAUTHORIZED"),
		RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND"),
		BAD_REQUEST("BAD_REQUEST");
		
		static final ArrayList<String> types = new ArrayList<String>(Arrays.asList("OK","UNAUTHORIZED","RESOURCE_NOT_FOUND","BAD_REQUEST"));
		
		public String description;
		
		ReplyMessageStatus(String description) {
			this.description = description;
		}
		
		static ReplyMessageStatus type(String text) throws ParseMessageException {
			
			switch(types.indexOf(text)) {
			
				case 0:
					return ReplyMessageStatus.OK;
				case 1:
					return ReplyMessageStatus.UNAUTHORIZED;
				case 2:
					return ReplyMessageStatus.RESOURCE_NOT_FOUND;
				case 3:
					return ReplyMessageStatus.BAD_REQUEST;
				default:
					throw new ParseMessageException("Invalid Protocol Action");
			
			}
					
		}
				
	}

	
	public static String buildResponseMessage(ReplyMessageStatus status) {
		
		return status.description;
	
	}
	
	public static String buildResponseMessage(ReplyMessageStatus status, JSONArray jsonBody) throws JSONException {
				
		return status.description + " " + jsonBody.toString();
		
	}
	
}
