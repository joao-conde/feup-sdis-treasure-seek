package sdis.communications;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;


import sdis.util.ParseMessageException;

public class ClientMessage {

    public static final String RESOURCE_PATH_SEPARATOR = "/";

    public enum MessageType {

        CREATE("CREATE"),
        UPDATE("UPDATE"),
        DELETE("DELETE"),
        RETRIEVE("RETRIEVE"),
        LOGIN("LOGIN"),
        LOGOUT("LOGOUT"),
        RETRIEVE_HOST("RETRIEVE_HOST");

        static final ArrayList<String> types = new ArrayList<String>(Arrays.asList("CREATE","UPDATE","DELETE","RETRIEVE","LOGIN","LOGOUT","RETRIEVE_HOST","NEW_SERVER", "RETRIEVE_HOST"));

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
                default:
                    throw new ParseMessageException("Invalid Protocol Action");

            }

        }

    }

    public static String buildRequestMessage(MessageType type) {
        return type.description;
    }


    public static String buildRequestMessage(MessageType type, String resourcePath, JSONObject jsonBody)  {

        return type.description + " " + resourcePath + " " + jsonBody.toString();

    }

    public static String buildRequestMessage(MessageType type, JSONObject jsonBody) {

        return type.description + " " + jsonBody.toString();

    }

    public static String buildRequestMessage(MessageType type, String resourcePath) {

        return type.description + " " + resourcePath;

    }




}
