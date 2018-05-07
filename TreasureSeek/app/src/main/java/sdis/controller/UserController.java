package sdis.controller;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.facebook.AccessToken;

import org.json.JSONException;
import org.json.JSONObject;

import sdis.communications.ClientMessage;
import sdis.model.GlobalModel;
import sdis.model.Model;
import sdis.model.User;
import sdis.treasureseek.R;
import sdis.util.ParseMessageException;

public class UserController {

    private User userModel = GlobalModel.getInstance().getUserModel();
    private SharedPreferences preferences;

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

        message =  ClientMessage.buildRequestMessage(ClientMessage.MessageType.LOGIN, Model.ModelType.USER.getResourceName() ,json);

        return message;
    }

    public String buildLogoutMessage(AccessToken token) {

        return "";
    }

}
