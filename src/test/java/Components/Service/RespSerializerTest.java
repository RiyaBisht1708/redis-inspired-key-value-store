package Components.Service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RespSerializerTest {

    private final RespSerializer respSerializer = new RespSerializer();

    @Test
    void testDeserializePing() {
        String ping = "*1\r\n$4\r\nPING\r\n";

        List<String[]> commands = respSerializer.deseralize(
                ping.getBytes(StandardCharsets.UTF_8)
        );

        assertEquals(1, commands.size());
        assertArrayEquals(new String[]{"PING"}, commands.get(0));
    }

    @Test
    void testDeserializeSetCommand() {
        String set = "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n";

        List<String[]> commands = respSerializer.deseralize(
                set.getBytes(StandardCharsets.UTF_8)
        );

        assertEquals(1, commands.size());
        assertArrayEquals(new String[]{"SET", "key", "value"}, commands.get(0));
    }

    @Test
    void testMultipleCommands() {
        String input =
                "*1\r\n$4\r\nPING\r\n" +
                "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n";

        List<String[]> commands = respSerializer.deseralize(
                input.getBytes(StandardCharsets.UTF_8)
        );

        assertEquals(2, commands.size());
        assertArrayEquals(new String[]{"PING"}, commands.get(0));
        assertArrayEquals(new String[]{"SET", "key", "value"}, commands.get(1));
    }

    @Test
    void testSerializeUnicodeBulkString() {
        String value = "café";
        String actual = respSerializer.serializeBulkString(value);
        int byteLength = value.getBytes(StandardCharsets.UTF_8).length;

        assertEquals("$" + byteLength + "\r\n" + value + "\r\n", actual);
    }

    @Test
    void testDeserializeUnicodePayloadUsesByteLength() {
        String encoded = respSerializer.respArray(new String[]{"ECHO", "café"});

        List<String[]> commands = respSerializer.deseralize(
                encoded.getBytes(StandardCharsets.UTF_8)
        );

        assertEquals(1, commands.size());
        assertArrayEquals(new String[]{"ECHO", "café"}, commands.get(0));
    }

    @Test
    void testIncompleteCommandAfterCompleteCommandPreservesCompletedCommand() {
        String ping = "*1\r\n$4\r\nPING\r\n";
        String partialSet = "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nva";
        byte[] input = (ping + partialSet).getBytes(StandardCharsets.UTF_8);

        IncompleteRespException exception = assertThrows(
                IncompleteRespException.class,
                () -> respSerializer.deseralize(input)
        );

        assertEquals(1, exception.getCompletedCommands().size());
        assertArrayEquals(
                new String[]{"PING"},
                exception.getCompletedCommands().get(0)
        );
        assertEquals(
                ping.getBytes(StandardCharsets.UTF_8).length,
                exception.getConsumedBytes()
        );
    }

    @Test
    void testConsumedOffsetIsByteBasedAfterUnicodeCommand() {
        String unicodeCommand = respSerializer.respArray(new String[]{"ECHO", "café"});
        String partialSet = "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nva";
        byte[] input = (unicodeCommand + partialSet).getBytes(StandardCharsets.UTF_8);

        IncompleteRespException exception = assertThrows(
                IncompleteRespException.class,
                () -> respSerializer.deseralize(input)
        );

        assertEquals(1, exception.getCompletedCommands().size());
        assertArrayEquals(
                new String[]{"ECHO", "café"},
                exception.getCompletedCommands().get(0)
        );
        assertEquals(
                unicodeCommand.getBytes(StandardCharsets.UTF_8).length,
                exception.getConsumedBytes()
        );
    }

    @Test
    void testIncompleteUnicodePayloadCanBeCompletedByNextRead() {
        String encoded = respSerializer.respArray(new String[]{"ECHO", "café"});
        byte[] allBytes = encoded.getBytes(StandardCharsets.UTF_8);
        byte[] firstChunk = java.util.Arrays.copyOf(allBytes, allBytes.length - 2);

        IncompleteRespException exception = assertThrows(
                IncompleteRespException.class,
                () -> respSerializer.deseralize(firstChunk)
        );
        assertTrue(exception.getCompletedCommands().isEmpty());
        assertEquals(0, exception.getConsumedBytes());

        byte[] secondChunk = java.util.Arrays.copyOfRange(
                allBytes, allBytes.length - 2, allBytes.length
        );
        byte[] combined = new byte[firstChunk.length + secondChunk.length];
        System.arraycopy(firstChunk, 0, combined, 0, firstChunk.length);
        System.arraycopy(secondChunk, 0, combined, firstChunk.length, secondChunk.length);

        List<String[]> commands = respSerializer.deseralize(combined);
        assertEquals(1, commands.size());
        assertArrayEquals(new String[]{"ECHO", "café"}, commands.get(0));
    }

    @Test
    void testSerializeNullBulkString() {
        assertEquals("$-1\r\n", respSerializer.serializeBulkString(null));
    }

    @Test
    void testMalformedInputIsRejected() {
        byte[] input = "invalid".getBytes(StandardCharsets.UTF_8);

        assertThrows(
                IllegalArgumentException.class,
                () -> respSerializer.deseralize(input)
        );
    }
}
