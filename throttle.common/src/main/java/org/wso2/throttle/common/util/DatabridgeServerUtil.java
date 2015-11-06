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
package org.wso2.throttle.common.util;

import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.databridge.commons.utils.EventDefinitionConverterUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

public class DatabridgeServerUtil {
    private static Logger log = Logger.getLogger(DatabridgeServerUtil.class);
    public static final String LOCAL_HOST = "localhost";

    public static void setTrustStoreParams() {
        File filePath = new File("throttle.common" + File.separator + "src" + File.separator + "main" + File.separator +
                "resources");
        if (!filePath.exists()) {
            filePath = new File("components" + File.separator + "data-bridge" + File.separator + "org.wso2.carbon.databridge.agent" + File.separator + "src" + File.separator + "test" + File.separator + "resources");
        }
        if (!filePath.exists()) {
            filePath = new File("resources");
        }
        if (!filePath.exists()) {
            filePath = new File("test" + File.separator + "resources");
        }
        String trustStore = filePath.getAbsolutePath();
        System.setProperty("javax.net.ssl.trustStore", trustStore + File.separator + "client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

    }

    public static void setKeyStoreParams() {
        File filePath = new File("throttle.common" + File.separator + "src" + File.separator + "main" + File.separator +
                "resources");
        if (!filePath.exists()) {
            filePath = new File("components" + File.separator + "data-bridge" + File.separator + "org.wso2.carbon.databridge.agent" + File.separator + "src" + File.separator + "test" + File.separator + "resources");
        }
        if (!filePath.exists()) {
            filePath = new File("resources");
        }
        if (!filePath.exists()) {
            filePath = new File("test" + File.separator + "resources");
        }
        String keyStore = filePath.getAbsolutePath();
        System.setProperty("Security.KeyStore.Location", keyStore + File.separator + "wso2carbon.jks");
        System.setProperty("Security.KeyStore.Password", "wso2carbon");
    }

    public static String getDataAgentConfigPath() {
        File filePath = new File("throttle.common" + File.separator + "src" + File.separator + "main" + File.separator +
                "resources");
        if (!filePath.exists()) {
            filePath = new File("components" + File.separator + "data-bridge" + File.separator + "org.wso2.carbon.databridge.agent" + File.separator + "src" + File.separator + "test" + File.separator + "resources");
        }
        if (!filePath.exists()) {
            filePath = new File("resources");
        }
        if (!filePath.exists()) {
            filePath = new File("test" + File.separator + "resources");
        }
        return filePath.getAbsolutePath() + File.separator + "data-agent-config.xml";
    }

    public static String getDataBridgeConfigPath() {
        File filePath = new File("throttle.common" + File.separator + "src" + File.separator + "main" + File.separator +
                "resources");
        if (!filePath.exists()) {
            filePath = new File("components" + File.separator + "data-bridge" + File.separator + "org.wso2.carbon.databridge.agent" + File.separator + "src" + File.separator + "test" + File.separator + "resources");
        }
        if (!filePath.exists()) {
            filePath = new File("resources");
        }
        if (!filePath.exists()) {
            filePath = new File("test" + File.separator + "resources");
        }
        return filePath.getAbsolutePath() + File.separator + "data-bridge-config.xml";
    }

    public static StreamDefinition loadStream() {
        File fileEntry = new File("throttle.common" + File.separator + "src" + File.separator + "main" + File.separator +
                "resources" + File.separator + "ResultStream_1.0.0.json").getAbsoluteFile();
        BufferedReader bufferedReader = null;
        StreamDefinition streamDefinition = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            bufferedReader = new BufferedReader(new FileReader(fileEntry));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            streamDefinition = EventDefinitionConverterUtils.convertFromJson(stringBuilder.toString().trim());
        } catch (FileNotFoundException e) {
            log.error("Error in reading file " + fileEntry.getName(), e);
        } catch (IOException e) {
            log.error("Error in reading file " + fileEntry.getName(), e);
        } catch (MalformedStreamDefinitionException e) {
            log.error("Error in converting Stream definition " + e.getMessage(), e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                log.error("Error occurred when reading the file : " + e.getMessage(), e);
            }
            return streamDefinition;
        }
    }

}
