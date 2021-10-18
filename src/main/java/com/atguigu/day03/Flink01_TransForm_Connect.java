package com.atguigu.day03;

import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class Flink01_TransForm_Connect {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment().setParallelism(1);

        DataStreamSource<Integer> integerDataStreamSource = executionEnvironment.fromElements(1, 2, 3, 4, 5);

        DataStreamSource<String> stringDataStreamSource = executionEnvironment.fromElements("a", "b", "c");

        ConnectedStreams<Integer, String> connect = integerDataStreamSource.connect(stringDataStreamSource);

        DataStreamSink<Integer> first = connect.getFirstInput().print("first");

        DataStreamSink<String> second = connect.getSecondInput().print("second");

        executionEnvironment.execute();
    }
}
