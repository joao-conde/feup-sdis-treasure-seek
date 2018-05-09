package sdis.model;

public class User extends Model {

    public User(long id, String name, String email, String token, boolean admin) {
        super(ModelType.USER, new String[] {"id","email","token","name","admin"});

        this.setValue(this.fields[0], id);
        this.setValue(this.fields[1], email);
        this.setValue(this.fields[2], token);
        this.setValue(this.fields[3], name);
        this.setValue(this.fields[4], admin);

    }
}
