package ejm.chapter6.stock;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Hashtable;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonObject;

import javax.ws.rs.core.MediaType;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;

/**
 * @author Ken Finnigan
 */
public class StockCommand extends HystrixCommand<String> {
    // For simulating failures
    private static AtomicInteger counter = new AtomicInteger(1);
    private static Hashtable<String, Double> previousQuotes = new Hashtable();  // a cached copy of relevant previous day quotes
    
    private final String stockCode;

    public StockCommand(String stockCode) {
        super(Setter
                      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("StockGroup"))
                      .andCommandPropertiesDefaults(
                              HystrixCommandProperties.Setter()
                                      .withCircuitBreakerRequestVolumeThreshold(10)
                                      .withCircuitBreakerSleepWindowInMilliseconds(10000)
                                      .withCircuitBreakerErrorThresholdPercentage(50)
                      )
                      .andThreadPoolPropertiesDefaults(
                              HystrixThreadPoolProperties.Setter()
			      .withCoreSize(4) // modified from original code
                      )
        );

        this.stockCode = stockCode;
    }

    @Override
    protected String run() throws Exception {
        // For simulating failures
        int requestNum = counter.get();
        System.out.println("Processing request number " + requestNum + " for code: " + stockCode);
        counter.incrementAndGet();

        if (requestNum % 10 == 0) {
            throw new RuntimeException("Simulated Failure!");
        }
	
        // The actual request
        HttpURLConnection connection = null;

        try {
            URL url = new URL("https://sandbox.tradier.com/v1/markets/quotes?symbols=" + stockCode);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", MediaType.APPLICATION_JSON);
            connection.setRequestProperty("Authorization", "Bearer vGSq5QgMpwaiFYjgE8HtFoVfeVus");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Request Failed: HTTP Error code: " + connection.getResponseCode());
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String output;
            StringBuilder response = new StringBuilder();

            while ((output = reader.readLine()) != null) {
                response.append(output);
            }

            System.err.println("Successfully executed stock price request for: " + this.stockCode);

	    /**
	     * In the real world we'd only do this once a day.
	     *
	     * @nmcl
	     */

	    Double previousQuote = previousQuotes.get(stockCode);
	    
	    if (previousQuote == null)
	    {
		JsonReader jsonReader = Json.createReader(new StringReader(response.toString()));
		JsonObject object = jsonReader.readObject();
		
		jsonReader.close();

		previousQuotes.put(stockCode, new Double(object.getJsonObject("quotes").getJsonObject("quote").getJsonNumber("prevclose").doubleValue()));
	    }

            return response.toString() + " { \"request_num\":" + "\"" + requestNum + "\" }";
        } finally {
            assert connection != null;
            connection.disconnect();
        }
    }

    @Override
    protected String getFallback() {
        return "Yesterdays price for " + this.stockCode + " " + this.previousQuotes.get(stockCode);
    }

    @Override
    protected String getCacheKey() {
        return this.stockCode;
    }
}
