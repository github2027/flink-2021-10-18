package com.atguigu.day06;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.state.AggregatingState;
import org.apache.flink.api.common.state.AggregatingStateDescriptor;
import org.apache.flink.api.common.state.ReducingState;
import org.apache.flink.api.common.state.ReducingStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.elasticsearch.common.collect.Tuple;

public class Flink05_State_Keyed_AggState {

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
        //TODO 求水位的平均值
        keyedStream.process(new KeyedProcessFunction<String, WaterSensor, Tuple2<String,Double>>() {
            private AggregatingState<Integer,Double> aggregatingState;

            @Override
            public void open(Configuration parameters) throws Exception {
                aggregatingState = getRuntimeContext().getAggregatingState(new AggregatingStateDescriptor<Integer, Tuple2<Integer,Integer>, Double>("aggregating-state", new AggregateFunction<Integer, Tuple2<Integer, Integer>, Double>() {
                    @Override
                    public Tuple2<Integer, Integer> createAccumulator() {
                        //初始化累加器
                        return Tuple2.of(0,0);
                    }

                    @Override
                    public Tuple2<Integer, Integer> add(Integer value, Tuple2<Integer, Integer> accumulator) {
                        return Tuple2.of(accumulator.f0+value, accumulator.f1+1);
                    }

                    @Override
                    public Double getResult(Tuple2<Integer, Integer> accumulator) {
                        return accumulator.f0*1D/accumulator.f1;
                    }

                    @Override
                    public Tuple2<Integer, Integer> merge(Tuple2<Integer, Integer> a, Tuple2<Integer, Integer> b) {
                        return Tuple2.of(a.f0+b.f0,a.f1+b.f1);
                    }
                }, Types.TUPLE(Types.INT,Types.INT)));
            }

            @Override
            public void processElement(WaterSensor value, Context ctx, Collector<Tuple2<String, Double>> out) throws Exception {
                    aggregatingState.add(value.getVc());

                Double avg = aggregatingState.get();

                out.collect(Tuple2.of(value.getId(), avg));
            }
        }).print();

        env.execute();
    }
}
