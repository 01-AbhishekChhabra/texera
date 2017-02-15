package edu.uci.ics.textdb.planstore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.uci.ics.textdb.api.common.ITuple;
import edu.uci.ics.textdb.api.exception.TextDBException;
import edu.uci.ics.textdb.api.storage.IDataReader;
import edu.uci.ics.textdb.plangen.LogicalPlan;
import edu.uci.ics.textdb.plangen.LogicalPlanTest;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author sweetest, Kishore Narendran
 */
public class PlanStoreTest {
    private static PlanStore planStore;

    private static final String logicalPlanJson1 = "{\n" +
            "    \"operators\": [{\n" +
            "        \"operator_id\": \"operator1\",\n" +
            "        \"operator_type\": \"DictionarySource\",\n" +
            "        \"attributes\": \"attributes\",\n" +
            "        \"limit\": \"10\",\n" +
            "        \"offset\": \"100\",\n" +
            "        \"data_source\": \"query_plan_resource_test_table\",\n" +
            "        \"dictionary\": \"dict1\",\n" +
            "        \"matching_type\": \"PHRASE_INDEXBASED\"\n" +
            "    }, {\n" +
            "\n" +
            "        \"operator_id\": \"operator2\",\n" +
            "        \"operator_type\": \"TupleStreamSink\",\n" +
            "        \"attributes\": \"attributes\",\n" +
            "        \"limit\": \"10\",\n" +
            "        \"offset\": \"100\"\n" +
            "    }],\n" +
            "    \"links\": [{\n" +
            "        \"from\": \"operator1\",\n" +
            "        \"to\": \"operator2\"    \n" +
            "    }]\n" +
            "}";

    private static final String logicalPlanJson2 = "{\n" +
            "    \"operators\": [{\n" +
            "        \"operator_id\": \"operator1\",\n" +
            "        \"operator_type\": \"KeywordSource\",\n" +
            "        \"attributes\": \"attributes\",\n" +
            "        \"data_source\": \"query_plan_resource_test_table\",\n" +
            "        \"dictionary\": \"zika\",\n" +
            "        \"matching_type\": \"PHRASE_INDEXBASED\"\n" +
            "    }, {\n" +
            "\n" +
            "        \"operator_id\": \"operator2\",\n" +
            "        \"operator_type\": \"TupleStreamSink\",\n" +
            "        \"attributes\": \"attributes\",\n" +
            "        \"limit\": \"10\",\n" +
            "        \"offset\": \"100\"\n" +
            "    }],\n" +
            "    \"links\": [{\n" +
            "        \"from\": \"operator1\",\n" +
            "        \"to\": \"operator2\"    \n" +
            "    }]\n" +
            "}";


    @Before
    public void setUp() throws Exception {
        planStore = PlanStore.getInstance();
        planStore.createPlanStore();
    }

    @After
    public void cleanUp() throws Exception {
        planStore.destroyPlanStore();
    }

    public static void assertCorrectPlanExists(String planName, String logicalPlanJson) throws TextDBException {
        ITuple res = planStore.getPlan(planName);

        Assert.assertNotNull(res);

        String filePath = res.getField(PlanStoreConstants.FILE_PATH).getValue().toString();
        String returnedPlan = planStore.readPlanJson(filePath);

        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(logicalPlanJson);
        JsonElement returnedJsonElement = jsonParser.parse(returnedPlan);

        Assert.assertEquals(jsonElement, returnedJsonElement);
    }

    public static void assertPlanEquivalence(String plan1, String plan2) {
        Assert.assertNotNull(plan1);
        Assert.assertNotNull(plan2);

        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement1 = jsonParser.parse(plan1);
        JsonElement jsonElement2 = jsonParser.parse(plan2);

        Assert.assertEquals(jsonElement1, jsonElement2);
    }

    @Test
    public void testAddPlan() throws TextDBException {
        String planName = "plan";

        planStore.addPlan(planName, "basic dictionary source plan", logicalPlanJson1);

        assertCorrectPlanExists(planName, logicalPlanJson1);
    }

