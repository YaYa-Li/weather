package com.example.search.service;


import com.example.search.config.EndpointConfig;
import com.example.search.pojo.City;
import com.example.search.pojo.Consolidated_weather;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class WeatherServiceImpl implements WeatherService{

    @LoadBalanced
    private final RestTemplate restTemplate;

    private final RestTemplate externalRestTemplate;


    @Autowired
    public WeatherServiceImpl(RestTemplate getRestTemplate, RestTemplate restTemplate_external) {
        this.restTemplate = getRestTemplate;
        this.externalRestTemplate = restTemplate_external;
    }


    @Override
    @Retryable(include = IllegalAccessError.class)
    public List<Integer> findCityIdByName(String city) {
        City[] cities = restTemplate.getForObject(EndpointConfig.queryWeatherByCity + city, City[].class);
        List<Integer> ans = new ArrayList<>();
        for(City c: cities) {
            if(c != null && c.getWoeid() != null) {
                ans.add(c.getWoeid());
            }
        }
        return ans;
    }

    @Override
    public Map<String, Map> findCityNameById(int id) {
        Map<String, Map> ans = restTemplate.getForObject(EndpointConfig.queryWeatherById + id, HashMap.class);
        return ans;
    }

    @Override
    @Retryable(include = IllegalAccessError.class,value = RuntimeException.class, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    public List<City> getWeatherByName(String cities) {

        System.out.println("Search service city name=="+cities);

        List listOfCity = new ArrayList();
        ExecutorService executor = Executors.newCachedThreadPool();
        String[] citiesArr = cities.split(",");

        for(String city: citiesArr){
            System.out.println("city ====="+city);
            /*
             *  1.visit detail service to get city id
             *  2.visit 3rd api to get city weather with city id
             *  3. Add weather result to a list
             */
            CompletableFuture.supplyAsync(()->restTemplate.getForObject(EndpointConfig.queryIdByCityName + city, Integer.class), executor)
                    .thenApplyAsync(cityId->{
                        System.out.println("Search service city id=="+cityId);
                        return externalRestTemplate.getForObject(EndpointConfig.queryWeatherById + cityId, City.class);
                    })
                    .thenAcceptAsync(city_weather->{
                        System.out.println("Search service weather obj=="+city_weather);
                         listOfCity.add(city_weather);
                    });
        }

//        System.out.println("Search service city name=="+cities);
//        int cityId = restTemplate.getForObject(EndpointConfig.queryIdByCityName + cities, Integer.class);
//
//        System.out.println("Search service city id=="+cityId);
//        System.out.println("Search service url=="+EndpointConfig.queryWeatherById + cityId);
//        City weather_obj = externalRestTemplate.getForObject(EndpointConfig.queryWeatherById + cityId, City.class);
//
//        System.out.println(weather_obj.getConsolidated_weather().toString());
//        Consolidated_weather weatherDetail = weather_obj.getConsolidated_weather();
//        System.out.println(weatherDetail.getThe_temp()+"-----@@###------"
//        + weatherDetail.getWeather_state_name());
//        listOfCity.add(weather_obj);

        return listOfCity;
    }

    @Recover
    public String failedOperation(Exception e){
        return "The service is down:"+ e.getStackTrace();
    }




}


/**
 *  -> gateway -> eureka
 *       |
 *   weather-search -> hystrix(thread pool) -> 3rd party weather api
 *
 *
 *  circuit breaker(hystrix)
 * *  * *  * *  * *  * *  * *  * *  * *  * *  * *  * *  * *  * *  * *
 *   weather-search service should get city id from detail service
 *   and use multi-threading to query city's weather details
 *
 *   gateway
 *     |
 *  weather-service -> 3rd party api(id <-> weather)
 *    |
 *  detail-service -> 3rd party api (city <-> id)
 *
 *  failed situations:
 *      1. 3rd party api timeout -> retry + hystrix
 *      2. 3rd party api available time / rate limit
 *      3. security verification
 *  response
 *      1. no id -> error / empty
 *      2. large response -> pagination / file download (link / email)
 *  performance
 *      1. cache / db
 *
 *   gateway
 *     |
 *  weather-service -> cache(city - id - weather) (LFU)
 *    |
 *   DB (city - id - weather) <-> service <->  message queue  <-> scheduler <-> 3rd party api(city - id)
 *                                                                  |
 *                                                         update id - weather every 30 min
 *                                                         update city - id relation once per day
 *
 *  homework :
 *      deadline -> Wednesday midnight
 *      1. update detail service
 *          a. send request to 3rd party api -> get id by city
 *      2. update search service
 *          a. add ThreadPool
 *          b. send request to detail service -> get id by city
 *          c. use CompletableFuture send request to 3rd party api -> get weather by ids
 *          d. add retry feature
 */