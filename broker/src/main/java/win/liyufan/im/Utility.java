/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package win.liyufan.im;

import cn.wildfirechat.common.IMExceptionEvent;
import com.google.gson.Gson;
import com.hazelcast.util.StringUtil;
import io.moquette.server.Server;
import org.slf4j.Logger;

import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Utility {
    private static Map<String, IMExceptionEvent> exceptionEventHashMap = new ConcurrentHashMap<>();
    static AtomicLong SendExceptionExpireTime = new AtomicLong(0);
    static int SendExceptionDuration = 180000;
    static long lastSendTime = System.currentTimeMillis();
    static {
        new Thread(()->{
            while (true) {
                try {
                    Thread.sleep(60 * 1000);
                    sendExceptionEvent();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }
    public static InetAddress getLocalAddress(){
        try {
            Enumeration<NetworkInterface> b = NetworkInterface.getNetworkInterfaces();
            while( b.hasMoreElements()){
                for ( InterfaceAddress f : b.nextElement().getInterfaceAddresses())
                    if ( f.getAddress().isSiteLocalAddress())
                        return f.getAddress();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void printExecption(Logger LOG, Exception e) {
        printExecption(LOG, e, 0);
    }

    public static void printExecption(Logger LOG, Exception e, int eventType) {
        String message = "";

        for(StackTraceElement stackTraceElement : e.getStackTrace()) {
            message = message + System.lineSeparator() + stackTraceElement.toString();
        }
        LOG.error("Exception: {}", e.getMessage());
        LOG.error(message);
        if(eventType > 0) {
            String key = eventType+(e.getMessage() != null ? e.getMessage().split("\n")[0] : "null");
            IMExceptionEvent event = exceptionEventHashMap.get(key);
            if(event == null) {
                event = new IMExceptionEvent();
                event.event_type = eventType;
                event.msg = e.getMessage();
                event.call_stack = message;
                event.count = 0;

                exceptionEventHashMap.put(key, event);
            }
            event.count++;

            sendExceptionEvent();
        }
    }

    private static String gMonitorEventAddress;

    public static void setMonitorEventAddress(String gMonitorEventAddress) {
        Utility.gMonitorEventAddress = gMonitorEventAddress;
    }

    private static void sendExceptionEvent() {
        if(StringUtil.isNullOrEmpty(gMonitorEventAddress)) {
            return;
        }

        long now = System.currentTimeMillis();
        long t = SendExceptionExpireTime.getAndUpdate((long l) -> now - l >= SendExceptionDuration ? now : l);

        if(now - t >= SendExceptionDuration) {
            for (Map.Entry<String, IMExceptionEvent> entry : exceptionEventHashMap.entrySet()) {
                if(entry.getValue().count > 0) {
                    String jsonStr = new Gson().toJson(entry.getValue());
                    Server.getServer().getCallbackScheduler().execute(() -> HttpUtils.httpJsonPost(gMonitorEventAddress, jsonStr, HttpUtils.HttpPostType.POST_TYPE_Server_Exception_Callback));
                    entry.getValue().count = 0;
                    lastSendTime = now;
                }
            }
            if(now - lastSendTime > 24 * 3600 * 1000) {
                lastSendTime = now;
                IMExceptionEvent event = new IMExceptionEvent();
                event.event_type = 0;
                event.msg = "监控心跳通知";
                event.call_stack = "别担心！这个是心跳消息，已经有24小时没有出现异常消息了！";
                event.count = IMExceptionEvent.EventType.HEART_BEAT;
                String jsonStr = new Gson().toJson(event);
                Server.getServer().getCallbackScheduler().execute(() -> HttpUtils.httpJsonPost(gMonitorEventAddress, jsonStr, HttpUtils.HttpPostType.POST_TYPE_Server_Exception_Callback));
            }
        }
    }

    public static String getMacAddress() throws UnknownHostException,
        SocketException {
        InetAddress ipAddress = InetAddress.getLocalHost();
        NetworkInterface networkInterface = NetworkInterface
            .getByInetAddress(ipAddress);
        byte[] macAddressBytes = networkInterface.getHardwareAddress();
        StringBuilder macAddressBuilder = new StringBuilder();

        for (int macAddressByteIndex = 0; macAddressByteIndex < macAddressBytes.length; macAddressByteIndex++) {
            String macAddressHexByte = String.format("%02X",
                macAddressBytes[macAddressByteIndex]);
            macAddressBuilder.append(macAddressHexByte);

            if (macAddressByteIndex != macAddressBytes.length - 1)
            {
                macAddressBuilder.append(":");
            }
        }

        return macAddressBuilder.toString();
    }

    /**
     * 格式化
     *
     * @param jsonStr
     * @return
     * @author lizhgb
     * @Date 2015-10-14 下午1:17:35
     * @Modified 2017-04-28 下午8:55:35
     */
    public static String formatJson(String jsonStr) {
        if (null == jsonStr || "".equals(jsonStr))
            return "";
        StringBuilder sb = new StringBuilder();
        char last = '\0';
        char current = '\0';
        int indent = 0;
        boolean isInQuotationMarks = false;
        for (int i = 0; i < jsonStr.length(); i++) {
            last = current;
            current = jsonStr.charAt(i);
            switch (current) {
                case '"':
                    if (last != '\\'){
                        isInQuotationMarks = !isInQuotationMarks;
                    }
                    sb.append(current);
                    break;
                case '{':
                case '[':
                    sb.append(current);
                    if (!isInQuotationMarks) {
                        sb.append('\n');
                        indent++;
                        addIndentBlank(sb, indent);
                    }
                    break;
                case '}':
                case ']':
                    if (!isInQuotationMarks) {
                        sb.append('\n');
                        indent--;
                        addIndentBlank(sb, indent);
                    }
                    sb.append(current);
                    break;
                case ',':
                    sb.append(current);
                    if (last != '\\' && !isInQuotationMarks) {
                        sb.append('\n');
                        addIndentBlank(sb, indent);
                    }
                    break;
                default:
                    sb.append(current);
            }
        }

        return sb.toString();
    }

    /**
     * 添加space
     *
     * @param sb
     * @param indent
     * @author lizhgb
     * @Date 2015-10-14 上午10:38:04
     */
    private static void addIndentBlank(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append('\t');
        }
    }
}
