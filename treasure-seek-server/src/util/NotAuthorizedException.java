package util;

public class NotAuthorizedException extends TreasureSeekExcpetion {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8692782441155677091L;

	public NotAuthorizedException() {
		super("Not authorized");
	}

}
