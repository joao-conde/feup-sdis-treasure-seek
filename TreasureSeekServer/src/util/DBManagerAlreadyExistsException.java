package util;

public class DBManagerAlreadyExistsException extends TreasureSeekExcpetion {

	private static final long serialVersionUID = 1L;

	public DBManagerAlreadyExistsException() {
		
		super("DB Manager already exists");
		
	}
	
}
