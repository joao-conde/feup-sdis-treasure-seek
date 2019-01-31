package model;

public class FoundTreasure extends Model {

	private static final long serialVersionUID = -8340301245225905078L;

	public FoundTreasure() {
		super(ModelType.FOUND_TREASURE, new String[] {"userId","treasureId"});
	}

}
