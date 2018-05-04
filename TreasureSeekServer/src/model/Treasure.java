package model;

public class Treasure extends Model {

	private static final long serialVersionUID = -6622991695912044949L;

	public Treasure() {
		super(ModelType.TREASURE, new String[] {"id","name","latitude","longitude","description","userCreatorId"});
	}

}
