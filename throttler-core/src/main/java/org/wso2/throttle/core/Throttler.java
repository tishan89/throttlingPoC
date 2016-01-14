/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.throttle.core;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.wso2.carbon.databridge.agent.DataPublisher;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAgentConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointException;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.throttle.api.Policy;
import org.wso2.throttle.exception.ThrottleConfigurationException;
import org.wso2.throttle.util.ThrottlePolicyLoader;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class which does throttling.
 * 1. Get an instance
 * 2. Start
 * 3. Add rules
 * 4. Invoke isThrottled with {@link org.wso2.throttle.core.Throttler} object
 */
public class Throttler {
    private static final Logger log = Logger.getLogger(Throttler.class);
    private static final String RDBMS_THROTTLE_TABLE_NAME = "ThrottleTable";
    private static final String RDBMS_THROTTLE_TABLE_COLUMN_KEY = "throttle_key";
    private static final String RDBMS_THROTTLE_TABLE_COLUMN_ISTHROTTLED = "isThrottled";

    private static Throttler throttler;

    private SiddhiManager siddhiManager;
    private InputHandler eligibilityStreamInputHandler;
    private List<InputHandler> requestStreamInputHandlerList = new ArrayList<InputHandler>();
    private InputHandler globalThrottleStreamInputHandler;
    private static Map<String, ResultContainer> resultMap = new ConcurrentHashMap<String, ResultContainer>();

    private ExecutionPlanRuntime ruleRuntime;
    private int ruleCount = 0;


    private String hostName = "localhost";      //10.100.5.99
    private DataPublisher dataPublisher = null;

    private Throttler() {
    }

    public static synchronized Throttler getInstance() {
        if (throttler == null) {
            throttler = new Throttler();
            throttler.start();
        }
        return throttler;
    }

    /**
     * Starts throttler engine. Calling method should catch the exceptions and call stop to clean up.
     */
    private void start() {
        siddhiManager = new SiddhiManager();

        String commonExecutionPlan = "" +
                "define stream EligibilityStream (rule string, messageID string, isEligible bool, key string);\n" +
                "define stream GlobalThrottleStream (key string, isThrottled bool);\n" +
                "\n" +
                "@IndexBy('key')\n" +
                "define table ThrottleTable (key string, isThrottled bool);\n" +
                "\n" +
                "FROM EligibilityStream[isEligible==false]\n" +
                "SELECT rule, messageID, false AS isThrottled\n" +
                "INSERT INTO ThrottleStream;\n" +
                "\n" +
                "FROM EligibilityStream[isEligible==true] LEFT OUTER JOIN ThrottleTable\n" +
                "\tON ThrottleTable.key == EligibilityStream.key\n" +
                "SELECT rule, messageID, ifThenElse((ThrottleTable.isThrottled is null),false,ThrottleTable.isThrottled) AS isThrottled\n" +
                "INSERT INTO ThrottleStream;" +
                "\n" +
                "from GlobalThrottleStream\n" +
                "select *\n" +
                "insert into ThrottleTable;";

        ExecutionPlanRuntime commonExecutionPlanRuntime = siddhiManager.createExecutionPlanRuntime(commonExecutionPlan);

        //add callback to get local throttling result and add it to ResultContainer
        commonExecutionPlanRuntime.addCallback("ThrottleStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                for (Event event : events) {
                    resultMap.get(event.getData(1).toString()).addResult((String) event.getData(0), (Boolean) event.getData(2));
                }
            }
        });

        //get and register inputHandler
        this.eligibilityStreamInputHandler = commonExecutionPlanRuntime.getInputHandler("EligibilityStream");
        this.globalThrottleStreamInputHandler = commonExecutionPlanRuntime.getInputHandler("GlobalThrottleStream");

        commonExecutionPlanRuntime.start();

        populateThrottleTable();

        try {
            populateThrottlingPolicies();
        } catch (ThrottleConfigurationException e) {
            log.error("Error loading throttling policies from file. " + e.getMessage(), e);
        }

        //initialize binary data publisher to send requests to global CEP instance
        initDataPublisher();
    }

    /**
     * Reads throttling policy file and load {@link org.wso2.throttle.core.QueryTemplateStore}
     * @throws ThrottleConfigurationException
     */
    private void populateThrottlingPolicies() throws ThrottleConfigurationException {
        for(Policy policy : ThrottlePolicyLoader.loadThrottlingPolicies()){
            QueryTemplateStore.getInstance().addThrottlingEligibilityQuery(policy.getEligibilityQuery());
        }
    }

