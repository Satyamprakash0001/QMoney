
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {


  protected RestTemplate restTemplate= new RestTemplate();
  StockQuotesService stockQuotesService;


  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }

  public PortfolioManagerImpl(){

  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF




  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException, StockQuoteServiceException {
    //      String url= buildUri(symbol, from , to);
     
    //  TiingoCandle[] tiingoresult=restTemplate.getForObject(url, TiingoCandle[].class);
      
    //  if(tiingoresult!=null)
    //       return Arrays.stream(tiingoresult).collect(Collectors.toList());
    //  return new ArrayList<Candle>();

    return stockQuotesService.getStockQuote(symbol, from, to);
  }


  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
       String uriTemplate = "https:api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
            + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
      return uriTemplate;
  }



  public static String getToken() {
    return "bfab33eb91575e1b83114f8f2a19290f068cf05a";
  }

  // protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
  //   return "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?startDate=" + startDate 
  //   + "&endDate=" + endDate + "&token=" + getToken();
  // }


  private Double getOpeningPriceOnStartDate(List<Candle> candles) {
    return candles.get(0).getOpen();
  }

  private Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.get(candles.size() - 1).getClose();
  }


  private AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {
    double total_num_years = DAYS.between(trade.getPurchaseDate(), endDate) / 365.2422;
    double totalReturns = (sellPrice - buyPrice) / buyPrice;
    double annualized_returns = Math.pow((1.0 + totalReturns), (1.0 / total_num_years)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualized_returns, totalReturns);
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate) throws StockQuoteServiceException 
  {
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();

    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      List<Candle> candles;
      try {
        candles = getStockQuote(portfolioTrade.getSymbol(), portfolioTrade.getPurchaseDate(), endDate);
        AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(endDate, portfolioTrade,
        getOpeningPriceOnStartDate(candles), getClosingPriceOnEndDate(candles));
        annualizedReturns.add(annualizedReturn);
      } catch (JsonProcessingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
        
    }
    return annualizedReturns.stream().sorted(getComparator()).collect(Collectors.toList());
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads)
      throws StockQuoteServiceException {

        ExecutorService executer=Executors.newFixedThreadPool(numThreads);

      List<Future<AnnualizedReturn>> futureannualizedreturn=null;
      List<AnnualizedReturn> anualreturn=new ArrayList<>();
      List<ThreadHandler> handler=new ArrayList<>();

      for(PortfolioTrade portfoliotrade: portfolioTrades){
        handler.add(new ThreadHandler(endDate,portfoliotrade));
      }
          
        try {
          futureannualizedreturn= executer.invokeAll(handler);

          for(Future<AnnualizedReturn> fuanuiter : futureannualizedreturn){
            anualreturn.add(fuanuiter.get());
          }



        } catch (InterruptedException e) {
            
          throw new StockQuoteServiceException(e.getMessage());
         
        } catch (ExecutionException e) {
            
          throw new StockQuoteServiceException(e.getMessage());
         
        }
 
        executer.shutdown();

      return anualreturn.stream().sorted(Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed()).collect(Collectors.toList());
   
  
  }

  class ThreadHandler implements Callable<AnnualizedReturn>{

    PortfolioTrade porttradeobj;
    LocalDate date;
    
    public ThreadHandler(LocalDate date,PortfolioTrade porttradeobj) {
      this.date = date;
      this.porttradeobj = porttradeobj;
    }


    public ThreadHandler() {
      
    }


    @Override
    public AnnualizedReturn call() throws Exception {

      List<Candle> candleresult=stockQuotesService.getStockQuote(porttradeobj.getSymbol(), porttradeobj.getPurchaseDate(), date);

      return calculateAnnualizedReturns(date, porttradeobj, candleresult.get(0).getOpen(), candleresult.get(candleresult.size()-1).getClose());
      
    }

  }

  
}

