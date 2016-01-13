/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.log4j.Logger;
import org.wso2.carbon.databridge.commons.Credentials;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.databridge.commons.utils.EventDefinitionConverterUtils;
import org.wso2.carbon.databridge.core.AgentCallback;
import org.wso2.carbon.databridge.core.DataBridge;
import org.wso2.carbon.databridge.core.DataBridgeReceiverService;
import org.wso2.carbon.databridge.core.DataBridgeSubscriberService;
import org.wso2.carbon.databridge.core.Utils.AgentSession;
import org.wso2.carbon.databridge.core.definitionstore.InMemoryStreamDefinitionStore;
import org.wso2.carbon.databridge.core.exception.DataBridgeException;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;
import org.wso2.carbon.databridge.core.internal.authentication.AuthenticationHandler;
import org.wso2.carbon.databridge.receiver.binary.conf.BinaryDataReceiverConfiguration;
import org.wso2.carbon.databridge.receiver.binary.internal.BinaryDataReceiver;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.throttle.common.util.DatabridgeServerUtil;
import org.wso2.throttle.internal.ThrottleServiceValueHolder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Borrowed this class from org.wso2.carbon.databridge.agent.test.binary.BinaryTestServer and modified.
 */
public class EventReceivingServer {
//    Logger log = Logger.getLogger(EventReceivingServer.class);
//
//    private BinaryDataReceiver binaryDataReceiver;
//    private InMemoryStreamDefinitionStore streamDefinitionStore;
//    private AtomicInteger numberOfEventsReceived = new AtomicInteger(0);
//    private Throttler throttler;
//
//
//    public void addStreamDefinition(StreamDefinition streamDefinition, int tenantId)
//            throws StreamDefinitionStoreException {
//        streamDefinitionStore.saveStreamDefinitionToStore(streamDefinition, tenantId);
//    }
//
//    public void addStreamDefinition(String streamDefinitionStr, int tenantId)
//            throws StreamDefinitionStoreException, MalformedStreamDefinitionException {
//        StreamDefinition streamDefinition = EventDefinitionConverterUtils.convertFromJson(streamDefinitionStr);
//        getStreamDefinitionStore().saveStreamDefinitionToStore(streamDefinition, tenantId);
//    }
//
//    private InMemoryStreamDefinitionStore getStreamDefinitionStore() {
//        if (streamDefinitionStore == null) {
//            streamDefinitionStore = new InMemoryStreamDefinitionStore();
//        }
//        return streamDefinitionStore;
//    }
//
//    public void start(int tcpPort, int securePort) throws DataBridgeException, IOException, StreamDefinitionStoreException {
//        throttler = Throttler.getInstance();
//
//        DatabridgeServerUtil.setKeyStoreParams();
//        DatabridgeServerUtil.setTrustStoreParams();
//        DataBridgeSubscriberService receiverService = ThrottleServiceValueHolder.getDataBridgeSubscriberService();
//        receiverService.subscribe(new AgentCallback() {
//
//            public void definedStream(StreamDefinition streamDefinition,
//                                      int tenantId) {
//                log.info("StreamDefinition " + streamDefinition);
//            }
//
//            @Override
//            public void removeStream(StreamDefinition streamDefinition, int tenantId) {
//                log.info("StreamDefinition remove " + streamDefinition);
//            }
//
//            @Override
//            public void receive(List<Event> eventList, Credentials credentials) {
//                if (log.isDebugEnabled()) {
//                    numberOfEventsReceived.addAndGet(eventList.size());
//                    log.debug("Received events : " + numberOfEventsReceived);
//                }
//                for (Event event : eventList) {
//                    try {
//                        throttler.getGlobalThrottleStreamInputHandler().send(event.getTimeStamp(),
//                                event.getPayloadData());
//                    } catch (InterruptedException e) {
//                        log.error("Interruption occurred while sending message to global inout stream. " + e.getMessage(), e);
//                    }
//
//                }
//            }
//
//        });
//
//        log.info("Event Receiving Server Started");
//    }
//
//    public int getNumberOfEventsReceived() {
//        if (numberOfEventsReceived != null) return numberOfEventsReceived.get();
//        else return 0;
//    }
//
//    public void resetReceivedEvents() {
//        numberOfEventsReceived.set(0);
//    }
//
//    public void stop() {
//        binaryDataReceiver.stop();
//        log.info("Test Server Stopped");
//    }


}
