/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OutputCheckUserOnline {
    public static class Session {
        public String clientId;
        public String userId;
        public int platform;
        public int status; //0 online, 1 have session offline
        public long lastSeen;

        public Session(String clientId, String userId, int platform, int status, long lastSeen) {
            this.clientId = clientId;
            this.userId = userId;
            this.platform = platform;
            this.status = status;
            this.lastSeen = lastSeen;
        }
    }

    public void addSession(String userId, String clientId, int platform, int status, long lastSeen) {
        Session session = new Session(clientId, userId, platform, status, lastSeen);
        sessions.add(session);
    }

    private List<Session> sessions = new ArrayList<>();

    public List<Session> getSessions() {
        return sessions;
    }

    public void setSessions(List<Session> sessions) {
        this.sessions = sessions;
    }
}
