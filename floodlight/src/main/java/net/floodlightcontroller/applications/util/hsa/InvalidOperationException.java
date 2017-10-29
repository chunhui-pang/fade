package net.floodlightcontroller.applications.util.hsa;

/**
 * Exception class: this exception would be thrown when the operation is invalid.
 * For example:
 * <ul>
 *     <li>Define a {@link TernaryArray} will length not times of 8</li>
 * </ul>
 */
public class InvalidOperationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4632010901201892639L;

	public InvalidOperationException() {
		super();
	}

	public InvalidOperationException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidOperationException(String message) {
		super(message);
	}

}
