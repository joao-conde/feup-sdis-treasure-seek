package sdis.controller;

import com.facebook.AccessToken;

import org.json.JSONException;
import org.json.JSONObject;

import sdis.communications.ClientMessage;
import sdis.model.GlobalModel;
import sdis.model.User;
import sdis.util.ParseMessageException;

public class UserController {

    private User userModel = GlobalModel.getInstance().getUserModel();

    private static UserController instance;

    public static UserController getInstance() {

        if(instance == null)
            instance = new UserController();
        return instance;
    }

    public String buildLoginMessage(AccessToken token) {

        JSONObject json = new JSONObject();
        try {
            json.put("token", token.getToken());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String message  = null;


        try {
            message =  ClientMessage.buildRequestMessage(ClientMessage.MessageType.LOGIN.description, json);
        } catch (JSONException | ParseMessageException e) {
            e.printStackTrace();
        }

        return message;
    }


}
