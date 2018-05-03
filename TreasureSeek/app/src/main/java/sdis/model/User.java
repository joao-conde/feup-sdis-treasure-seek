package sdis.model;

public class User extends Model {

    public User() {
        super(ModelType.USER, new String[] {"id","username","email","token","admin"});
    }
}