    @Test
    public void testUpdatePlan() throws TextDBException {
        LogicalPlan logicalPlan1 = LogicalPlanTest.getLogicalPlan1();
        LogicalPlan logicalPlan2 = LogicalPlanTest.getLogicalPlan2();

        String planName1 = "plan1";

        planStore.addPlan(planName1, "basic dictionary source plan", logicalPlanJson1);

        planStore.updatePlan(planName1, logicalPlanJson2);

        assertCorrectPlanExists(planName1, logicalPlanJson2);
    }

    @Test
    public void testDeletePlan() throws TextDBException {

        String planName1 = "plan1";
        String planName2 = "plan2";

        planStore.addPlan(planName1, "basic dictionary source plan", logicalPlanJson1);
        planStore.addPlan(planName2, "basic keyword source plan", logicalPlanJson2);

        planStore.deletePlan(planName1);

        ITuple returnedPlan = planStore.getPlan(planName1);
        Assert.assertNull(returnedPlan);
    }

    @Test
    public void testPlanIterator() throws TextDBException {

        List<String> validPlans = new ArrayList<>();
        validPlans.add(logicalPlanJson1);
        validPlans.add(logicalPlanJson2);

        List<String> expectedPlans = new ArrayList<>();

        String planNamePrefix = "plan_";

        for (int i = 0; i < 100; i++) {
            String plan = validPlans.get(i % 2);
            expectedPlans.add(plan);
            planStore.addPlan(planNamePrefix + i, "basic plan " + i, plan);
        }


        IDataReader reader = planStore.getPlanIterator();
        reader.open();

        ITuple tuple = null;
        String[] returnedPlans = new String[expectedPlans.size()];

        while ((tuple = reader.getNextTuple()) != null) {
            String planName = tuple.getField(PlanStoreConstants.NAME).getValue().toString();
            int planIdx = Integer.parseInt(planName.split("_")[1]);
            String filePath = tuple.getField(PlanStoreConstants.FILE_PATH).getValue().toString();
            String logicalPlanJson = planStore.readPlanJson(filePath);
            returnedPlans[planIdx] = logicalPlanJson;
        }
        reader.close();

        for(int i = 0; i < expectedPlans.size(); i++) {
            assertPlanEquivalence(expectedPlans.get(i), returnedPlans[i]);
        }
    }

    @Test(expected = TextDBException.class)
    public void testAddPlanWithInvalidName() throws TextDBException {

        String planName = "plan/regex";

        planStore.addPlan(planName, "basic dictionary source plan", logicalPlanJson1);
    }

    @Test(expected = TextDBException.class)
    public void testAddPlanWithEmptyName() throws TextDBException {

        String planName = "";

        planStore.addPlan(planName, "basic dictionary source plan", logicalPlanJson1);
    }

    @Test(expected = TextDBException.class)
    public void testAddMultiplePlansWithSameName() throws TextDBException {

        String planName = "plan";

        planStore.addPlan(planName, "basic dictionary source plan", logicalPlanJson1);
        planStore.addPlan(planName, "basic dictionary source plan", logicalPlanJson2);
    }

    @Test
    public void testDeleteNotExistingPlan() throws TextDBException {

        String planName = "plan";

        planStore.addPlan(planName, "basic dictionary source plan", logicalPlanJson1);

        planStore.deletePlan(planName + planName);

        assertCorrectPlanExists(planName, logicalPlanJson1);
    }

    @Test
    public void testUpdateNotExistingPlan() throws TextDBException {

        String planName = "plan";

        planStore.addPlan(planName, "basic dictionary source plan", logicalPlanJson1);

        planStore.updatePlan(planName + planName, "basic dictionary source plan", logicalPlanJson1);

        assertCorrectPlanExists(planName, logicalPlanJson1);
    }

    @Test
    public void testNotDeletePlanBySubstring() throws TextDBException {

        String planName = "plan_sub";

        planStore.addPlan(planName, "basic dictionary source plan", logicalPlanJson1);

        planStore.deletePlan(planName.substring(0, 4));

        assertCorrectPlanExists(planName, logicalPlanJson1);
    }

}
