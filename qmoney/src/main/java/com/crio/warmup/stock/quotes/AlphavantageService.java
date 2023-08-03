
package com.crio.warmup.stock.quotes;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {



        RestTemplate restTemplate=new RestTemplate();
        AlphavantageDailyResponse alphadailyres;
      
        public AlphavantageService(RestTemplate restTemplate) {
            this.restTemplate=restTemplate;
        }
      
        String getAPI(){
          return "0K4LCXEQZW4RSEE6";
        }
      
        String createUrl(String symbol){
      
          return "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="+symbol+"&outputsize=full&apikey="+getAPI();
        }
      
        @Override
        public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws StockQuoteServiceException
           {
      
                  
            String responseString = restTemplate.getForObject(createUrl(symbol), String.class);

            AlphavantageDailyResponse alphavantageDailyResponse;
            try {
              ObjectMapper objectMapper=new ObjectMapper();
              objectMapper.registerModule(new JavaTimeModule());
        
              alphavantageDailyResponse =
                  objectMapper.readValue(responseString, AlphavantageDailyResponse.class);
              if (alphavantageDailyResponse.getCandles() == null || responseString == null)
                throw new StockQuoteServiceException("Invalid Response Found");
            } catch (JsonProcessingException e) {
              throw new StockQuoteServiceException(e.getMessage());
            }
            List<Candle> alphavantageCandles = new ArrayList<>();
            Map<LocalDate, AlphavantageCandle> mapOFDateAndAlphavantageCandle =
                alphavantageDailyResponse.getCandles();
            for (LocalDate localDate : mapOFDateAndAlphavantageCandle.keySet()) {
              if (localDate.isAfter(from.minusDays(1)) && localDate.isBefore(to.plusDays(1))) {
                AlphavantageCandle alphavantageCandle =
                    alphavantageDailyResponse.getCandles().get(localDate);
                alphavantageCandle.setDate(localDate);
                alphavantageCandles.add(alphavantageCandle);
              }
              // AlphavantageCandle alphacandle=alphadailyres.getCandles();
            }
            return alphavantageCandles.stream().sorted(Comparator.comparing(Candle::getDate))
                .collect(Collectors.toList());
        
            //return candlelist.stream().sorted(Comparator.comparing(Candle::getDate)).collect(Collectors.toList());
      
        }
        


}


