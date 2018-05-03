package sdis.model;

public class Treasure extends Model {

    public Treasure() {
        super(ModelType.TREASURE, new String[] {"id","name","latitude","longitude","description","userCreatorId"});
    }
}
