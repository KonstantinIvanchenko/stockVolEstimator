import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.print.PrinterIOException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class CompDataPullAV implements DataPull {

    ArrayList<PriceSpreadCont> arrPrices;

    public String targetURL;
    public String urlParameters;
    public HttpURLConnection connection;
    StringBuilder rawGetData;

    public JSONObject rawJSONdata;
    public JSONParser rawJSONParser;

    CompDataPullAV(){
        arrPrices = new ArrayList<PriceSpreadCont>(100);

        rawGetData = new StringBuilder();
        rawJSONParser = new JSONParser();
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


    private List<String> extractDataLines(JSONObject tempObject, String requiredField) {

        List<String> requiredData = new ArrayList<String>();

        for (Object key : tempObject.keySet())
        {
            String keyString = (String) key;
            Object keyvalue = tempObject.get(keyString);

            //System.out.println("key: "+keyString+" value:"+keyvalue);

            if (keyString == requiredField) {
                requiredData.add(keyvalue.toString());
            }

            if (keyvalue instanceof  JSONObject)
                extractDataLines((JSONObject) keyvalue, requiredField);
        }

        return requiredData;

    }


    public List<String> getDataList(String timeSeriesType, String timeSeriesField){
        JSONObject tempObject = (JSONObject) rawJSONdata.get(timeSeriesType);

        return extractDataLines(tempObject, timeSeriesField);
    }

    public JSONObject getRawData() {
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
            Object obj = rawJSONParser.parse(rawGetData.toString());
            rawJSONdata = (JSONObject) obj;

            return rawJSONdata;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            if (connection != null)
                //Close connection regardless
                connection.disconnect();
        }

        return rawJSONdata;
    }

    public ArrayList<PriceSpreadCont> getSingleList(){

        return null;
    }
}