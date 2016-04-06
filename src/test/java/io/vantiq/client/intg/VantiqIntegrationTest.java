package io.vantiq.client.intg;

import com.google.gson.JsonObject;
import io.vantiq.client.*;
import io.vantiq.client.internal.VantiqSession;
import okhttp3.Response;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertTrue;

/**
 * Mocked unit tests for the Vantiq
 */
@Category(IntegrationTests.class)
public class VantiqIntegrationTest {

    private Vantiq vantiq;
    private final UnitTestResponseHandler handler = new UnitTestResponseHandler();

    private static String server   = null;
    private static String username = null;
    private static String password = null;

    @BeforeClass
    public static void setUpIntgTest() throws Exception {
        // Pull values from java properties, so the credentials are not checked in
        server   = System.getProperty("server");
        username = System.getProperty("username");
        password = System.getProperty("password");

        if(server == null || username == null || password == null) {
            throw new IllegalStateException("Must set 'server', 'username', and 'password' Java System Properties");
        }
    }

    @Before
    public void setUp() throws Exception {
        vantiq = new Vantiq(server);
        vantiq.authenticate(username, password, handler);
        waitForCompletion();
        handler.reset();
    }

    @After
    public void tearDown() throws Exception {
        vantiq = null;
    }

    //------------------------------------------------------------------------------------
    // Helper methods
    //------------------------------------------------------------------------------------

    private void waitForCompletion() throws InterruptedException {
        synchronized (handler) {
            handler.wait(2000);
        }
    }

