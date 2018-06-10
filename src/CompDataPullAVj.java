import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import java.lang.reflect.Field;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;


public class CompDataPullAVj implements DataPull {

    public String targetURL;

    /**
     * A nested class implementing required stock prices format
     */
    public class shortPrice{
        public shortPrice(){}

        public shortPrice(String[] items){
            if (items.length >= 5){
                this.openPrice = items[0];
                this.highPrice = items[1];
                this.lowPrice = items[2];
                this.closePrice = items[3];
                this.volume = items[4];
            }

            //Initialize HashMap with FieldsMap for easy addressing to fields of interest later at reduce phase
            Field[] fields = shortPrice.class.getDeclaredFields();

            for(Field f : fields){
                fieldsMap.put( f.toString(), f);
            }
        }
        public String openPrice;
        public String highPrice;
        public String lowPrice;
        public String closePrice;
        public String volume;

        public shortPrice getShortPrice(){
            return this;
        }

        public void setShortPrice(shortPrice sp){
            this.openPrice = sp.openPrice;
            this.closePrice = sp.closePrice;
            this.highPrice = sp.highPrice;
            this.lowPrice = sp.lowPrice;
            this.volume = sp.volume;
        }

        //HashMap with field names for easy access at reduce phase
        private final HashMap<String, Field> fieldsMap = new HashMap<String, Field>();

        //Get field identifier by name
        public Field whichField (String fieldName){
            return fieldsMap.get(fieldName);
        }

    }

    /**
     * A nested class implementing required stock price format with time stamp insertion
     */
    public class stampedPrices extends shortPrice{
        public Date timeStamp;
        public String cName;

        public stampedPrices(){super();}

        public stampedPrices(Date dateItem, shortPrice pricesItem){
            this.timeStamp = dateItem;
            super.openPrice = pricesItem.openPrice;
            super.highPrice = pricesItem.highPrice;
            super.lowPrice = pricesItem.lowPrice;
            super.closePrice = pricesItem.closePrice;
            super.volume = pricesItem.volume;
        }

        public stampedPrices(Date dateItem, String cNameItem, String[] items){
            super(items);
            this.timeStamp = dateItem;
            this.cName = cNameItem;
        }

        public String getcName() {return  this.cName;}

        public Date getDate(){
            return this.timeStamp;
        }

        public void setDate(Date d){
            timeStamp = d;
        }

        public shortPrice getPrice(){
            return super.getShortPrice();
        }

        public void setPrice(shortPrice sp){
            super.setShortPrice(sp);
        }

        public void printStampedPrices(){
            System.out.println("Date "+this.timeStamp+
                    "CompName"+this.cName+
                    "Open "+super.openPrice+
                    "High "+super.highPrice+
                    "Low" +super.lowPrice+
                    "Close "+super.closePrice+
                    "Volume "+super.volume
            );
        }

    }

    CompDataPullAVj(){
    }

    /**
     * URL builder for request types INTRADAY and DAILY
     * @param infoType - request type identifier
     * @param compIndex - company 4-letter index
     * @param timeframe - timeframe identifier
     * @return targetURL - in the format of alphavantage.co
     */
    public String buildGetReqStockURL(String infoType, String compIndex, String timeframe){

        String signatureAccess = getAccessSignature("AVaccessSign.txt");

        if(infoType.equals("TIME_SERIES_INTRADAY") && (timeframe.equals("1min") ||
                timeframe.equals("5min") || timeframe.equals("15min") || timeframe.equals("30min")
                || timeframe.equals("60min"))){
            this.targetURL = new String ("https://" +
                    "www.alphavantage.co/query?" +
                    "function="+infoType+
                    "&symbol="+compIndex+
                    "&interval="+timeframe+
                    "&apikey="+signatureAccess);

            return targetURL;
        }

        if(infoType.equals("TIME_SERIES_DAILY") && (timeframe.equals("full") ||
                timeframe.equals("compact"))){
            this.targetURL = new String ("https://" +
                    "www.alphavantage.co/query?" +
                    "function="+infoType+
                    "&symbol="+compIndex+
                    "&outputsize="+timeframe+
                    "&apikey="+signatureAccess);

            return targetURL;
        }

        return null;
    }

