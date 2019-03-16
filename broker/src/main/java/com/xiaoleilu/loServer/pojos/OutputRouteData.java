/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.pojos;

import java.util.List;

public class OutputRouteData { ;
    private List<String> serverIPs;
    private int longPort;
    private int shortPort;
    private String secret;

    public OutputRouteData(List<String> serverIPs, int longPort, int shortPort, String secret) {
        this.serverIPs = serverIPs;
        this.longPort = longPort;
        this.shortPort = shortPort;
        this.secret = secret;
    }

    public int getLongPort() {
        return longPort;
    }

    public void setLongPort(int longPort) {
        this.longPort = longPort;
    }

    public int getShortPort() {
        return shortPort;
    }

    public void setShortPort(int shortPort) {
        this.shortPort = shortPort;
    }

    public List<String> getServerIPs() {
        return serverIPs;
    }

    public void setServerIPs(List<String> serverIPs) {
        this.serverIPs = serverIPs;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
