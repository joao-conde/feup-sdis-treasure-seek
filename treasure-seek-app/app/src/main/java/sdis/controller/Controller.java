package sdis.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Pair;

import com.facebook.AccessToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import sdis.util.TreasureSeekException;

import static android.content.Context.WIFI_SERVICE;


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
    private String loadBalancerAddress;

    private String currentAppServerAddress;
    private int currentAppServerPort;

    public static int LOAD_BALANCER_PORT = 6789;

    private Controller(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(context.getString(R.string.treasureSeekPreferences), Context.MODE_PRIVATE);
        this.connectionHelper = new ConnectionHelper(context);

    }

    private void setLoggedUser(JSONObject user) throws JSONException {

        long id = preferences.getLong(context.getString(R.string.userId), 0);
        String name = preferences.getString(context.getString(R.string.nameOfUser), "");
        String email = preferences.getString(context.getString(R.string.email), "");
        boolean admin = preferences.getBoolean(context.getString(R.string.isAdmin), false);

        JSONArray foundTreasuresJSON = user.getJSONArray("foundTreasures");
        ArrayList<Treasure> foundTreasures = new ArrayList<>();

        for(int i = 0; i < foundTreasuresJSON.length(); i++) {

            foundTreasures.add(new Treasure((JSONObject) foundTreasuresJSON.get(i)));

        }

        loggedUser = new User(id,name,email,AccessToken.getCurrentAccessToken().getToken(),admin, foundTreasures);

    }

    private void setLoggedUserFoundTreasures(JSONArray foundTreasuresJSON) throws JSONException {

        ArrayList<Treasure> foundTreasures = new ArrayList<>();

        for(int i = 0; i < foundTreasuresJSON.length(); i++) {

            foundTreasures.add(new Treasure((JSONObject) foundTreasuresJSON.get(i)));

        }

        loggedUser.setValue("foundTreasures", foundTreasures);

    }

    private String buildLoginMessage(boolean subscribeNotifications) {

        JSONObject json = new JSONObject();
        String token = AccessToken.getCurrentAccessToken().getToken();
        try {

            WifiManager wifiMgr = (WifiManager)  context.getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            int ip = wifiInfo.getIpAddress();
            
            int firstOctect = ip & 0xff;
            int secondOctect = ip >> 8 & 0xff;
            int thirdOctect = ip >> 16 & 0xff;
            int fourthOctect = ip >> 24 & 0xff;

            String address = subscribeNotifications ? String.format("%d.%d.%d.%d", firstOctect,secondOctect,thirdOctect,fourthOctect) : "";

            json.put("token", token);
            json.put("address",address);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return ClientMessage.buildRequestMessage(ClientMessage.MessageType.LOGIN, Model.ModelType.USER.getResourceName() ,json);

    }

    private String buildLogoutMessage(long id, String name) {

        JSONObject json = new JSONObject();
        String token = (String)loggedUser.getValue("token");
        try {
            json.put("token", token);
            json.put( "id", id);
            json.put("name", name);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  ClientMessage.buildRequestMessage(ClientMessage.MessageType.LOGOUT, Model.ModelType.USER.getResourceName() ,json);

    }

    private String buildFoundTreasureMessage(int treasureIndex, String answer) {

        JSONObject json = new JSONObject();
        String token = (String)loggedUser.getValue("token");
        Treasure treasure = treasures.get(treasureIndex);
        try {
            json.put("token", token);
            json.put( "userId", loggedUser.getValue("id"));
            json.put("name", loggedUser.getValue("name"));
            json.put( "treasureId", treasure.getValue("id"));
            json.put("answer", answer);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  ClientMessage.buildRequestMessage(ClientMessage.MessageType.CREATE, Model.ModelType.FOUND_TREASURE.getResourceName() ,json);


    }

    private String buildRequestAllTreasuresMessage() {

        JSONObject json = new JSONObject();
        String token = (String)loggedUser.getValue("token");
        try {
            json.put("token", token);
            json.put( "userId", loggedUser.getValue("id"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  ClientMessage.buildRequestMessage(ClientMessage.MessageType.RETRIEVE, Model.ModelType.TREASURE.getResourceName(), json);

    }

    private String buildNewTreasureMessage(String desc, String challenge, String answer, Double latitude, Double longitude) {

        JSONObject json = new JSONObject();
        String token = (String)loggedUser.getValue("token");
        Long userId = (Long)loggedUser.getValue("id");

        try {
            json.put("token",token);
            json.put("userCreatorId",userId);
            json.put("description",desc);
            json.put("challenge",challenge);
            json.put("answer",answer);
            json.put("latitude",latitude);
            json.put("longitude",longitude);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return ClientMessage.buildRequestMessage(ClientMessage.MessageType.CREATE, Model.ModelType.TREASURE.getResourceName() ,json);

    }

    public boolean getAvailableServer() throws IOException, JSONException, ParseMessageException, NoAvailableServer {

        Pair<String,Integer> result;

        ServerMessage reply = connectionHelper.sendMessage(ClientMessage.MessageType.RETRIEVE_HOST.description, loadBalancerAddress, LOAD_BALANCER_PORT);

        if(reply.getStatus() == ServerMessage.ReplyMessageStatus.BAD_REQUEST)
            throw new NoAvailableServer();

        String host  = reply.getBody().getJSONObject(0).getString("host");
        Integer port = reply.getBody().getJSONObject(0).getInt("port");
        result = new Pair<>(host,port);

        this.currentAppServerAddress = result.first;
        this.currentAppServerPort = result.second;

        return true;

    }

    public boolean loginToTreasureSeek(boolean subscribeNotifications) throws IOException, ParseMessageException, JSONException {

        ServerMessage reply =  connectionHelper.sendMessageOverSSL(buildLoginMessage(subscribeNotifications), currentAppServerAddress, currentAppServerPort);

        if(reply != null && reply.getStatus() == ServerMessage.ReplyMessageStatus.OK) {

            JSONObject user = (JSONObject) reply.getBody().get(0);
            JSONArray treasures = (JSONArray) reply.getBody().get(1);
            setTreasures(treasures);
            saveSession(user);
            return true;

        }

        return false;

    }

    public boolean logoutFromTreasureSeek() throws IOException, ParseMessageException, JSONException {

        String userName = (String)loggedUser.getValue("name");
        long id = (Long)loggedUser.getValue("id");

        ServerMessage reply =  connectionHelper.sendMessageOverSSL(buildLogoutMessage(id, userName), currentAppServerAddress, currentAppServerPort);

        if(reply != null && reply.getStatus() == ServerMessage.ReplyMessageStatus.OK) {
            deleteSession();
            return true;
        }

        return false;

    }

    public boolean sendFoundTreasure(int treasureIndex, String answer) throws JSONException, TreasureSeekException, IOException {

        ServerMessage reply = connectionHelper.sendMessageOverSSL(buildFoundTreasureMessage(treasureIndex,answer), currentAppServerAddress, currentAppServerPort);

        if(reply == null || reply.getStatus() != ServerMessage.ReplyMessageStatus.OK)
            throw new TreasureSeekException("Oops, Something Went Wrong connecting to Treasure Seek Server");

        JSONObject response = reply.getBody().getJSONObject(0);

        if(response.getBoolean("result")) {

            int id = response.getInt("id");
            String challenge = response.getString("challenge");
            String challengeAnswer = response.getString("answer");
            return saveFoundTreasure(id,challenge,challengeAnswer);

        }

        return false;


    }

    public ArrayList<Treasure> getAllTreasures() {

        return treasures;

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

        setLoggedUser(user);

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

        this.treasures = new ArrayList<>();

        for(int i = 0; i < array.length(); i++) {

            JSONObject treasure = (JSONObject) array.get(i);
            this.treasures.add(new Treasure(treasure));

        }

    }

    public void setLoadBalancerAddress(String loadBalancerAddress) {
        this.loadBalancerAddress = loadBalancerAddress;
    }

    public boolean saveFoundTreasure(int treasureId, String challenge, String answer) {

        int foundTreasureIndex = -1;
        Treasure foundTreasure = null;

        for(int i = 0; i < treasures.size(); i++) {

            Treasure treasure = treasures.get(i);

            if((int)treasure.getValue("id") == treasureId) {

                foundTreasureIndex = i;
                foundTreasure = treasure;
                break;

            }

        }

        if(foundTreasureIndex == -1)
            return false;

        treasures.remove(foundTreasureIndex);

        foundTreasure.setValue("challenge",challenge);
        foundTreasure.setValue("answer",answer);

        return ((ArrayList<Treasure>) loggedUser.getValue("foundTreasures")).add(foundTreasure);

    }

    public boolean insertNewTreasure(String desc, String challenge, String answer, Double latitude, Double longitude) throws JSONException, ParseMessageException, IOException {

        ServerMessage reply =  connectionHelper.sendMessageOverSSL(buildNewTreasureMessage(desc,challenge,answer,latitude,longitude), currentAppServerAddress, currentAppServerPort);

        if(reply != null && reply.getStatus() == ServerMessage.ReplyMessageStatus.OK) {

            return true;

        }

        return false;

    }

    public void addTreasure(Treasure treasure) {
        treasures.add(treasure);
    }

    public boolean requestAllTreasures() throws JSONException, ParseMessageException, IOException {

        String getAllTreasuresMessage = buildRequestAllTreasuresMessage();

        ServerMessage reply =  connectionHelper.sendMessageOverSSL(getAllTreasuresMessage, currentAppServerAddress, currentAppServerPort);

        if(reply != null && reply.getStatus() == ServerMessage.ReplyMessageStatus.OK) {

            JSONArray treasures = (JSONArray) reply.getBody().get(0);
            JSONArray foundTreasures = (JSONArray) reply.getBody().get(1);
            setTreasures(treasures);
            setLoggedUserFoundTreasures(foundTreasures);
            return true;

        }

        return false;

    }


    public JSONArray getRanking() throws JSONException, IOException, ParseMessageException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", loggedUser.getValue("id"));
        jsonObject.put("token", loggedUser.getValue("token"));

        String message = ClientMessage.buildRequestMessage(ClientMessage.MessageType.RETRIEVE,
                Model.ModelType.USER.getResourceName(),jsonObject);

        ServerMessage reply = connectionHelper.sendMessageOverSSL(message, currentAppServerAddress, currentAppServerPort);



        if(reply.getStatus() == ServerMessage.ReplyMessageStatus.OK) {

            return reply.getBody();

        }

        return new JSONArray();

    }

}
