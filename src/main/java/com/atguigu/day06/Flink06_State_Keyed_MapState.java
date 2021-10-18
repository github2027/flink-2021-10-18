package com.atguigu.day06;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.AggregatingState;
import org.apache.flink.api.common.state.AggregatingStateDescriptor;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

public class Flink06_State_Keyed_MapState {

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
        //TODO 去重:去掉重复的水位值，思路：把水位值作为MapState的key来实现去重，value随意
        keyedStream.process(new KeyedProcessFunction<String, WaterSensor, WaterSensor>() {
            private MapState<Integer,WaterSensor> mapState;

            @Override
            public void open(Configuration parameters) throws Exception {
                mapState = getRuntimeContext().getMapState(new MapStateDescriptor<Integer, WaterSensor>("map-state", Integer.class, WaterSensor.class));
            }

            @Override
            public void processElement(WaterSensor value, Context ctx, Collector<WaterSensor> out) throws Exception {
                //拿当前的水位去状态中判断是否存在相同的key
                if(!mapState.contains(value.getVc())){
                   //证明这个vc不存在
                    out.collect(value);
                    mapState.put(value.getVc(),value);
                }
            }
        }).print();


        env.execute();
    }
}
