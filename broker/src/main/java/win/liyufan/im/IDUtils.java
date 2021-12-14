/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package win.liyufan.im;

import java.util.HashMap;
import java.util.HashSet;

public class IDUtils {
    private static final int RADIX = 36;
    private static final int IDLEN = 9;
    private static final int[] FACTOR = {29,19,1,11,7,17,13,23,5};

    private static char getChar(long number) {
        if (number < 26) {
            return (char)('a' + number);
        } else {
            return (char)('0' + number - 26);
        }
    }

    public static String toUid(long id) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < IDLEN; i++) {
            long bit = ((id % RADIX) * FACTOR[i] + FACTOR[i] + 5)%RADIX;
            sb.append(getChar(bit));
            id = id / RADIX;
        }
        return sb.toString();
    }
    
    public static void main(String[] args) {
        HashSet<String> idset = new HashSet<>();
        for (long i = 0; i < 1000000000000L; i++) {
            String uid = toUid(i);
            if(!idset.add(uid)) {
                System.out.println("error, dupliate id");
                System.exit(-1);
            }
        }
    }
}
