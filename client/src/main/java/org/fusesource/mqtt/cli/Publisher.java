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

import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.ByteArrayOutputStream;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.hawtdispatch.Task;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Publisher {

    private final MQTT mqtt = new MQTT();
    private QoS qos = QoS.AT_MOST_ONCE;
    private UTF8Buffer topic;
    private Buffer body;
    private boolean debug;
    private boolean retain;
    private long count = 1;
    private long sleep;
    private boolean prefixCounter;

    private static void displayHelpAndExit(int exitCode) {
        stdout("");
        stdout("This is a simple mqtt client that will publish to a topic.");
        stdout("");
        stdout("Arguments: [-h host] [-k keepalive] [-c] [-i id] [-u username [-p password]]");
        stdout("           [--will-topic topic [--will-payload payload] [--will-qos qos] [--will-retain]]");
        stdout("           [-d] [-n count] [-s sleep] [-q qos] [-r] -t topic ( -pc | -m message | -z | -f file )");
        stdout("");
        stdout("");
        stdout(" -h : mqtt host uri to connect to. Defaults to tcp://localhost:1883.");
        stdout(" -k : keep alive in seconds for this client. Defaults to 60.");
        stdout(" -c : disable 'clean session'.");
        stdout(" -i : id to use for this client. Defaults to a random id.");
        stdout(" -u : provide a username (requires MQTT 3.1 broker)");
        stdout(" -p : provide a password (requires MQTT 3.1 broker)");
        stdout(" --will-topic : the topic on which to publish the client Will.");
        stdout(" --will-payload : payload for the client Will, which is sent by the broker in case of");
        stdout("                  unexpected disconnection. If not given and will-topic is set, a zero");
        stdout("                  length message will be sent.");
        stdout(" --will-qos : QoS level for the client Will.");
        stdout(" --will-retain : if given, make the client Will retained.");
        stdout(" -d : display debug info on stderr");
        stdout(" -n : the number of times to publish the message");
        stdout(" -s : the number of milliseconds to sleep between publish operations (defaut: 0)");
        stdout(" -q : quality of service level to use for the publish. Defaults to 0.");
        stdout(" -r : message should be retained.");
        stdout(" -t : mqtt topic to publish to.");
        stdout(" -m : message payload to send.");
        stdout(" -z : send a null (zero length) message.");
        stdout(" -f : send the contents of a file as the message.");
        stdout(" -pc : prefix a message counter to the message");
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
        Publisher main = new Publisher();

        // Process the arguments
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
                } else if ("-n".equals(arg)) {
                    main.count =  Long.parseLong(shift(argl));
                } else if ("-s".equals(arg)) {
                    main.sleep =  Long.parseLong(shift(argl));
                } else if ("-q".equals(arg)) {
                    int v = Integer.parseInt(shift(argl));
                    if( v > QoS.values().length ) {
                        stderr("Invalid qos value : " + v);
                        displayHelpAndExit(1);
                    }
                    main.qos = QoS.values()[v];
                } else if ("-r".equals(arg)) {
                    main.retain = true;
                } else if ("-t".equals(arg)) {
                    main.topic = new UTF8Buffer(shift(argl));
                } else if ("-m".equals(arg)) {
                    main.body = new UTF8Buffer(shift(argl)+"\n");
                } else if ("-z".equals(arg)) {
                    main.body = new UTF8Buffer("");
                } else if ("-f".equals(arg)) {
                    File file = new File(shift(argl));
                    RandomAccessFile raf = new RandomAccessFile(file, "r");
                    try {
                        byte[] data = new byte[(int) raf.length()];
                        raf.seek(0);
                        raf.readFully(data);
                        main.body = new Buffer(data);
                    } finally {
                        raf.close();
                    }
                } else if ("-pc".equals(arg)) {
                    main.prefixCounter = true;
                } else {
                    stderr("Invalid usage: unknown option: " + arg);
                    displayHelpAndExit(1);
                }
            } catch (NumberFormatException e) {
                stderr("Invalid usage: argument not a number");
                displayHelpAndExit(1);
            }
        }

        if (main.topic == null) {
            stderr("Invalid usage: no topic specified.");
            displayHelpAndExit(1);
        }
        if (main.body == null) {
            stderr("Invalid usage: -z -m or -f must be specified.");
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
        
        connection.listener(new Listener() {

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
            }
        });

        new Task() {
            private long sent = 0;
            public void run() {
                final Task publish = this;
                Buffer message  = body;
                if(prefixCounter) {
                    long id = sent + 1;
                    ByteArrayOutputStream os = new ByteArrayOutputStream(message.length + 15);
                    os.write(new AsciiBuffer(Long.toString(id)));
                    os.write(':');
                    os.write(body);
                    message = os.toBuffer();
                }
                connection.publish(topic, message, qos, retain, new Callback<byte[]>() {
                    public void onSuccess(byte[] value) {
                        sent ++;
                        if(debug) {
                            stdout("Sent message #"+sent);
                        }
                        if( sent < count ) {
                            if(sleep>0) {
                                System.out.println("Sleeping");
                                connection.getDispatchQueue().executeAfter(sleep, TimeUnit.MILLISECONDS, publish);
                            } else {
                                connection.getDispatchQueue().execute(publish);
                            }
                        } else {
                            connection.disconnect(new Callback<Void>() {
                                public void onSuccess(Void value) {
                                    done.countDown();
                                }
                                public void onFailure(Throwable value) {
                                    done.countDown();
                                }
                            });                            
                        }
                    }
                    public void onFailure(Throwable value) {
                        stderr("Publish failed: " + value);
                        if(debug) {
                            value.printStackTrace();
                        }
                        System.exit(2);
                    }
                });
            }
        }.run();

        try {
            done.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

}
