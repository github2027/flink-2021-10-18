package com.atguigu.day03;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

public class Flink01_TransForm_Process {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        DataStreamSource<String> streamSource = env.socketTextStream("hadoop102", 9999);

       /* SingleOutputStreamOperator<WaterSensor> map = streamSource.map(new MapFunction<String, WaterSensor>() {
            @Override
            public WaterSensor map(String value) throws Exception {
                String[] split = value.split(",");
                return new WaterSensor(split[0], Long.valueOf(split[1]), Integer.valueOf(split[2]));
            }
        });*/
        SingleOutputStreamOperator<WaterSensor> process = streamSource.process(new ProcessFunction<String, WaterSensor>() {
            @Override
            public void processElement(String value, Context ctx, Collector<WaterSensor> out) throws Exception {
                String[] split = value.split(",");

                out.collect(new WaterSensor(split[0], Long.valueOf(split[1]), Integer.valueOf(split[2])));
            }
        });

        KeyedStream<WaterSensor, String> keyedStream = process.keyBy(r -> r.getId());

       /* SingleOutputStreamOperator<WaterSensor> reduce = keyedStream.reduce(new ReduceFunction<WaterSensor>() {
            @Override
            public WaterSensor reduce(WaterSensor value1, WaterSensor value2) throws Exception {
                return new WaterSensor(value1.getId(), System.currentTimeMillis(), value1.getVc() + value2.getVc());
            }
        });*/
        SingleOutputStreamOperator<Integer> result = keyedStream.process(new KeyedProcessFunction<String, WaterSensor, Integer>() {
            private Integer count = 0;

            @Override
            public void processElement(WaterSensor value, Context ctx, Collector<Integer> out) throws Exception {

                System.out.println("KeyedProcess.....");
                count++;
                out.collect(count);
            }
        });

        result.print("数据个数");

        process.print("经过process将字符串转化成JavaBean");
        env.execute();
    }
}
