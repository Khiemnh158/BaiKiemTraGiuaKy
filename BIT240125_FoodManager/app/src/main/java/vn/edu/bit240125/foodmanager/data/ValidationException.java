package vn.edu.bit240125.foodmanager.data;

public class ValidationException extends RuntimeException {
  public final String key;

  public ValidationException(String key) {
    super(key);
    this.key = key;
  }
}
