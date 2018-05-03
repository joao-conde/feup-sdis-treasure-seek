package sdis.model;

public class FoundTreasure extends Model {

    public FoundTreasure() {
        super(ModelType.FOUND_TREASURE, new String[] {"userId","treasureId"});
    }
}
