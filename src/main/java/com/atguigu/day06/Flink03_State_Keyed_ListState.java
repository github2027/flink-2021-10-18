package com.atguigu.day06;

import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
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

public class Flink03_State_Keyed_ListState {

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

        keyedStream.process(new KeyedProcessFunction<String, WaterSensor, List<Integer>>() {
            //TODO 定义状态
            private ListState<Integer> listState;

            @Override
            public void open(Configuration parameters) throws Exception {
                //TODO 初始化状态
                listState=getRuntimeContext().getListState(new ListStateDescriptor<Integer>("list-state", Integer.class));
            }

            @Override
            public void processElement(WaterSensor value, Context ctx, Collector<List<Integer>> out) throws Exception {

                //1.把当前水位保存到状态中
                listState.add(value.getVc());
                //2.创建List集合用来保存从状态中提取的数据
                ArrayList<Integer> list = new ArrayList<>();

                Iterable<Integer> integers = listState.get();

                for (Integer integer : integers) {
                    list.add(integer);
                }
                //3.对list集合中的数据做排序,按照从大到小排序
                list.sort(new Comparator<Integer>() {
                    @Override
                    public int compare(Integer o1, Integer o2) {
                        return o2-o1;
                    }
                });
                //4.将下标为3的也就是排第四的水位值删除
                if(list.size()>3){list.remove(3);}
                out.collect(list);

                //5.更新状态
                listState.update(list);
            }
        }).print();

        env.execute();
    }
}
