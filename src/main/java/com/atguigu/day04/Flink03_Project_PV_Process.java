package com.atguigu.day04;


import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import scala.Int;

public class Flink03_Project_PV_Process {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment().setParallelism(1);

        DataStreamSource<String> streamSource = env.readTextFile("input/UserBehavior.csv");

        SingleOutputStreamOperator<Tuple2<String, Integer>> result = streamSource.process(new ProcessFunction<String, Tuple2<String, Integer>>() {
            private Integer count = 0;

            @Override
            public void processElement(String value, Context ctx, Collector<Tuple2<String, Integer>> out) throws Exception {

                String[] split = value.split(",");

                UserBehavior userBehavior = new UserBehavior(Long.parseLong(split[0]), Long.parseLong(split[1]), Integer.parseInt(split[2]), split[3], Long.parseLong(split[4]));

                if ("pv".equals(userBehavior.getBehavior())) {
                    count++;
                    out.collect(new Tuple2("pv", count));
                }
            }
        });
        result.print();
        env.execute();
    }
}
