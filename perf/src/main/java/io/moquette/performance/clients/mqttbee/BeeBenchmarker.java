package io.moquette.performance.clients.mqttbee;

import io.reactivex.Flowable;
import io.reactivex.Single;
import org.mqttbee.api.mqtt.MqttClient;
import org.mqttbee.api.mqtt.datatypes.MqttQos;
import org.mqttbee.api.mqtt.mqtt3.Mqtt3Client;
import org.mqttbee.api.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import org.mqttbee.api.mqtt.mqtt3.message.publish.Mqtt3Publish;
import org.mqttbee.api.mqtt.mqtt3.message.publish.Mqtt3PublishResult;
import org.mqttbee.api.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck;
import org.mqttbee.rx.FlowableWithSingle;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BeeBenchmarker {

    public static void main(String[] args) {
        final Mqtt3Client client = MqttClient.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost("localhost")
            .serverPort(1883)
            .useMqttVersion3()
            .buildReactive();

        /*
         * As we use the reactive API, the following line does not connect yet, but return a reactive type.
         * e.g. Single is something like a lazy and reusable future.
         */
        final Single<Mqtt3ConnAck> connAckSingle = client.connect().keepAlive(10, TimeUnit.SECONDS).done();

//        /*
//         * Same here: the following line does not subscribe yet, but return a reactive type.
//         * FlowableWithSingle is a combination of the single SUBACK message and a Flowable of PUBLISH messages.
//         * A Flowable is an asynchronous stream, that enables back pressure from the application over MQTT Bee to the broker.
//         */
//        final FlowableWithSingle<Mqtt3SubAck, Mqtt3Publish> subAckAndMatchingPublishes = client.subscribeWithStream()
//            .addSubscription().topicFilter("a/b/c").qos(MqttQos.AT_LEAST_ONCE).done()
//            .addSubscription().topicFilter("a/b/c/d").qos(MqttQos.EXACTLY_ONCE).done()
//            .done();
//
//        /*
//         * The reactive types offer as many operators that will not be covered here.
//         * Here we register simple callbacks to print messages when we received the CONNACK, SUBACK and matching PUBLISH messages.
//         */
//        final Single<Mqtt3ConnAck> connectScenario = connAckSingle
//            .doOnSuccess(connAck -> System.out.println("Connected with return code " + connAck.getReturnCode()))
//            .doOnError(throwable -> System.out.println("Connection failed, " + throwable.getMessage()));
//
//        final Flowable<Mqtt3Publish> subscribeScenario = subAckAndMatchingPublishes
//            .doOnSingle((subAck, subscription) ->
//                System.out.println("Subscribed with return codes " + subAck.getReturnCodes()))
//            .doOnNext(publish ->
//                System.out.println("Received publish" +
//                    ", topic: " + publish.getTopic() +
//                    ", QoS: " + publish.getQos() +
//                    ", payload: " + new String(publish.getPayloadAsBytes())
//                )
//            );
//
//        /*
//         * Reactive types let us compose a sequence of actions
//         */
//        final Flowable<Mqtt3Publish> connectAndSubscribe = connectScenario.toCompletable().andThen(subscribeScenario);
//
//        /*
//         * By subscribing to reactive types, the sequence is executed
//         */
//        connectAndSubscribe.blockingSubscribe();



        /*
         * Fake a stream of PUBLISH messages to publish with incrementing ids
         */
        final AtomicInteger counter = new AtomicInteger();
        Flowable<Mqtt3Publish> messagesToPublish = Flowable.generate(emitter -> {
            final int id = counter.incrementAndGet();
            final Mqtt3Publish publishMessage = Mqtt3Publish.builder()
                .topic("a/b/c").qos(MqttQos.AT_LEAST_ONCE).payload(("test " + id).getBytes()).build();
            emitter.onNext(publishMessage);
        });
        /*
         * Emit 1 message only every 100 milliseconds
         */
        messagesToPublish = messagesToPublish.zipWith(Flowable.interval(10, TimeUnit.MILLISECONDS), (publish, aLong) -> publish);

        final Single<Mqtt3ConnAck> connectScenario = connAckSingle
            .doOnSuccess(connAck -> System.out.println("Connected with return code " + connAck.getReturnCode()))
            .doOnError(throwable -> System.out.println("Connection failed, " + throwable.getMessage()));

        final Flowable<Mqtt3PublishResult> publishScenario = client.publish(messagesToPublish)
            .doOnNext(publishResult -> System.out.println("Publish acknowledged: " + new String(publishResult.getPublish().getPayloadAsBytes())));

        connectScenario.toCompletable().andThen(publishScenario).blockingSubscribe();

    }
}
