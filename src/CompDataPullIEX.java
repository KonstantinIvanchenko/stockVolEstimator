import org.json.JSONString;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;


public class CompDataPullIEX implements DataPull {

    public String targetURL;
    public String urlParameters;
    public HttpURLConnection connection;
    StringBuilder rawGetData;

    public JSONArray rawJSONdata;
    public JSONParser rawJSONParser;

    CompDataPullIEX(){
        rawGetData = new StringBuilder();
        rawJSONParser = new JSONParser();
    }


    public void buildGetReqStock(String infoType, String compIndex, String timeframe) {

        if (infoType == "splits" && (timeframe == "5y" || timeframe == "2y" || timeframe == "1y" || timeframe == "ytd"
                || timeframe == "6m" || timeframe == "3m" || timeframe == "1m")) {
            targetURL = new String("https://api.iextrading.com/1.0/stock/" + compIndex.toLowerCase() + "/" + infoType + "/" + timeframe);
        } else {
            targetURL = new String("https://api.iextrading.com/1.0/stock/" + compIndex.toLowerCase() + "/price");
        }
    }

    /*public String extractDataLines(JSONAr tempObject) {

        for (Object key : tempObject.keySet())
        {
            String keyString = (String) key;
            Object keyvalue = tempObject.get(keyString);

            System.out.println("key: "+keyString+" value:"+keyvalue);

            if (keyvalue instanceof  JSONObject)
                extractDataLines((JSONObject) keyvalue);
        }

        return "";

    }*/

    public void getDataList(String Type, String descriptor){
        JSONArray tempObject = (JSONArray) rawJSONdata.get(Integer.parseInt(descriptor));//TODO: put proper descriptor

        //extractDataLines(tempObject);
    }

    public JSONArray getRawData() {
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
            rawJSONdata = (JSONArray) obj;

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


    public void pullData() {
        String delimiter = ",";
    }
}
