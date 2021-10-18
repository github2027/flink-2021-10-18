package com.atguigu.day1;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * 1.配置文件
 * 2.执行任务时配置
 * 3.代码中设置全局的并行度
 * 4.代码中每个算子中设置的并行度
 */
public class Flink_Unbounded {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStreamSource<String> streamSource = executionEnvironment.socketTextStream("hadoop102", 9999);

        SingleOutputStreamOperator<Tuple2<String, Integer>> step1 = streamSource.flatMap((FlatMapFunction<String, Tuple2<String, Integer>>) (value, out) -> {
            String[] strings = value.split(" ");

            for (String string : strings) {
                out.collect(new Tuple2<>(string, 1));
            }
        }).returns(Types.TUPLE(Types.STRING,Types.INT));
        KeyedStream<Tuple2<String, Integer>, Tuple> step2 = step1.keyBy(0);

        SingleOutputStreamOperator<Tuple2<String, Integer>> step3 = step2.sum(1);

        step3.print();

        executionEnvironment.execute();
    }
}
