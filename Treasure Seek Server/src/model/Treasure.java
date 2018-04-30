package model;

public class Treasure extends Model {

	public Treasure() {
		super(ModelType.Treasure, new String[] {"id","name","latitude","longitude","description","userCreatorId"});
	}

}
