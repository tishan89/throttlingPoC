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

package org.wso2.throttle.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.databridge.commons.Credentials;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.core.AgentCallback;
import org.wso2.carbon.databridge.core.DataBridgeSubscriberService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @scr.component name="throttleService.component" immediate="true"
 */

//scr.reference name="agentserverservice.service"
//interface="org.wso2.carbon.databridge.core.DataBridgeSubscriberService" cardinality="1..1"
//policy="dynamic" bind="setDataBridgeSubscriberService" unbind="unSetDataBridgeSubscriberService"

/**
 * OSGi initialization class.
 * This class will get DataBridgeSubscriberService upon initialization and store in
 * {@link org.wso2.throttle.internal.ThrottleServiceValueHolder} to be used by {@link org.wso2.throttle.core.EventReceivingServer}
 */
public class ThrottleDS {
    private static final Log log = LogFactory.getLog(ThrottleDS.class);

    protected void activate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Successfully deployed the WSO2 throttling service");
        }
    }
//
//    protected void setDataBridgeSubscriberService(
//            DataBridgeSubscriberService dataBridgeSubscriberService) {
//        if (ThrottleServiceValueHolder.getDataBridgeSubscriberService() == null) {
//            ThrottleServiceValueHolder.registerDataBridgeSubscriberService(dataBridgeSubscriberService);
//
//            dataBridgeSubscriberService.subscribe(new AgentCallback() {
//                @Override
//                public void definedStream(StreamDefinition streamDefinition, int i) {
//
//                }
//
//                @Override
//                public void removeStream(StreamDefinition streamDefinition, int i) {
//
//                }
//
//                @Override
//                public void receive(List<Event> events, Credentials credentials) {
//                    try {
//                        PrivilegedCarbonContext.startTenantFlow();
//                        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(credentials.getTenantId());
//                        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(credentials.getDomainName());
//                        for (Event event : events) {
//                            //todo
//                            if (log.isDebugEnabled()) {
//                                log.debug("Event received in wso2Event Adapter - " + event);
//                            }
//                        }
//                    } finally {
//                        PrivilegedCarbonContext.endTenantFlow();
//                    }
//                }
//            });
//        }
//    }
//
//    protected void unSetDataBridgeSubscriberService(
//            DataBridgeSubscriberService dataBridgeSubscriberService) {
//
//    }
}
