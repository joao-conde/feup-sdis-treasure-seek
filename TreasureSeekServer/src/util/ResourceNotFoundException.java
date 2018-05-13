package util;

public class ResourceNotFoundException extends TreasureSeekExcpetion {


	private static final long serialVersionUID = -6799982225821279697L;

	public ResourceNotFoundException() {
		super("Resource not found");
	}

}
