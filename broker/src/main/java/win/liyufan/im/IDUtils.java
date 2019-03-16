/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package win.liyufan.im;

public class IDUtils {
    private static char getChar(int number) {
        if (number < 0 || number >= 64) {
            number = number % 64;
        }
        if (number < 26) {
            return (char)('A' + number);
        } else if(number < 52) {
            return (char)('a' + number - 26);
        } else if(number < 62) {
            return (char)('0' + number - 52);
        } else if(number == 62) {
            return '_';
        }
        return '-';
    }

    private static int getNumber(char ch) {

        if (ch >= 'A' && ch <= 'Z') {
            return ch - 'A';
        } else if(ch >= 'a' && ch <= 'z') {
            return ch - 'a' + 26;
        } else if(ch >= '0' && ch <= '9') {
            return ch - '0' + 52;
        } else if(ch == '_') {
            return 62;
        }
        return 63;
    }

    public static String toUid(int id) {
        StringBuilder sb = new StringBuilder();
        int rand0 = 0;
        int rand3 = 0;
        int rand6 = 0;
        for (int i = 0; i < 8; i++) {
            if (i == 0 || i == 3 || i == 6) {
                int rand = (int)(Math.random()*64+0.5);
                sb.append(getChar(rand));
                if (i == 0) {
                    rand0 = rand;
                } else if (i == 3) {
                    rand3 = rand;
                } else {
                    rand6 = rand;
                }
            } else {
                if (i < 5) {
                    sb.append(getChar((id + rand0) % 64));
                } else if(i == 5) {
                    sb.append(getChar((id + rand3) % 64));
                } else {
                    sb.append(getChar((id + rand6) % 64));
                }
                id = id / 64;
            }
        }
        return sb.toString();
    }

    public static int fromUid(String uid) {
        int id = 0;
        int rand0 = 0;
        int rand3 = 0;
        int rand6 = 0;
        for (int i = 0; i < 8; i++) {
            if (i == 0 || i == 3 || i == 6) {
                int rand = getNumber(uid.charAt(i));
                if (i == 0) {
                    rand0 = rand;
                } else if (i == 3) {
                    rand3 = rand;
                } else {
                    rand6 = rand;
                }
            } else {
                int c = getNumber(uid.charAt(i));

                if (i < 5) {
                    c = (c+64-rand0)%64;
                } else if(i == 5) {
                    c = (c+64-rand3)%64;
                } else {
                    c = (c+64-rand6)%64;
                }
                if (i < 3)
                    id = id + (c << (i-1)*6);
                else if(i < 6)
                    id = id + (c << (i-2)*6);
                else
                    id = id + (c << (i-3)*6);
            }
        }
        return id;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100000000; i++) {
            String uid = toUid(i);
            System.out.println(i + ", " + uid + ", " + fromUid(uid));
        }
    }
}
