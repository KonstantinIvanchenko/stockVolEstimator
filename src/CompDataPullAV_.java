import javafx.util.converter.DateStringConverter;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.type.TypeReference;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;


public class CompDataPullAV_ implements DataPull {

    ArrayList<PriceSpreadCont> arrPrices;

    public String targetURL;
    public String urlParameters;
    public HttpURLConnection connection;
    StringBuilder rawGetData;

    //public JSONObject rawJSONdata;
    //public JSONParser rawJSONParser;

    public String rawJSONdata;

    public class shortPrice{
        public shortPrice(){super();}

        public shortPrice(String[] items){
            if (items.length >= 5){
                this.openPrice = items[0];
                this.highPrice = items[1];
                this.lowPrice = items[2];
                this.closePrice = items[3];
                this.volume = items[4];
            }
        }
        public String openPrice;
        public String highPrice;
        public String lowPrice;
        public String closePrice;
        public String volume;

    }

    public class stampedPrices{
        public Date timeStamp;
        public shortPrice prices;

        public stampedPrices(){super();}

        public stampedPrices(Date dateItem, shortPrice pricesItem){
            this.timeStamp = dateItem;
            this.prices = pricesItem;
        }

        public Date getDate(){
            return this.timeStamp;
        }

        public void setDate(Date d){
            timeStamp = d;
        }

        public shortPrice getPrice(){
            return this.prices;
        }

        public void setPrice(shortPrice sp){
            prices = sp;
        }

    }

    CompDataPullAV_(){
        arrPrices = new ArrayList<PriceSpreadCont>(100);

        rawGetData = new StringBuilder();
        //rawJSONParser = new JSONParser();
    }

    public void buildGetReqStock(String infoType, String compIndex, String timeframe){

        String signatureAccess = getAccessSignature("AVaccessSign.txt");

        if(infoType == "TIME_SERIES_INTRADAY" && (timeframe == "1min" ||
                timeframe == "5min" || timeframe == "15min" || timeframe == "30min"
                || timeframe == "60min")){
            targetURL = new String ("https://" +
                    "www.alphavantage.co/query?" +
                    "function="+infoType+
                    "&symbol="+compIndex+
                    "&interval="+timeframe+
                    "&apikey="+signatureAccess);
        }
    }

    private String getAccessSignature(String path)
    {
        try{
            List<String> lineSignature = Files.readAllLines(Paths.get(path));

            String signature = lineSignature.get(0);

            return signature;

        }catch (IOException e){
            e.printStackTrace();
        }

        return "";
    }


    //**********************************

    public List<stampedPrices> convertDataFromRaw(){
        //String rawDataFromAV = getRawData();
        try {
            URL url = new URL(targetURL);

            ObjectMapper jmapper = new ObjectMapper();
            DateFormat dformat = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
            jmapper.setDateFormat(dformat);
            jmapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            //List<stampedPrices> listPrices = jmapper.readValue(url, new TypeReference<stampedPrices>(){});

            JsonNode outputNode = new ObjectMapper().readTree(url);

            JsonNode outputTimeSeries = outputNode.path("Time Series (1min)");

            //List<stampedPrices> listPrices = jmapper.readValue(outputNode, new TypeReference<stampedPrices>(){});

            List<stampedPrices> listPrices = new ArrayList<>();

            //Iterator<JsonNode> it = outputTimeSeries.getElements();


            for (Iterator<String> itd = outputTimeSeries.getFieldNames(); itd.hasNext();)
            {
                String fieldd = itd.next();
                JsonNode dateIteration = outputTimeSeries.get(fieldd);
                System.out.println(fieldd + " => "+dateIteration);

                //shortPrice prices = jmapper.readValue(outputTimeSeries.get(field), shortPrice.class);

                String[] tempString = new String[5];
                int index = 0;


                for (Iterator<String> itp = dateIteration.getFieldNames(); itp.hasNext();)
                {
                    String fieldp = itp.next();

                    JsonNode priceIteration = dateIteration.get(fieldp);
                    System.out.println(fieldp + " => "+priceIteration);

                    //newPrices.prices[index] =
                    tempString[index] = priceIteration.toString();
                    index++;
                }

                shortPrice tempShortPrice = new shortPrice(tempString);
                //get date with predefined format
                stampedPrices tempStampedPrice = new stampedPrices(dformat.parse(fieldd), tempShortPrice);

                listPrices.add(tempStampedPrice);

            }

            return listPrices;
        }catch(java.text.ParseException e){
            e.printStackTrace();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        return null;

    }

    private String getRawData() {
        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            InputStream is = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader bufLineInput = new BufferedReader(isr);

            rawGetData = new StringBuilder();
            String line;

            while ((line = bufLineInput.readLine()) != null) {
                rawGetData.append(line);
                //rawGetData.append('\r');
            }

            bufLineInput.close();

            //Convert received string into JSON object and return
            //Object obj = rawJSONParser.parse(rawGetData.toString());
            //rawJSONdata = (JSONObject) obj;

            //return rawJSONdata;

            return rawGetData.toString();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null)
                //Close connection regardless
                connection.disconnect();
        }

        return rawJSONdata;
    }


}