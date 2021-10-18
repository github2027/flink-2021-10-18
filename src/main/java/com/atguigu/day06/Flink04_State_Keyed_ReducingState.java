package com.atguigu.day06;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.ReducingState;
import org.apache.flink.api.common.state.ReducingStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Flink04_State_Keyed_ReducingState {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        DataStreamSource<String> localStreamSource = env.socketTextStream("localhost", 9999);

        SingleOutputStreamOperator<WaterSensor> waterSensorSingleOutputStreamOperator = localStreamSource.map(new MapFunction<String, WaterSensor>() {
            @Override
            public WaterSensor map(String value) throws Exception {
                String[] split = value.split(",");
                return new WaterSensor(
                        split[0],
                        Long.parseLong(split[1]),
                        Integer.parseInt(split[2])
                );
            }
        });

        KeyedStream<WaterSensor, String> keyedStream = waterSensorSingleOutputStreamOperator.keyBy(r -> r.getId());

        keyedStream.process(new KeyedProcessFunction<String, WaterSensor, Integer>() {
            private ReducingState <Integer> reducingState;

            @Override
            public void open(Configuration parameters) throws Exception {
                reducingState=getRuntimeContext().getReducingState(new ReducingStateDescriptor<Integer>("reducing-state", new ReduceFunction<Integer>() {
                    @Override
                    public Integer reduce(Integer value1, Integer value2) throws Exception {

                        return value1+value2;
                    }
                }, Integer.class));
            }

            @Override
            public void processElement(WaterSensor value, Context ctx, Collector<Integer> out) throws Exception {
                //1.将当前数据存入状态中
                reducingState.add(value.getVc());
                //2.取出聚合计算后的数据
                Integer sum = reducingState.get();
                //3.输出
                out.collect(sum);
            }
        }).print();

        env.execute();
    }
}
