package com.atguigu.day02;

import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class Flink02_Source_File {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStreamSource<String> stringDataStreamSource = executionEnvironment.readTextFile("input/word.txt");

        stringDataStreamSource.print();

        executionEnvironment.execute();
    }
}
