package com.xiaohunao.iplocationdisplay.location;

import java.util.Optional;

public interface LocationProvider {
    Optional<IpLocation> lookup(String ip) throws Exception;
}