    private String getISOString(Date d) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(d);
    }

    //------------------------------------------------------------------------------------
    // Integration Tests
    //------------------------------------------------------------------------------------

    @Test
    public void testSelect() throws Exception {
        vantiq.select("types", null, null, null, handler);
        waitForCompletion();

        JsonObject match = null;
        for(JsonObject obj : handler.getBodyAsList()) {
            if("TestType".equals(obj.get("name").getAsString())) {
                match = obj;
            }
        }
        assertThat("TestType", match, is(notNullValue()));
    }

    @Test
    public void testSelectWithConstraints() throws Exception {
        JsonObject where = new JsonObject();
        where.addProperty("name", "TestType");

        vantiq.select("types", Arrays.asList("_id", "name"), where, null, handler);
        waitForCompletion();

        assertThat("Single TestType result", handler.getBodyAsList().size(), is(1));
        assertThat("Single TestType result", handler.getBodyAsList().get(0).get("name").getAsString(), is("TestType"));
    }

    @Test
    public void testSelectWithSorting() throws Exception {
        vantiq.select("types", Arrays.asList("_id", "name"), null, new SortSpec("name", true), handler);
        waitForCompletion();

        List<JsonObject> result = handler.getBodyAsList();
        String firstName = result.get(0).get("name").getAsString();
        String lastName  = result.get(result.size() - 1).get("name").getAsString();

        assertThat("Ensure results are sorted", firstName.compareTo(lastName), greaterThan(0));
    }

    @Test
    public void testSelectOne() throws Exception {
        vantiq.selectOne("types", "TestType", handler);
        waitForCompletion();

        assertThat("TestType result", handler.getBodyAsJsonObject().get("name").getAsString(), is("TestType"));
    }

    @Test
    public void testCount() throws Exception {
        vantiq.select("types", Collections.singletonList("_id"), null, null, handler);
        waitForCompletion();

        int countFromSelect = handler.getBodyAsList().size();
        vantiq.count("types", null, handler.reset());
        waitForCompletion();

        assertThat("Count match", handler.getBodyAsInt(), is(countFromSelect));
    }

    @Test
    public void testCountWithConstraints() throws Exception {
        JsonObject where = new JsonObject();
        where.addProperty("name", "TestType");

        vantiq.count("types", where, handler);
        waitForCompletion();

        assertThat("Count match", handler.getBodyAsInt(), is(1));
    }

    @Test
    public void testInsertAndUpdateRecord() throws Exception {
        Date now = new Date();

        String id = "IU-" + now.getTime();
        JsonObject record = new JsonObject();
        record.addProperty("id", id);
        record.addProperty("ts", getISOString(now));
        record.addProperty("x", 3.14159);
        record.addProperty("k", 42);

        JsonObject embedded = new JsonObject();
        embedded.addProperty("a", 1);
        embedded.addProperty("b", 2);
        record.add("o", embedded);

        // Insert record
        vantiq.insert("TestType", record, handler);
        waitForCompletion();

        // Select it back to ensure it was added successfully
        JsonObject where = new JsonObject();
        where.addProperty("id", id);
        vantiq.select("TestType", null, where, null, handler.reset());
        waitForCompletion();
        assertThat("Insert succeeded", handler.getBodyAsList().size(), is(1));

        JsonObject inserted = handler.getBodyAsList().get(0);
        assertThat("Insert has correct value", inserted.get("k").getAsInt(), is(42));

        // Update record
        JsonObject toUpdate = new JsonObject();
        toUpdate.addProperty("k", 13);
        vantiq.update("TestType", inserted.get("_id").getAsString(), toUpdate, handler.reset());
        waitForCompletion();

        // Select it back to ensure it was updated successfully
        vantiq.select("TestType", null, where, null, handler.reset());
        waitForCompletion();
        assertThat("Update succeeded", handler.getBodyAsList().size(), is(1));

        JsonObject updated = handler.getBodyAsList().get(0);
        assertThat("Update has correct value", updated.get("k").getAsInt(), is(13));
        assertThat("Update maintains other properties", updated.get("o").getAsJsonObject().get("a").getAsInt(), is(1));
    }

    @Test
    public void testUpsertRecord() throws Exception {
        Date now = new Date();

        String id = "UP-" + now.getTime();
        JsonObject record = new JsonObject();
        record.addProperty("id", id);
        record.addProperty("ts", getISOString(now));
        record.addProperty("x", 3.14159);
        record.addProperty("k", 42);

        JsonObject embedded = new JsonObject();
        embedded.addProperty("a", 1);
        embedded.addProperty("b", 2);
        record.add("o", embedded);

        // Insert record using upsert
        vantiq.upsert("TestType", record, handler);
        waitForCompletion();
        assertTrue("Upsert (insert) succeeded", handler.success);

        // Select it back to ensure it was added successfully
        JsonObject where = new JsonObject();
        where.addProperty("id", id);
        vantiq.select("TestType", null, where, null, handler.reset());
        waitForCompletion();
        assertThat("Insert succeeded", handler.getBodyAsList().size(), is(1));

        JsonObject inserted = handler.getBodyAsList().get(0);
        assertThat("Insert has correct value", inserted.get("k").getAsInt(), is(42));

        // Modify value
        inserted.addProperty("k", 13);

        // Update record using upsert
        vantiq.upsert("TestType", inserted, handler.reset());
        waitForCompletion();
        assertTrue("Upsert (update) succeeded", handler.success);

        // Select it back to ensure it was updated successfully
        vantiq.select("TestType", null, where, null, handler.reset());
        waitForCompletion();
        assertThat("Update succeeded", handler.getBodyAsList().size(), is(1));

        JsonObject updated = handler.getBodyAsList().get(0);
        assertThat("Update has correct value", updated.get("k").getAsInt(), is(13));
        assertThat("Update maintains other properties", updated.get("o").getAsJsonObject().get("a").getAsInt(), is(1));
    }

    @Test
    public void testDelete() throws Exception {
        Date now = new Date();

        String id = "DL-" + now.getTime();
        JsonObject record = new JsonObject();
        record.addProperty("id", id);
        record.addProperty("ts", getISOString(now));
        record.addProperty("x", 3.14159);
        record.addProperty("k", 42);

        JsonObject embedded = new JsonObject();
        embedded.addProperty("a", 1);
        embedded.addProperty("b", 2);
        record.add("o", embedded);

        // Insert record using upsert
        vantiq.insert("TestType", record, handler);
        waitForCompletion();
        assertTrue("Insert succeeded", handler.success);

        // Select all records and ensure that it was in the list
        vantiq.select("TestType", Arrays.asList("_id", "id"), null, null, handler.reset());
        waitForCompletion();

        int numRecords = handler.getBodyAsList().size();
        JsonObject match = null;
        for(JsonObject obj : handler.getBodyAsList()) {
            if(id.equals(obj.get("id").getAsString())) {
                match = obj;
            }
        }
        assertThat("Select found inserted record", match, is(notNullValue()));

        // Delete record
        JsonObject where = new JsonObject();
        where.addProperty("id", id);

        vantiq.delete("TestType", where, handler.reset());
        waitForCompletion();
        assertTrue("Delete succeeded", handler.success);

        // Select it back to ensure it was updated successfully
        vantiq.select("TestType", Arrays.asList("_id", "id"), null, null, handler.reset());
        waitForCompletion();

        assertThat("Delete removed record", handler.getBodyAsList().size(), is(numRecords - 1));
        match = null;
        for(JsonObject obj : handler.getBodyAsList()) {
            if(id.equals(obj.get("id").getAsString())) {
                match = obj;
            }
        }
        assertThat("Select should not find deleted record", match, is(nullValue()));
    }

    @Test
    public void testDeleteOne() throws Exception {
        Date now = new Date();

        String id = "DLONE-" + now.getTime();
        JsonObject record = new JsonObject();
        record.addProperty("id", id);
        record.addProperty("ts", getISOString(now));
        record.addProperty("x", 3.14159);
        record.addProperty("k", 42);

        JsonObject embedded = new JsonObject();
        embedded.addProperty("a", 1);
        embedded.addProperty("b", 2);
        record.add("o", embedded);

        // Insert record using upsert
        vantiq.insert("TestType", record, handler);
        waitForCompletion();
        assertTrue("Insert succeeded", handler.success);

        // Select all records and ensure that it was in the list
        vantiq.select("TestType", Arrays.asList("_id", "id"), null, null, handler.reset());
        waitForCompletion();

        int numRecords = handler.getBodyAsList().size();
        JsonObject match = null;
        for(JsonObject obj : handler.getBodyAsList()) {
            if(id.equals(obj.get("id").getAsString())) {
                match = obj;
            }
        }
        assertThat("Select found inserted record", match, is(notNullValue()));

        // Delete record
        vantiq.deleteOne("TestType", match.get("_id").getAsString(), handler.reset());
        waitForCompletion();
        assertTrue("Delete succeeded", handler.success);

        // Select it back to ensure it was updated successfully
        vantiq.select("TestType", Arrays.asList("_id", "id"), null, null, handler.reset());
        waitForCompletion();

        assertThat("Delete removed record", handler.getBodyAsList().size(), is(numRecords - 1));
        match = null;
        for(JsonObject obj : handler.getBodyAsList()) {
            if(id.equals(obj.get("id").getAsString())) {
                match = obj;
            }
        }
        assertThat("Select should not find deleted record", match, is(nullValue()));
    }

    @Test
    public void testPublishTopic() throws Exception {
        Date now = new Date();

        String id = "PB-" + now.getTime();
        JsonObject message = new JsonObject();
        message.addProperty("id", id);
        message.addProperty("ts", getISOString(now));
        message.addProperty("x", 3.14159);
        message.addProperty("k", 42);

        JsonObject embedded = new JsonObject();
        embedded.addProperty("a", 1);
        embedded.addProperty("b", 2);
        message.add("o", embedded);

        // Publish to topic
        vantiq.publish("topics", "/test/topic", message, handler);
        waitForCompletion();
        assertTrue("Insert succeeded", handler.success);

        // Rule should insert the record into TestType
        // so select it to find the record.  However, this may take a little bit of time
        // so, adding slight delay.
        Thread.sleep(50);

        // Select all records and ensure that it was in the list
        JsonObject where = new JsonObject();
        where.addProperty("id", id);

        vantiq.select("TestType", null, where, null, handler.reset());
        waitForCompletion();

        assertThat("Found result", handler.getBodyAsList().size(), is(1));
        assertThat("Correct value", handler.getBodyAsList().get(0).get("k").getAsInt(), is(42));
    }

    @Test
    public void testExecuteProcedure() throws Exception {
        JsonObject params = new JsonObject();
        params.addProperty("arg1", 3.14159);
        params.addProperty("arg2", "xxx");
        vantiq.execute("echo", params, handler);
        waitForCompletion();

        assertThat("Valid response", handler.getBodyAsJsonObject().get("arg1").getAsDouble(), is(3.14159));
        assertThat("Valid response", handler.getBodyAsJsonObject().get("arg2").getAsString(), is("xxx"));
    }
}