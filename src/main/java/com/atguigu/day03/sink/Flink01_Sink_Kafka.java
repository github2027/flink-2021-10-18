package com.atguigu.day03.sink;

import com.alibaba.fastjson.JSON;
import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.Properties;

public class Flink01_Sink_Kafka {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        DataStreamSource<String> streamSource = env.socketTextStream("hadoop102", 9998);

        SingleOutputStreamOperator<String> result = streamSource.map(new MapFunction<String, String>() {
            @Override
            public String map(String value) throws Exception {
                String[] split = value.split(",");
                WaterSensor waterSensor = new WaterSensor(split[0], Long.valueOf(split[1]), Integer.valueOf(split[2]));
                return JSON.toJSONString(waterSensor);
            }
        });

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers","hadoop102:9092");
        result.addSink(new FlinkKafkaProducer<String>("topic_sensor",new SimpleStringSchema(),properties));

        env.execute();
    }
}
