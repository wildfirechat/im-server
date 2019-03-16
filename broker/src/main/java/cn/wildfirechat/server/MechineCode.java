/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.server;

import win.liyufan.im.Utility;

import java.util.Base64;

public class MechineCode {
    public static void main(String[] args) throws Exception {
        for (String arg: args
             ) {
            System.out.println(arg);
        }

        if (args.length != 2) {
            System.out.println("Usage: ip port");
            System.exit(-1);
        }
        String mac = Utility.getMacAddress();
        mac += "|";
        mac += args[0];
        mac += "|";
        mac += args[1];
        mac = Base64.getEncoder().encodeToString(mac.getBytes());
        System.out.println(mac);
    }
}
