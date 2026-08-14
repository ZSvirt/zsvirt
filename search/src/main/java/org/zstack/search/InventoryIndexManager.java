package org.zstack.search;

import org.apache.http.client.HttpClient;
import org.zstack.header.Service;

public interface InventoryIndexManager {
    HttpClient getHttpClient();
    
    String getElasticSearchBaseUrl();
}
