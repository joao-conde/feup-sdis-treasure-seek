package model;

public class User extends Model {

	public User() {
		super(ModelType.User, new String[] {"id","username","email","token","admin"});
	}

}
