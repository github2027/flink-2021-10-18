package com.atguigu.day02;

import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Arrays;
import java.util.List;

public class Flink01_Source_Collection {
    public static void main(String[] args) throws Exception {

        List<WaterSensor> waterSensors = Arrays.asList(
                new WaterSensor("ws_001", 1577844001L, 45),
                new WaterSensor("ws_002", 1577844015L, 43),
                new WaterSensor("ws_003", 1577844020L, 42)

        );

        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment().setParallelism(1);

        DataStreamSource<WaterSensor> waterSensorDataStreamSource = executionEnvironment.fromCollection(waterSensors);

        waterSensorDataStreamSource.print();

        executionEnvironment.execute();
    }

}
