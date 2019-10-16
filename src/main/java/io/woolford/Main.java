package io.woolford;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;

import java.io.IOException;
import java.util.*;

public class Main {

    public static void main(String[] args) {

        // set props for Kafka Steams app (see KafkaConstants)
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, KafkaConstants.APPLICATION_ID);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.KAFKA_BROKERS);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(StreamsConfig.PRODUCER_PREFIX + ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor");
        props.put(StreamsConfig.CONSUMER_PREFIX + ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor");

        final StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> processPortStream = builder.stream("process-port");

        processPortStream.map((key, value) -> {

            // create mapper that will ignore JSON elements if they don't exist in the POJO.
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // OSquery outputs a header record, that contains a map of columns. First we parse the header record into a POJO.
            OsqueryDifferentialHeader osqueryDifferentialHeader = null;
            try {
                osqueryDifferentialHeader = mapper.readValue(value, OsqueryDifferentialHeader.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // The columns, from the header record, are mapped to the process record.
            ProcessPortRecord processPortRecord = mapper.convertValue(osqueryDifferentialHeader.getColumns(), ProcessPortRecord.class);

            // Create a key comprising the source and destination addresses and ports.
            List<String> keyElements = Arrays.asList(
                    processPortRecord.getLocalAddress(),
                    processPortRecord.getLocalPort(),
                    processPortRecord.getRemoteAddress(),
                    processPortRecord.getRemotePort());

            String compositeKey = String.join(":", keyElements);

            // Create a JSON string from the columns.
            String processPortRecordJson = null;
            try {
                processPortRecordJson = mapper.writeValueAsString(processPortRecord);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            // The processPortRecordSubset contains just those fields we want to attach. We're effectively taking the
            // value, and moving the source/destination ip/ports components to the key.
            ProcessPortRecordSubset processPortRecordSubset = null;
            String processPortRecordSubsetJson = null;
            try {
                processPortRecordSubset = mapper.readValue(processPortRecordJson, ProcessPortRecordSubset.class);
                processPortRecordSubsetJson = mapper.writeValueAsString(processPortRecordSubset);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // If the action is "removed", that is effectively a delete, in which case the value of
            // a deleted key will be null.
            String action = osqueryDifferentialHeader.getAction(); // removed or added
            if (action.equals("removed"))  {
                processPortRecordSubsetJson = null;
            }

            KeyValue keyValue = new KeyValue<>(compositeKey, processPortRecordSubsetJson);

            return keyValue;
        }).to("process-port-keyed");

        // run it
        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        streams.start();

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

    }

}