//    //todo: this method has not being implemented completely. Will be done after doing perf tests.
//    private void deployRuleToGlobalCEP(String templateID, String parameter1, String parameter2) {
//        String queries = QueryTemplateStore.constructEnforcementQuery();
//
//        ExecutionPlanRuntime ruleRuntime = siddhiManager.createExecutionPlanRuntime("define stream RequestStream (messageID string, app_key string, api_key string, resource_key string, app_tier string, api_tier string, resource_tier string); " +
//                queries);
//
//        GlobalCEPClient globalCEPClient = new GlobalCEPClient();
//        globalCEPClient.deployExecutionPlan(queries);
//    }

    public void deployLocalCEPRules() {
        StringBuilder eligibilityQueriesBuilder = new StringBuilder();
        eligibilityQueriesBuilder.append("define stream RequestStream (" + QueryTemplateStore.getInstance()
                .loadThrottlingAttributes() + "); \n");

        for (String eligibilityQuery : QueryTemplateStore.getInstance().loadThrottlingEligibilityQueries()) {
            eligibilityQueriesBuilder.append(eligibilityQuery);
            ruleCount++;
        }


        ExecutionPlanRuntime ruleRuntime = siddhiManager.createExecutionPlanRuntime(
                eligibilityQueriesBuilder.toString());

        //Add call backs. Here, we take output events and insert into EligibilityStream
        ruleRuntime.addCallback("EligibilityStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                try {
                    getEligibilityStreamInputHandler().send(events);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Error occurred when publishing to EligibilityStream.", e);
                }
            }
        });

        //get and register input handler for RequestStream, so isThrottled() can use it.
        requestStreamInputHandlerList.add(ruleRuntime.getInputHandler("RequestStream"));
        ruleRuntime.start();
    }


    /**
     * Returns whether the given throttleRequest is throttled.
     *
     * @param throttleRequest User throttleRequest to APIM which needs to be checked whether throttled
     * @return Throttle status for current throttleRequest
     */
    public boolean isThrottled(Object[] throttleRequest) {
        //UUID uniqueKey = UUID.randomUUID();
        if (ruleCount != 0) {
            String uniqueKey = (String) throttleRequest[0];
            ResultContainer result = new ResultContainer(ruleCount);
            resultMap.put(uniqueKey.toString(), result);
            Iterator<InputHandler> handlerList = requestStreamInputHandlerList.iterator();
            while (handlerList.hasNext()) {
                InputHandler inputHandler = handlerList.next();
                try {
                    inputHandler.send(Arrays.copyOf(throttleRequest, throttleRequest.length));
                } catch (InterruptedException e) {
                    //interrupt current thread so that interrupt can propagate
                    Thread.currentThread().interrupt();
                    log.error(e.getMessage(), e);
                }
            }
            //Blocked call to return synchronous result
            boolean isThrottled = false;
            try {
                isThrottled = result.isThrottled();
            } catch (InterruptedException e) {
                //interrupt current thread so that interrupt can propagate
                Thread.currentThread().interrupt();
                log.error(e.getMessage(), e);
            }
            if (!isThrottled) {                                           //Only send served throttleRequest to global throttler
                sendToGlobalThrottler(throttleRequest);
            }
            resultMap.remove(uniqueKey);
            return isThrottled;
        } else {
            return false;
        }
    }

    public void stop() {
        if (siddhiManager != null) {
            siddhiManager.shutdown();
        }
        if (ruleRuntime != null) {
            ruleRuntime.shutdown();
        }
    }

    /**
     * Copies physical ThrottleTable to this instance's in-memory ThrottleTable.
     * Todo will be removed when siddhi can load data time to time.
     */
    private void populateThrottleTable() {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        basicDataSource.setUrl("jdbc:mysql://localhost/org_wso2_throttle_DataSource");
        basicDataSource.setUsername("root");
        basicDataSource.setPassword("root");

        Connection connection = null;
        try {
            connection = basicDataSource.getConnection();
            DatabaseMetaData dbm = connection.getMetaData();
            // check if "ThrottleTable" table is there
            ResultSet tables = dbm.getTables(null, null, RDBMS_THROTTLE_TABLE_NAME, null);
            if (tables.next()) { // Table exists
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM " + RDBMS_THROTTLE_TABLE_NAME);
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    String key = resultSet.getString(RDBMS_THROTTLE_TABLE_COLUMN_KEY);
                    Boolean isThrottled = resultSet.getBoolean(RDBMS_THROTTLE_TABLE_COLUMN_ISTHROTTLED);
                    try {
                        getGlobalThrottleStreamInputHandler().send(new Object[]{key, isThrottled});
                    } catch (InterruptedException e) {
                        log.error("Error occurred while sending an event.", e);
                    }
                }
            } else {  // Table does not exist
                log.warn("RDBMS ThrottleTable does not exist. Make sure global throttler server is started.");
            }
        } catch (SQLException e) {
            log.error("Error occurred while copying throttle data from global throttler server.", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.error("Error occurred while closing database connection.", e);
                }
            }
        }
    }


    private InputHandler getEligibilityStreamInputHandler() {
        return eligibilityStreamInputHandler;
    }

    public InputHandler getGlobalThrottleStreamInputHandler() {
        return globalThrottleStreamInputHandler;
    }

    private void sendToGlobalThrottler(Object[] throttleRequest) {
        org.wso2.carbon.databridge.commons.Event event = new org.wso2.carbon.databridge.commons.Event();
        event.setStreamId(DataBridgeCommonsUtils.generateStreamId("org.wso2.throttle.request.stream", "1.0.3"));
        event.setMetaData(null);
        event.setCorrelationData(null);
        event.setPayloadData(throttleRequest);
        dataPublisher.tryPublish(event);
    }

    private void initDataPublisher() {
        try {
            dataPublisher = new DataPublisher("Binary", "tcp://" + hostName + ":9621",
                    "ssl://" + hostName + ":9721", "admin", "admin");
        } catch (DataEndpointAgentConfigurationException e) {
            log.error(e.getMessage(), e);
        } catch (DataEndpointException e) {
            log.error(e.getMessage(), e);
        } catch (DataEndpointConfigurationException e) {
            log.error(e.getMessage(), e);
        } catch (DataEndpointAuthenticationException e) {
            log.error(e.getMessage(), e);
        } catch (TransportException e) {
            log.error(e.getMessage(), e);
        }


    }

}
