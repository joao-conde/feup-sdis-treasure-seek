package model;

public class User extends Model {

	private static final long serialVersionUID = 8364239647574512618L;

	public User() {
		super(ModelType.USER, new String[] {"id","email","token","admin"});
	}

}
