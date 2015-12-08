package me.magnet.consultant;

public class ConsultantException extends RuntimeException {

	public ConsultantException(String message) {
		super(message);
	}

	public ConsultantException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public ConsultantException(Throwable throwable) {
		super(throwable);
	}

}
