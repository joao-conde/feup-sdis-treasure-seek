package sdis.communications;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;


import sdis.util.ParseMessageException;

public class ClientMessage {

    public static final String RESOURCE_PATH_SEPARATOR = "/";

    public static enum MessageType {

        CREATE("CREATE"),
        UPDATE("UPDATE"),
        DELETE("DELETE"),
        RETRIEVE("RETRIEVE"),
        LOGIN("LOGIN"),
        LOGOUT("LOGOUT");

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
                default:
                    throw new ParseMessageException("Invalid Protocol Action");

            }

        }

    }


    public static String buildRequestMessage(String type, String resourcePath, JSONObject jsonBody) throws JSONException, ParseMessageException {

        return MessageType.type(type).description + " " + resourcePath + " " + jsonBody.toString();

    }

    public static String buildRequestMessage(String type, JSONObject jsonBody) throws JSONException, ParseMessageException {

        return MessageType.type(type).description + " " + jsonBody.toString();

    }




}
