package junghun.studycicd.simulation;

public class SimulatedErrorException extends Exception {

    private final ErrorType errorType;

    public SimulatedErrorException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
