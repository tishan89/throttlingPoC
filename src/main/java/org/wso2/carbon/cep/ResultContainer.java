/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.cep;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ResultContainer {
    private static final Logger log = Logger.getLogger(ResultContainer.class);
    private Semaphore semaphore;
    private List<Boolean> results;

    public ResultContainer(int size) {
        semaphore = new Semaphore(-1 * size - 1);
        results = Collections.synchronizedList(new ArrayList<Boolean>(size));
    }

    public void addResult(Boolean result) {
        results.add(result);
        semaphore.release();
    }

    public void acquire() {
        try {
            this.semaphore.acquire();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Wait for other threads to post results and then return results back.
     *
     * @return isThrottled
     */
    public Boolean isThrottled() {
        try {
            this.semaphore.acquire();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        for (Boolean isThrottled : results) {
            if (isThrottled) {
                return isThrottled;
            }
        }
        return false;
    }
}
