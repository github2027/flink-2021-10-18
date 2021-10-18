package com.atguigu.day02;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.util.Properties;

public class Flink03_Source_Kafka {

    public static void main(String[] args) throws Exception {

        Properties properties = new Properties();

        properties.setProperty("bootstrap.servers","hadoop102:9092,hadoop103:9092,hadoop104:9092" );

        properties.setProperty("group.id", "Flink01_Source_Kafka");

        properties.setProperty("auto.offset.reset", "latest");

        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStreamSource<String> kafkaSource = executionEnvironment.addSource(new FlinkKafkaConsumer<>("sensor", new SimpleStringSchema(), properties));

        kafkaSource.print();

        executionEnvironment.execute();

    }
}
