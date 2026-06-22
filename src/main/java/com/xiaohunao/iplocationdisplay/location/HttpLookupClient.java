package com.xiaohunao.iplocationdisplay.location;

import java.time.Duration;

public interface HttpLookupClient {
    HttpLookupResponse get(String url, Duration timeout) throws Exception;
}
