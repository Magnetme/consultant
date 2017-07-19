package me.magnet.consultant;

public class ConsulException extends RuntimeException {

	private final int status;

	public ConsulException(int status, String message) {
		super(message);
		this.status = status;
	}

	public int getStatus() {
		return status;
	}
}
