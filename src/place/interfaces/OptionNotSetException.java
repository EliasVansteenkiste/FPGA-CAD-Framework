package place.interfaces;

public class OptionNotSetException extends RuntimeException {
    private static final long serialVersionUID = -5376366096369010060L;

    public OptionNotSetException(String message) {
        super(message);
    }
}
