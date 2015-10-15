package org.wso2.carbon.cep;

import com.google.common.annotations.VisibleForTesting;
import org.junit.Test;

import java.util.Properties;

public class testCEP {

    @Test
    public void testLocalCEP(){
        LocalCEP localCEP = new LocalCEP();
        Properties properties = new Properties();
        properties.put("name","API1");
        localCEP.addThrottlingType(LocalCEP.ThrottlingType.perAPI, properties);
        localCEP.addThrottlingType(LocalCEP.ThrottlingType.perUser, properties);
        localCEP.init();
    }
}
