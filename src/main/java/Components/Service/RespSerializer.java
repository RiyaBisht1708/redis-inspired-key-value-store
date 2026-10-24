package Components.Service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class RespSerializer {

    private static final int MAX_ARRAY_LENGTH = 10_000;
    private static final int MAX_BULK_STRING_LENGTH = 1_000_000;

    public String serializeBulkString(String value) {
        if (value == null) {
            return "$-1\r\n";
        }

        int byteLength = value.getBytes(StandardCharsets.UTF_8).length;
        return "$" + byteLength + "\r\n" + value + "\r\n";
    }

    /**
     * Decodes as many complete RESP arrays as are present in {@code command}.
     *
     * If the final command is incomplete, throws IncompleteRespException.
     * The exception carries any earlier complete commands and the byte offset
     * where the incomplete command starts, allowing a TCP server to retain
     * just the unfinished portion of its input buffer.
     */
    public List<String[]> deseralize(byte[] command) {
        List<String[]> result = new ArrayList<>();

        if (command == null || command.length == 0) {
            return result;
        }

        int i = 0;

        while (i < command.length) {
            int commandStart = i;

            if (command[i] != '*') {
                throw new IllegalArgumentException(
                        "Expected RESP array at byte index " + i
                );
            }
            i++;

            int arrayHeaderEnd = findCrlf(command, i);
            if (arrayHeaderEnd == -1) {
                throwIfMalformedPartialNumber(command, i, command.length, "RESP array length");
                throw new IncompleteRespException(
                        "Incomplete array header", result, commandStart
                );
            }

            int arrayLength = parseNonNegativeNumber(
                    command, i, arrayHeaderEnd, MAX_ARRAY_LENGTH, "RESP array length"
            );
            if (arrayLength == 0) {
                throw new IllegalArgumentException("RESP command array cannot be empty");
            }
            i = arrayHeaderEnd + 2;

            String[] parts = new String[arrayLength];

            for (int partIndex = 0; partIndex < arrayLength; partIndex++) {
                if (i >= command.length) {
                    throw new IncompleteRespException(
                            "Incomplete bulk-string header", result, commandStart
                    );
                }

                if (command[i] != '$') {
                    throw new IllegalArgumentException(
                            "Expected RESP bulk string at byte index " + i
                    );
                }
                i++;

                int bulkHeaderEnd = findCrlf(command, i);
                if (bulkHeaderEnd == -1) {
                    throwIfMalformedPartialNumber(
                            command, i, command.length, "bulk-string length"
                    );
                    throw new IncompleteRespException(
                            "Incomplete bulk-string header", result, commandStart
                    );
                }

                int bulkLength = parseNonNegativeNumber(
                        command, i, bulkHeaderEnd,
                        MAX_BULK_STRING_LENGTH, "bulk-string length"
                );
                i = bulkHeaderEnd + 2;

                // Bulk-string lengths count bytes, not Java characters.
                if (command.length - i < bulkLength + 2L) {
                    throw new IncompleteRespException(
                            "Incomplete bulk-string payload", result, commandStart
                    );
                }

                int payloadEnd = i + bulkLength;
                if (command[payloadEnd] != '\r' || command[payloadEnd + 1] != '\n') {
                    throw new IllegalArgumentException(
                            "Missing bulk-string terminator at byte index " + payloadEnd
                    );
                }

                parts[partIndex] = new String(
                        command, i, bulkLength, StandardCharsets.UTF_8
                );
                i = payloadEnd + 2;
            }

            result.add(parts);
        }

        return result;
    }

    private int findCrlf(byte[] data, int start) {
        for (int i = start; i + 1 < data.length; i++) {
            if (data[i] == '\r' && data[i + 1] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private void throwIfMalformedPartialNumber(
            byte[] data, int start, int end, String description
    ) {
        for (int i = start; i < end; i++) {
            byte value = data[i];
            if (value == '\r' && i == end - 1) {
                continue; // A CR at the end may be followed by LF in the next TCP read.
            }
            if (value < '0' || value > '9') {
                throw new IllegalArgumentException("Invalid " + description);
            }
        }
    }

    private int parseNonNegativeNumber(
            byte[] data, int start, int end, int maxValue, String description
    ) {
        if (start == end) {
            throw new IllegalArgumentException("Missing " + description);
        }

        long value = 0;
        for (int i = start; i < end; i++) {
            byte digit = data[i];
            if (digit < '0' || digit > '9') {
                throw new IllegalArgumentException("Invalid " + description);
            }

            value = value * 10 + (digit - '0');
            if (value > maxValue) {
                throw new IllegalArgumentException(description + " exceeds maximum");
            }
        }

        return (int) value;
    }

    public String respInteger(int value) {
        return ":" + value + "\r\n";
    }

    public String respArray(String[] command) {
        StringBuilder response = new StringBuilder();
        response.append('*').append(command.length).append("\r\n");

        for (String value : command) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            response.append('$').append(bytes.length).append("\r\n");
            response.append(value).append("\r\n");
        }

        return response.toString();
    }

    public String respArray(List<String> command) {
        StringBuilder response = new StringBuilder();
        response.append('*').append(command.size()).append("\r\n");

        for (String value : command) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            response.append('$').append(bytes.length).append("\r\n");
            response.append(value).append("\r\n");
        }

        return response.toString();
    }

    public String[] parseArray(String[] parts) {
        int length = Integer.parseInt(parts[0]);
        String[] command = new String[length];

        command[0] = parts[2];

        int index = 1;
        for (int i = 4; i < parts.length; i += 2) {
            command[index++] = parts[i];
        }

        return command;
    }
}
