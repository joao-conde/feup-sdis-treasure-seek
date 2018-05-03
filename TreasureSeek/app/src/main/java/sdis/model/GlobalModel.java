package sdis.model;

public class GlobalModel {

    private static GlobalModel instance;

    public static GlobalModel getInstance() {

        if(instance == null)
            instance = new GlobalModel();
        return instance;
    }

    private User userModel = new User();

    public User getUserModel() {
        return userModel;
    }
}
