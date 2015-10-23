package org.wso2.throttle.core;

import org.apache.log4j.Logger;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.output.StreamCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class which does throttling
 */
public class Throttler {
    private static final Logger log = Logger.getLogger(Throttler.class);

    private SiddhiManager siddhiManager = new SiddhiManager();
    private Map<String,ExecutionPlanRuntime> apiToEPRuntimeMap = new HashMap<String, ExecutionPlanRuntime>();
    private Map<String, List<Policy>> apiToPoliciesMap = new HashMap<String, List<Policy>>();

    private static Map<String, ResultContainer> resultMap = new ConcurrentHashMap<String, ResultContainer>();


    public boolean isThrottled(Request request) {
        String apiName = request.getAPIName();

        UUID uniqueKey = UUID.randomUUID();

        ExecutionPlanRuntime planRuntime = apiToEPRuntimeMap.get(apiName);
        if(planRuntime == null){
            log.warn("No throttle policies attached to API: " + apiName + " hence allowing request.");
            return false;
        }

        ResultContainer result = new ResultContainer(apiToPoliciesMap.get(apiName).size());
        resultMap.put(uniqueKey.toString(), result);

        for (Policy policy : apiToPoliciesMap.get(apiName)) {
            try {
                planRuntime.getInputHandler(policy.name() + "EvalStream").send(new Object[]{uniqueKey.toString(), request.getIp(), 5});
            } catch (InterruptedException e) {
                log.error("Error sending events to Siddhi " + e.getMessage(), e);
            }
        }
        Boolean isThrottled = result.isThrottled();
        //todo: send to remote CEP as well.... For now, we return isThrottled
        return isThrottled;
    }

    /**
     * @param apiName the api to which the policy need to be applied.
     * @param policy For now, Policy is an enum. In future, this could be an XML file or something similar.
     * @return
     */
    public synchronized void addPolicy(String apiName, Policy policy) {         //todo: need removePolicy() also
        String newExecutionPlan = createExecutionPlan(policy);
        ExecutionPlanRuntime newExecutionPlanRuntime = siddhiManager.createExecutionPlanRuntime(newExecutionPlan);
        ExecutionPlanRuntime oldExecutionPlanRuntime = apiToEPRuntimeMap.get(apiName);
        if(oldExecutionPlanRuntime != null){
            //todo: add debug log on shutting down old runtime
            oldExecutionPlanRuntime.shutdown();
        }
        //todo: add debug log on starting new runtime
        newExecutionPlanRuntime.start();
        addCallbacks(newExecutionPlanRuntime);
        apiToEPRuntimeMap.put(apiName, newExecutionPlanRuntime);

        //Update apiToPoliciesMap
        List<Policy> policyList = apiToPoliciesMap.get(apiName);
        if(policyList == null){
            policyList = new ArrayList<Policy>();
        }
        policyList.add(policy);
        apiToPoliciesMap.put(apiName, policyList);
    }


    private String createExecutionPlan(Policy policy){
        List<String> definitionList = new ArrayList<String>();
        List<String> queryList = new ArrayList<String>();
        definitionList.add("define stream Rule1EvalStream (messageID string, ip string, maxCount int); ");
        definitionList.add("define stream Rule1InStream (ip string, isThrottled bool); ");
        definitionList.add("@IndexedBy('ip') define table Rule1Table (ip string, isThrottled bool); ");

        queryList.add("from Rule1InStream select * insert into Rule1Table; ");
        queryList.add("from Rule1EvalStream [not (Rule1Table.ip == Rule1EvalStream.ip in Rule1Table)] select " +
                      "messageID, ip, false as isThrottled insert into LocalResultStream;");
        queryList.add("from Rule1EvalStream join Rule1Table on Rule1Table.ip == Rule1EvalStream.ip select " +
                      "Rule1EvalStream.messageID, Rule1EvalStream.ip, Rule1Table.isThrottled insert into LocalResultStream;");


        definitionList.add("define stream Rule2EvalStream (messageID string, ip string, maxCount int); ");
        definitionList.add("define stream Rule2InStream (ip string, isThrottled bool); ");
        definitionList.add("@IndexedBy('ip') define table Rule2Table (ip string, isThrottled bool); ");

        queryList.add("from Rule2InStream select * insert into Rule2Table; ");
        queryList.add("from Rule2EvalStream[not (Rule2Table.ip == Rule2EvalStream.ip in Rule2Table)] select " +
                      "messageID, ip, false as isThrottled insert into LocalResultStream; ");
        queryList.add("from Rule2EvalStream join Rule2Table on Rule2Table.ip == Rule2EvalStream.ip select " +
                      "Rule2EvalStream.messageID , Rule2EvalStream.ip, Rule2Table.isThrottled insert into " +
                      "LocalResultStream; ");
        return constructFullQuery(definitionList, queryList);
    }


    private String constructFullQuery(List<String> definitionList, List<String> queryList) {
        StringBuilder stringBuilder = new StringBuilder();
        for(String definition : definitionList){
            stringBuilder.append(definition);
        }
        for(String query : queryList){
            stringBuilder.append(query);
        }
        return stringBuilder.toString();
    }


    private void addCallbacks(ExecutionPlanRuntime runtime){
        runtime.addCallback("LocalResultStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                for (Event event : events) {
                    resultMap.get(event.getData(0)).addResult((Boolean) event.getData(2));
                }
            }
        });
    }
}
