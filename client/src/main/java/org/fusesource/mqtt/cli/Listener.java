/**
 * Copyright (C) 2010-2012, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.mqtt.cli;

import org.fusesource.mqtt.client.*;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.mqtt.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Listener {

    private final MQTT mqtt = new MQTT();
    private final ArrayList<Topic> topics = new ArrayList<Topic>();
    private boolean debug;
    private boolean showTopic;

    private static void displayHelpAndExit(int exitCode) {
        stdout("");
        stdout("This is a simple mqtt client that will subscribe to topics and print all messages it receives.");
        stdout("");
        stdout("Arguments: [-h host] [-k keepalive] [-c] [-i id] [-u username [-p password]]");
        stdout("           [--will-topic topic [--will-payload payload] [--will-qos qos] [--will-retain]]");
        stdout("           [-d] [-s]");
        stdout("           ( [-q qos] -t topic )+");
        stdout("");
        stdout("");
        stdout(" -h : mqtt host uri to connect to. Defaults to tcp://localhost:1883.");
        stdout(" -k : keep alive in seconds for this client. Defaults to 60.");
        stdout(" -c : disable 'clean session' (store subscription and pending messages when client disconnects).");
        stdout(" -i : id to use for this client. Defaults to a random id.");
        stdout(" -u : provide a username (requires MQTT 3.1 broker)");
        stdout(" -p : provide a password (requires MQTT 3.1 broker)");
        stdout(" --will-topic : the topic on which to publish the client Will.");
        stdout(" --will-payload : payload for the client Will, which is sent by the broker in case of");
        stdout("                  unexpected disconnection. If not given and will-topic is set, a zero");
        stdout("                  length message will be sent.");
        stdout(" --will-qos : QoS level for the client Will.");
        stdout(" --will-retain : if given, make the client Will retained.");
        stdout(" -d : dispaly debug info on stderr");
        stdout(" -s : show message topics in output");
        stdout(" -q : quality of service level to use for the subscription. Defaults to 0.");
        stdout(" -t : mqtt topic to subscribe to. May be repeated multiple times.");
        stdout(" -v : MQTT version to use 3.1 or 3.1.1. (default: 3.1)");
        stdout("");
        System.exit(exitCode);
    }

    private static void stdout(Object x) {
        System.out.println(x);
    }
    private static void stderr(Object x) {
        System.err.println(x);
    }


    private static String shift(LinkedList<String> argl) {
        if(argl.isEmpty()) {
            stderr("Invalid usage: Missing argument");
            displayHelpAndExit(1);
        }
        return argl.removeFirst();
    }
    
    public static void main(String[] args) throws Exception {
        Listener main = new Listener();

        // Process the arguments
        QoS qos = QoS.AT_MOST_ONCE;
        LinkedList<String> argl = new LinkedList<String>(Arrays.asList(args));
        while (!argl.isEmpty()) {
            try {
                String arg = argl.removeFirst();
                if ("--help".equals(arg)) {
                    displayHelpAndExit(0);
                } else if ("-v".equals(arg)) {
                    main.mqtt.setVersion(shift(argl));
                } else if ("-h".equals(arg)) {
                    main.mqtt.setHost(shift(argl));
                } else if ("-k".equals(arg)) {
                    main.mqtt.setKeepAlive(Short.parseShort(shift(argl)));
                } else if ("-c".equals(arg)) {
                    main.mqtt.setCleanSession(false);
                } else if ("-i".equals(arg)) {
                    main.mqtt.setClientId(shift(argl));
                } else if ("-u".equals(arg)) {
                    main.mqtt.setUserName(shift(argl));
                } else if ("-p".equals(arg)) {
                    main.mqtt.setPassword(shift(argl));
                } else if ("--will-topic".equals(arg)) {
                    main.mqtt.setWillTopic(shift(argl));
                } else if ("--will-payload".equals(arg)) {
                    main.mqtt.setWillMessage(shift(argl));
                } else if ("--will-qos".equals(arg)) {
                    int v = Integer.parseInt(shift(argl));
                    if( v > QoS.values().length ) {
                        stderr("Invalid qos value : " + v);
                        displayHelpAndExit(1);
                    }
                    main.mqtt.setWillQos(QoS.values()[v]);
                } else if ("--will-retain".equals(arg)) {
                    main.mqtt.setWillRetain(true);
                } else if ("-d".equals(arg)) {
                    main.debug = true;
                } else if ("-s".equals(arg)) {
                    main.showTopic = true;
                } else if ("-q".equals(arg)) {
                    int v = Integer.parseInt(shift(argl));
                    if( v > QoS.values().length ) {
                        stderr("Invalid qos value : " + v);
                        displayHelpAndExit(1);
                    }
                    qos = QoS.values()[v]; 
                } else if ("-t".equals(arg)) {
                    main.topics.add(new Topic(shift(argl), qos));
                } else {
                    stderr("Invalid usage: unknown option: " + arg);
                    displayHelpAndExit(1);
                }
            } catch (NumberFormatException e) {
                stderr("Invalid usage: argument not a number");
                displayHelpAndExit(1);
            }
        }

        if (main.topics.isEmpty()) {
            stderr("Invalid usage: no topics specified.");
            displayHelpAndExit(1);
        }

        main.execute();
        System.exit(0);
    }

    private void execute() {
        final CallbackConnection connection = mqtt.callbackConnection();

        final CountDownLatch done = new CountDownLatch(1);
        
        // Handle a Ctrl-C event cleanly.
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                setName("MQTT client shutdown");
                if(debug) {
                    stderr("Disconnecting the client.");
                }
                connection.getDispatchQueue().execute(new Task() {
                    public void run() {
                        connection.disconnect(new Callback<Void>() {
                            public void onSuccess(Void value) {
                                done.countDown();
                            }
                            public void onFailure(Throwable value) {
                                done.countDown();
                            }
                        });
                    }
                });
            }
        });
        
        connection.listener(new org.fusesource.mqtt.client.Listener() {

            public void onConnected() {
                if (debug) {
                    stderr("Connected");
                }
            }

            public void onDisconnected() {
                if (debug) {
                    stderr("Disconnected");
                }
            }

            public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {
                try {
                    if (showTopic) {
                        stdout("");
                        stdout("Topic: " + topic);
                        body.writeTo(System.out);
                        stdout("");
                    } else {
                        body.writeTo(System.out);
                    }
                    ack.run();
                } catch (IOException e) {
                    onFailure(e);
                }
            }

            public void onFailure(Throwable value) {
                if (debug) {
                    value.printStackTrace();
                } else {
                    stderr(value);
                }
                System.exit(2);
            }
        });

        connection.resume();
        connection.connect(new Callback<byte[]>() {
            public void onFailure(Throwable value) {
                if (debug) {
                    value.printStackTrace();
                } else {
                    stderr(value);
                }
                System.exit(2);
            }

            public void onSuccess(byte[] value) {
                final Topic[] ta = topics.toArray(new Topic[topics.size()]);
                connection.subscribe(ta, new Callback<byte[]>() {
                    public void onSuccess(byte[] value) {
                        if(debug) {
                            for (int i = 0; i < value.length; i++) {
                                stderr("Subscribed to Topic: " + ta[i].name() + " with QoS: " + QoS.values()[value[i]]);
                            }
                        }                        
                    }
                    public void onFailure(Throwable value) {
                        stderr("Subscribe failed: " + value);
                        if(debug) {
                            value.printStackTrace();
                        }
                        System.exit(2);
                    }
                });
            }
        });

        try {
            done.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);

    }

}