    /**
     * URL builder for request types WEEKLY and MONTHLY
     * @param infoType - request type identifier
     * @param compIndex - company 4-letter index
     * @return targetURL - in the format of alphavantage.co
     */
    public String buildGetReqStockURL(String infoType, String compIndex){

        String signatureAccess = getAccessSignature("AVaccessSign.txt");

        if(infoType.equals("TIME_SERIES_WEEKLY") || infoType.equals("TIME_SERIES_MONTHLY")){
            this.targetURL = new String ("https://" +
                    "www.alphavantage.co/query?" +
                    "function="+infoType+
                    "&symbol="+compIndex+
                    "&apikey="+signatureAccess);

            return targetURL;
        }

        return null;
    }


    /**
     * Loads access signature from file. Serves as user identifier. File shall contain no other data.
     * @param path - Path to file storing the signature.
     * @return returns signature as string
     */
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


    /**
     * The method pulls a json node tree. A prior raw data pull request (i.e. getRawData()) is not needed with this method.
     * @return List of price sets.
     */

    /**
     *
     * @param url - targetURL for data request
     * @param compSymb
     * @return
     * @throws IOException
     */

    /**
     * Executes data request in and process it with a specific json node identifier (Specific to alphavantage.co output format).
     * @param url - url for data request. (Uses alphavantage.co format)
     * @param compSymb - company 4-letter index
     * @param outputNodeName - output node name for json parser. Example "Time Series (1min)"
     * @return List of data converted into stampedPrices
     * @throws IOException
     */
    public List<stampedPrices> convertDataFromRaw(String url, String compSymb, String outputNodeName) throws IOException{
        //String rawDataFromAV = getRawData();
        try {
            //URL url = new URL(this.targetURL);
            URL localURL = new URL(url);

            ObjectMapper jmapper = new ObjectMapper();

            //datePattern depends on the time series we request. For "min" of INTRA_DAY the pattern shall have extended form.
            StringBuilder datePattern = new StringBuilder("yyyy-MM-dd");
//TODO: check the date format for non INTRADAY requests
            if(outputNodeName.contains("min"))
                datePattern.append(" HH:mm:ss");

            DateFormat dformat = new SimpleDateFormat(datePattern.toString());
            jmapper.setDateFormat(dformat);
            jmapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            //List<stampedPrices> listPrices = jmapper.readValue(url, new TypeReference<stampedPrices>(){});

            JsonNode outputNode = new ObjectMapper().readTree(localURL);

            JsonNode outputTimeSeries = outputNode.path(outputNodeName);

            //List<stampedPrices> listPrices = jmapper.readValue(outputNode, new TypeReference<stampedPrices>(){});

            List<stampedPrices> listPrices = new ArrayList<>();

            //Iterator<JsonNode> it = outputTimeSeries.getElements();


            for (Iterator<String> itd = outputTimeSeries.getFieldNames(); itd.hasNext();)
            {
                String fieldd = itd.next();
                JsonNode dateIteration = outputTimeSeries.get(fieldd);
                //System.out.println(fieldd + " => "+dateIteration);

                //shortPrice prices = jmapper.readValue(outputTimeSeries.get(field), shortPrice.class);

                String[] tempString = new String[5];
                int index = 0;


                for (Iterator<String> itp = dateIteration.getFieldNames(); itp.hasNext();)
                {
                    String fieldp = itp.next();

                    JsonNode priceIteration = dateIteration.get(fieldp);
                    //System.out.println(fieldp + " => "+priceIteration);

                    //newPrices.prices[index] =
                    tempString[index] = priceIteration.toString();
                    index++;
                }

                stampedPrices tStampedPrice = new stampedPrices(dformat.parse(fieldd), compSymb, tempString);

                //shortPrice tempShortPrice = new shortPrice(tempString);
                //get date with predefined format
                //stampedPrices tempStampedPrice = new stampedPrices(dformat.parse(fieldd), tempShortPrice);

                listPrices.add(tStampedPrice);

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


}