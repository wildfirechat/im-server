/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.persistence;

import java.io.Serializable;

public class UserClientEntry implements Serializable {
    public UserClientEntry(String userId, String clientId) {
        this.userId = userId;
        this.clientId = clientId;
    }

    public String userId;
    public String clientId;
}
