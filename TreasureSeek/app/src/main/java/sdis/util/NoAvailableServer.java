package sdis.util;

public class NoAvailableServer extends TreasureSeekException {
    public NoAvailableServer() {
        super("No available server");
    }
}
