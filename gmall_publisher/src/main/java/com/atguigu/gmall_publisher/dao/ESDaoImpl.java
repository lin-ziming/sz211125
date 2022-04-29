package com.atguigu.gmall_publisher.dao;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall_publisher.bean.Option;
import com.atguigu.gmall_publisher.bean.SaleDetail;
import com.atguigu.gmall_publisher.bean.Stat;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Smexy on 2022/4/29
 */
@Repository
public class ESDaoImpl implements ESDao {

    //es客户端
    @Autowired
    private JestClient jestClient;

    /*
            查询ES，获取返回值

GET /gmall2022_sale_detail2022-04-29/_search
{
  "query": {
    "match": {
      "sku_name": "手机"
    }

  },
  "aggs": {
    "gendercount": {
      "terms": {
        "field": "user_gender",
        "size": 2
      }
    },
    "agecount":{
      "terms": {
        "field": "user_age",
        "size": 150
      }
    }
  }
  ,
  from: x,
  size : x
}
     */
    public SearchResult queryES(String date111, Integer startpage, Integer size, String keyword) throws IOException {

        String indexName = "gmall2022_sale_detail" + date111;

        // 一页要展示多少条数据，就应该返回多少条数据
        int from = (startpage - 1) * size;

        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("sku_name", keyword);

        TermsBuilder agg1 = AggregationBuilders.terms("gendercount").size(2).field("user_gender");
        TermsBuilder agg2 = AggregationBuilders.terms("agecount").size(150).field("user_age");

        String requestBody = new SearchSourceBuilder().query(matchQueryBuilder).aggregation(agg1).aggregation(agg2).from(from).size(size).toString();

        Search search = new Search.Builder(requestBody).addIndex(indexName).addType("_doc").build();

        return jestClient.execute(search);


    }

    // 解析返回值，得到detail:[SaleDetail...]
        public List<SaleDetail> getDetail(SearchResult searchResult){

            ArrayList<SaleDetail> result = new ArrayList<>();

            List<SearchResult.Hit<SaleDetail, Void>> hits = searchResult.getHits(SaleDetail.class);

            for (SearchResult.Hit<SaleDetail, Void> hit : hits) {

                SaleDetail source = hit.source;

                source.setEs_metadata_id(hit.id);

                result.add(source);

            }

            return result;

        }

    // 解析返回值，得到年龄stat
    public Stat getAgeStat(SearchResult searchResult){


        MetricAggregation aggregations = searchResult.getAggregations();

        TermsAggregation agecount = aggregations.getTermsAggregation("agecount");

        List<TermsAggregation.Entry> buckets = agecount.getBuckets();

        Long from20To30Count = 0l;
        Long ge30Count = 0l;
        Long lt20Count = 0l;

        for (TermsAggregation.Entry bucket : buckets) {

            if (Integer.parseInt(bucket.getKey()) < 20){

                lt20Count += bucket.getCount();

            }else if (Integer.parseInt(bucket.getKey()) >= 30){

                ge30Count += bucket.getCount();

            }else{

                from20To30Count += bucket.getCount();

            }

        }

        Long totalCount = from20To30Count + ge30Count + lt20Count;

        DecimalFormat decimalFormat = new DecimalFormat("###.##%");

        ArrayList<Option> options = new ArrayList<>();

        options.add(new Option("20岁到30岁",decimalFormat.format(from20To30Count * 1.0d / totalCount)));
        options.add(new Option("30岁及30岁以上",decimalFormat.format(ge30Count * 1.0d / totalCount)));
        options.add(new Option("20岁以下",decimalFormat.format(lt20Count * 1.0d / totalCount)));

        return new Stat("用户年龄占比",options);

    }

    // 解析返回值，得到性别stat
    public Stat getGenderStat(SearchResult searchResult){


        MetricAggregation aggregations = searchResult.getAggregations();

        TermsAggregation agecount = aggregations.getTermsAggregation("gendercount");

        List<TermsAggregation.Entry> buckets = agecount.getBuckets();

        Long maleCount = 0l;
        Long femaleCount = 0l;


        for (TermsAggregation.Entry bucket : buckets) {

            if (bucket.getKey().equals("M")){

                maleCount += bucket.getCount();

            }else{

                femaleCount += bucket.getCount();

            }

        }

        Long totalCount = maleCount + femaleCount ;

        DecimalFormat decimalFormat = new DecimalFormat("###.##%");

        ArrayList<Option> options = new ArrayList<>();

        options.add(new Option("男",decimalFormat.format(maleCount * 1.0d / totalCount)));
        options.add(new Option("女",decimalFormat.format(femaleCount * 1.0d / totalCount)));

        return new Stat("用户性别占比",options);

    }


    @Override
    public JSONObject getESData(String date111, Integer startpage, Integer size, String keyword) throws IOException {

        SearchResult searchResult = queryES(date111, startpage, size, keyword);

        List<SaleDetail> detail = getDetail(searchResult);

        Stat genderStat = getGenderStat(searchResult);
        Stat ageStat = getAgeStat(searchResult);

        ArrayList<Stat> stats = new ArrayList<>();

        stats.add(genderStat);
        stats.add(ageStat);

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("total",searchResult.getTotal());
        jsonObject.put("stats",stats);
        jsonObject.put("detail",detail);


        return jsonObject;
    }


}
