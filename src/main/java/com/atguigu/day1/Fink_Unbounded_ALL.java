package com.atguigu.day1;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

public class Fink_Unbounded_ALL {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment().setParallelism(1);

        DataStreamSource<String> streamSource = executionEnvironment.socketTextStream("hadoop102", 9999);

        streamSource.flatMap((FlatMapFunction<String, Tuple2<String, Long>>) (value, out) -> {
            for (String s : value.split(" ")) {
                    out.collect(Tuple2.of(s,1L));
            }
        }).returns(Types.TUPLE(Types.STRING,Types.LONG)).keyBy(0).sum(1).print();

        executionEnvironment.execute();
    }
}
