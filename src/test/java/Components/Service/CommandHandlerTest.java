package Components.Service;

import Components.Repository.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandHandlerTest {

    private Store store;
    private RespSerializer serializer;
    private CommandHandler handler;

    @BeforeEach
    void setUp() {
        serializer = new RespSerializer();
        store = new Store();
        store.respSerializer = serializer;

        handler = new CommandHandler();
        ReflectionTestUtils.setField(handler, "store", store);
        ReflectionTestUtils.setField(handler, "respSerializer", serializer);
    }

    @Test
    void deletesExistingKey() {
        store.set("name", "Riya");

        assertEquals(":1\r\n", handler.del(new String[]{"DEL", "name"}));
        assertEquals("$-1\r\n", store.get("name"));
    }

    @Test
    void missingKeyReturnsZero() {
        assertEquals(":0\r\n", handler.del(new String[]{"DEL", "missing"}));
    }

    @Test
    void deletingSameKeyTwiceReturnsZeroSecondTime() {
        store.set("name", "Riya");

        handler.del(new String[]{"DEL", "name"});

        assertEquals(":0\r\n", handler.del(new String[]{"DEL", "name"}));
    }

    @Test
    void deletesMultipleKeysAndCountsOnlyExistingKeys() {
        store.set("name", "Riya");
        store.set("city", "Dehradun");

        assertEquals(
                ":2\r\n",
                handler.del(new String[]{"DEL", "name", "city", "missing"})
        );
    }
}