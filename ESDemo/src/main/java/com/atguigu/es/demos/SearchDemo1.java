package com.atguigu.es.demos;

import com.atguigu.es.beans.Emp;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.AvgAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.List;

/**
 * Created by Smexy on 2022/4/29
 *
 *
 *      ③准备操作命令Action
 *
 *              读:  Search
 *              写:  Index(insert和update的合体，存在就更新，不存在就插入)
 *                   Delete
 *
 *              批量写: Bulk
 *
 *            所有的Action实现都提供了建造者模式。
 *                  获取Search，不是new Search()，而是 new SearchBulider().build()
 *
 */
public class SearchDemo1 {

    public static void main(String[] args) throws IOException {

        JestClientFactory jestClientFactory = new JestClientFactory();

        //设置集群地址
        jestClientFactory.setHttpClientConfig((new HttpClientConfig.Builder("http://hadoop102:9200")).build());

        //创建一个客户端,连接服务端
        JestClient jestClient = jestClientFactory.getObject();

        //构造 match:{}
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("hobby", "购物");

        // 构造 gendercount:{}
        TermsAggregationBuilder agg1 = AggregationBuilders.terms("gendercount").size(2).field("gender.keyword");

        AvgAggregationBuilder agg2 = AggregationBuilders.avg("avgage").field("age");

        // 构造外层 {}
        String requestBody = new SearchSourceBuilder().query(matchQueryBuilder).aggregation(agg1).aggregation(agg2).toString();

        //准备命令 Action
        Search search = new Search.Builder(requestBody).addIndex("test").addType("emps").build();

        SearchResult searchResult = jestClient.execute(search);

        System.out.println("total:"+searchResult.getTotal());
        System.out.println("max_score:"+searchResult.getMaxScore());
        List<SearchResult.Hit<Emp, Void>> hits = searchResult.getHits(Emp.class);

        for (SearchResult.Hit<Emp, Void> hit : hits) {
            System.out.println("_index"+hit.index);
            System.out.println("_type"+hit.type);
            System.out.println("_id"+hit.id);
            System.out.println("_score"+hit.score);
            System.out.println("_source"+hit.source);
        }

        MetricAggregation aggregations = searchResult.getAggregations();

        TermsAggregation gendercount = aggregations.getTermsAggregation("gendercount");

        List<TermsAggregation.Entry> buckets = gendercount.getBuckets();

        for (TermsAggregation.Entry bucket : buckets) {
            System.out.println(bucket.getKey()+":"+bucket.getCount());
        }

        AvgAggregation avgage = aggregations.getAvgAggregation("avgage");

        System.out.println("平均年龄"+avgage.getAvg());


        //关闭连接
        jestClient.close();




    }
}
