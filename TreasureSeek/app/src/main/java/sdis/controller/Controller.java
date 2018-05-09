package sdis.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import com.facebook.AccessToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import sdis.communications.ClientMessage;
import sdis.communications.ConnectionHelper;
import sdis.communications.ServerMessage;
import sdis.model.Model;
import sdis.model.Treasure;
import sdis.model.User;
import sdis.treasureseek.R;
import sdis.util.NoAvailableServer;
import sdis.util.ParseMessageException;


public class Controller {

    private static Controller instance;

    public static Controller getInstance() {
        return instance;
    }

    public static Controller getInstance(Context context) {

        if(instance == null)
            instance = new Controller(context);
        return instance;

    }

    private Context context;
    private SharedPreferences preferences;
    private User loggedUser;
    private ConnectionHelper connectionHelper;
    private ArrayList<Treasure> treasures = new ArrayList<>();

    public static int LOAD_BALANCER_PORT = 6789;


    private Controller(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(context.getString(R.string.treasureSeekPreferences), Context.MODE_PRIVATE);
        this.connectionHelper = new ConnectionHelper(context);

        if(isLogged())
            this.setLoggedUser();

    }

    private void setLoggedUser() {

        long id = preferences.getLong(context.getString(R.string.userId), 0);
        String name = preferences.getString(context.getString(R.string.nameOfUser), "");
        String email = preferences.getString(context.getString(R.string.email), "");
        boolean admin = preferences.getBoolean(context.getString(R.string.isAdmin), false);
        loggedUser = new User(id,name,email,AccessToken.getCurrentAccessToken().getToken(),admin);

    }

    private String buildLoginMessage(AccessToken token) {

        JSONObject json = new JSONObject();
        try {
            json.put("token", token.getToken());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return ClientMessage.buildRequestMessage(ClientMessage.MessageType.LOGIN, Model.ModelType.USER.getResourceName() ,json);

    }

    private String buildLogoutMessage(AccessToken token, long id, String name) {

        JSONObject json = new JSONObject();
        try {
            json.put("token", token.getToken());
            json.put( "id", id);
            json.put("name", name);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  ClientMessage.buildRequestMessage(ClientMessage.MessageType.LOGOUT, Model.ModelType.USER.getResourceName() ,json);

    }

    public  Pair<String, Integer> getAvailableServer(String loadBalancerHostAddress) throws IOException, JSONException, ParseMessageException, NoAvailableServer {

        Pair<String,Integer> result;

        ServerMessage reply = connectionHelper.sendMessage(ClientMessage.MessageType.RETRIEVE_HOST.description, loadBalancerHostAddress, LOAD_BALANCER_PORT);

        if(reply.getStatus() == ServerMessage.ReplyMessageStatus.BAD_REQUEST)
            throw new NoAvailableServer();

        String host  = reply.getBody().getJSONObject(0).getString("host");
        Integer port = reply.getBody().getJSONObject(0).getInt("port");
        result = new Pair<>(host,port);

        return result;

    }


    public ServerMessage loginToTreasureSeek(String serverAddress, int serverPort) throws IOException, ParseMessageException, JSONException {

        AccessToken token = AccessToken.getCurrentAccessToken();
        return connectionHelper.sendMessageOverSSL(buildLoginMessage(token), serverAddress, serverPort);

    }

    public ServerMessage logoutFromTreasureSeek(String serverAddress, int serverPort) throws IOException, ParseMessageException, JSONException {

        AccessToken token = AccessToken.getCurrentAccessToken();
        String userName = (String)loggedUser.getValue("name");
        long id = (Long)loggedUser.getValue("id");

        return connectionHelper.sendMessageOverSSL(buildLogoutMessage(token, id, userName), serverAddress, serverPort);

    }

    public void saveSession(JSONObject user) throws JSONException {

        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(context.getString(R.string.isLogged), true);
        editor.putString(context.getString(R.string.facebookToken), AccessToken.getCurrentAccessToken().getToken());
        editor.putString(context.getString(R.string.nameOfUser), user.getString("name"));
        editor.putString(context.getString(R.string.email), user.getString("email"));
        editor.putLong(context.getString(R.string.userId), user.getLong("id"));
        editor.putBoolean(context.getString(R.string.isAdmin), user.getBoolean("admin"));
        editor.commit();

        setLoggedUser();

    }

    public void deleteSession() {

        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(context.getString(R.string.isLogged), false);
        editor.remove(context.getString(R.string.facebookToken));
        editor.remove(context.getString(R.string.nameOfUser));
        editor.remove(context.getString(R.string.email));
        editor.remove(context.getString(R.string.userId));
        editor.commit();

        this.loggedUser = null;

    }

    public boolean isLogged() {

        return preferences.getBoolean(context.getString(R.string.isLogged), false);

    }

    public User getLoggedUser() {
        return loggedUser;
    }


    public void setTreasures(JSONArray array) throws JSONException {

        for(int i = 0; i < array.length(); i++) {

            this.treasures.add(new Treasure((JSONObject) array.get(i)));

        }

    }

}
