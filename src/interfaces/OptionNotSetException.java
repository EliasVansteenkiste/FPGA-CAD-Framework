package interfaces;

public class OptionNotSetException extends Exception {
    private static final long serialVersionUID = -5376366096369010060L;

    public OptionNotSetException(String message) {
        super(message);
    }
}
