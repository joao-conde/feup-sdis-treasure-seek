package sdis.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Treasure extends Model {

    public Treasure(JSONObject jsonObject) throws JSONException {
        this(jsonObject.getInt("id"),
                jsonObject.getDouble("latitude"),
                jsonObject.getDouble("longitude"),
                jsonObject.getString("description"));
    }

    public Treasure(int id, double lat, double lon, String desc) {
        super(ModelType.TREASURE, new String[] {"id","latitude","longitude","description"});

        this.setValue(this.fields[0], id);
        this.setValue(this.fields[1], lat);
        this.setValue(this.fields[2], lon);
        this.setValue(this.fields[3], desc);

    }
}
