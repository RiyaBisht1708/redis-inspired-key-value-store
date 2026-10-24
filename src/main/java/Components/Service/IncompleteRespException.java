
package Components.Service;

import java.util.List;

public class IncompleteRespException extends RuntimeException {
    private final List<String[]> completedCommands;
    private final int consumedBytes;

    public IncompleteRespException(String message) {
        this(message, List.of(), 0);
    }

    public IncompleteRespException(
            String message,
            List<String[]> completedCommands,
            int consumedBytes
    ) {
        super(message);
        this.completedCommands = List.copyOf(completedCommands);
        this.consumedBytes = consumedBytes;
    }

    public List<String[]> getCompletedCommands() {
        return completedCommands;
    }

    public int getConsumedBytes() {
        return consumedBytes;
    }
}
