package com.atguigu.day03.sink;

import com.alibaba.fastjson.JSON;
import com.atguigu.day02.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.xcontent.XContentType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Flink03_Sink_ES {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        DataStreamSource<String> streamSource = env.socketTextStream("hadoop102", 9999);

        SingleOutputStreamOperator<String> result = streamSource.map(new MapFunction<String, String>() {
            @Override
            public String map(String value) throws Exception {
                String[] split = value.split(",");
                WaterSensor waterSensor = new WaterSensor(split[0], Long.valueOf(split[1]), Integer.valueOf(split[2]));
                return JSON.toJSONString(waterSensor);
            }
        });

        List<HttpHost> httpHosts = Arrays.asList(
                new HttpHost("hadoop102",9200),
                new HttpHost("hadoop103",9200),
                new HttpHost("hadoop104",9200)
        );
        ElasticsearchSink.Builder<String> stringBuilder = new ElasticsearchSink.Builder<>(httpHosts, new ElasticsearchSinkFunction<String>() {
            @Override
            public void process(String element, RuntimeContext ctx, RequestIndexer indexer) {
                IndexRequest indexRequest = new IndexRequest("flink_0526", "_doc", "1001").source(element, XContentType.JSON);

                indexer.add(indexRequest);
            }
        });
        stringBuilder.setBulkFlushMaxActions(1);
        result.addSink(stringBuilder.build());


        env.execute();
    }
   /* public static void main(String[] args) throws Exception {
        ArrayList<WaterSensor> waterSensors = new ArrayList<>();
        waterSensors.add(new WaterSensor("sensor_1", 1607527992000L, 20));
        waterSensors.add(new WaterSensor("sensor_1", 1607527994000L, 50));
        waterSensors.add(new WaterSensor("sensor_1", 1607527996000L, 50));
        waterSensors.add(new WaterSensor("sensor_2", 1607527993000L, 10));
        waterSensors.add(new WaterSensor("sensor_2", 1607527995000L, 30));

        List<HttpHost> esHosts = Arrays.asList(
                new HttpHost("hadoop102", 9200),
                new HttpHost("hadoop103", 9200),
                new HttpHost("hadoop104", 9200));
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment().setParallelism(1);
        env
                .fromCollection(waterSensors)
                .addSink(new ElasticsearchSink.Builder<WaterSensor>(esHosts, new ElasticsearchSinkFunction<WaterSensor>() {

                    @Override
                    public void process(WaterSensor element, RuntimeContext ctx, RequestIndexer indexer) {
                        // 1. 创建es写入请求
                        IndexRequest request = Requests
                                .indexRequest("sensor")
                                .type("_doc")
                                .id(element.getId())
                                .source(JSON.toJSONString(element), XContentType.JSON);
                        // 2. 写入到es
                        indexer.add(request);
                    }
                }).build());

        env.execute();
    }*/

}
