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

package org.wso2.throttle.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.log4j.Logger;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.throttle.api.Policy;
import org.wso2.throttle.exception.ThrottleConfigurationException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ThrottlePolicyLoader {
    private static final Logger log = Logger.getLogger(ThrottlePolicyLoader.class);

    /**
     * Reads throttling policy config file and populate a list of {@link org.wso2.throttle.api.Policy} objects.
     * @return List of policies
     * @throws ThrottleConfigurationException
     */
    public static List<Policy> loadThrottlingPolicies() throws ThrottleConfigurationException {
        OMElement policyConfig = loadConfigXML();
        Iterator<OMElement> iterator = policyConfig.getChildrenWithName(new QName(ThrottleConstants.POLICY_ELEMENT));
        List<Policy> policyList = new ArrayList<Policy>();

        while (iterator.hasNext()) {
            OMElement policyElement = iterator.next();
            String name;
            String level;
            String tier;

            if ((name = policyElement.getAttributeValue(new QName(ThrottleConstants.NAME))) == null) {
                throw new ThrottleConfigurationException("Invalid policy element with no name attribute in " +
                        ThrottleConstants.THROTTLE_POLICY_XML);
            }

            if ((tier = policyElement.getAttributeValue(new QName(ThrottleConstants.TIER))) == null) {
                throw new ThrottleConfigurationException("Invalid policy element with no tier attribute in " +
                        ThrottleConstants.THROTTLE_POLICY_XML);
            }

            if ((level = policyElement.getAttributeValue(new QName(ThrottleConstants.LEVEL))) == null) {
                throw new ThrottleConfigurationException("Invalid policy element with no name attribute in " +
                        ThrottleConstants.THROTTLE_POLICY_XML);
            }

            OMElement descriptionElement = policyElement.getFirstChildWithName(new QName(ThrottleConstants.DESCRIPTION));

            OMElement eligibilityQueryElement;
            if ((eligibilityQueryElement = policyElement.getFirstChildWithName(new QName(ThrottleConstants
                    .ELIGIBILITY_QUERY))) == null) {
                throw new ThrottleConfigurationException("Invalid policy element with no eligibility query in " +
                        ThrottleConstants.THROTTLE_POLICY_XML);
            }

            OMElement decisionQueryElement;
            if ((decisionQueryElement = policyElement.getFirstChildWithName(new QName(ThrottleConstants
                    .DECISION_QUERY))) == null) {
                throw new ThrottleConfigurationException("Invalid policy element with no decision query in " +
                        ThrottleConstants.THROTTLE_POLICY_XML);
            }

            Policy policy = new Policy(name, level, tier, eligibilityQueryElement.getText(), decisionQueryElement.getText());
            if(descriptionElement != null){
                policy.setDescription(descriptionElement.getText());
            }
            policyList.add(policy);
        }

        return policyList;
    }

    /**
     * Loads the throttling policy config in {CARBON_HOME}/repository/conf/throttle-policy.xml as OM element
     * @return OMElement of throttle config file
     * @throws ThrottleConfigurationException
     */
    private static OMElement loadConfigXML() throws ThrottleConfigurationException {

        String carbonHome = System.getProperty(ServerConstants.CARBON_CONFIG_DIR_PATH);
        String path = carbonHome + File.separator + ThrottleConstants.THROTTLE_POLICY_XML;

        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(new File(path)));
            XMLStreamReader parser = XMLInputFactory.newInstance().
                    createXMLStreamReader(inputStream);
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            OMElement omElement = builder.getDocumentElement();
            omElement.build();
            return omElement;
        } catch (FileNotFoundException e) {
            throw new ThrottleConfigurationException(ThrottleConstants.THROTTLE_POLICY_XML + "cannot be found in the path : " + path, e);
        } catch (XMLStreamException e) {
            throw new ThrottleConfigurationException("Invalid XML for " + ThrottleConstants.THROTTLE_POLICY_XML +
                    " located in the path : " + path, e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                log.error("Can not shutdown the input stream", e);
            }
        }
    }

}
