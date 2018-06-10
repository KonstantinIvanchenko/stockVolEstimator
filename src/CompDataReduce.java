import javafx.util.converter.DateStringConverter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.concurrent.*;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

import java.lang.reflect.Field;


public class CompDataReduce {

    //Stock data base handling with compStockData object
    private CompDataPullAVj compStockData;
    //Contains symbolic data for companies
    public CompSymbReader compSymbData;
    //defines type of request for data pulling
    public String sTimeSeries;
    public String sTimePeriod;

    //Aggregated data list for all companies. Being updated every call.
    public ArrayList<List<CompDataPullAVj.stampedPrices>> arrCompStockData;
    //Stock data for current company within request methods.
    private List<CompDataPullAVj.stampedPrices> listCompStockDataSync;

    CompDataReduce(String timeSeries, String timePeriod) {
        compSymbData = new CompSymbReader();
        compStockData = new CompDataPullAVj();

        sTimeSeries = new String(timeSeries);
        sTimePeriod = new String(timePeriod);

        compSymbData.loadCompSymbols();
        arrCompStockData = new ArrayList<List<CompDataPullAVj.stampedPrices>>(compSymbData.compSymbolLength);
        listCompStockDataSync = Collections.synchronizedList( new ArrayList<>() );
    }

    //Setters/Getters
    //Hint:Adaptation of time series type in cyclic updated of company data
    public void setsTimeSeries(String sTimeSeries) {
        this.sTimeSeries = sTimeSeries;
    }
    //Hint:Adaptation of time series period in cyclic updated of company data
    public void setsTimePeriod(String sTimePeriod) {
        this.sTimePeriod = sTimePeriod;
    }

    public String getsTimePeriod() {
        return sTimePeriod;
    }

    public String getsTimeSeries() {
        return sTimeSeries;
    }

    public CompSymbReader getCompSymbData() {
        return compSymbData;
    }


    //Constants
    private static class Constants {
        public static final String INTRA_DAY = "TIME_SERIES_INTRADAY";
        public static final String DAILY = "TIME_SERIES_DAILY";
        public static final String WEEKLY = "TIME_SERIES_WEEKLY";
        public static final String MONTHLY = "TIME_SERIES_MONTHLY";

        public static final String ONEMIN = "1min";
        public static final String FIVEMIN = "5min";
        public static final String FIFTMIN = "15min";
        public static final String THIRMIN = "30min";
        public static final String SIXTMIN = "60min";

        public static final String FULL = "full";

    }

    private final String[] fieldNames = {"closePrice","volume"};

    /**
     * Gets outputnode name for json parser
     * @param requestType - request type
     * @param requestPeriod - request period for INTRADAY request type. Otherwise not used
     * @return String with json node name
     */
    private String getOutputNode(String requestType, String requestPeriod){

        if (requestType.equals(Constants.INTRA_DAY)){
            switch (requestPeriod) {
                case Constants.ONEMIN:
                    return "Time Series (1min)";
                case Constants.FIVEMIN:
                    return "Time Series (5min)";
                case Constants.FIFTMIN:
                    return "Time Series (15min)";
                case Constants.THIRMIN:
                    return "Time Series (30min)";
                case Constants.SIXTMIN:
                    return "Time Series (60min)";
                default:
                    return "";
            }
        }
        else {
            switch (requestType) {
                case Constants.DAILY:
                    return "Time Series (Daily)";
                case Constants.WEEKLY:
                    return "Weekly Time Series";
                case Constants.MONTHLY:
                    return "Monthly Time Series";
                default:
                    return "";
            }
        }
    }


    /**
     * Company time gap indicator for timeframe where data is missing.
     */
    private static class CompDataGap {
        public int secGap;

        public CompDataGap(int secNewGap){
            this.secGap = secNewGap;
        }

        public void setSecGap(int secGap) {
            this.secGap = secGap;
        }

        public int getSecGap() {
            return secGap;
        }
    }

    private HashMap<String, CompDataGap> hmUpTypeCompany = new HashMap<>();

    /**
     * Add new kv pair of time gap.
     * @param compKey - 4-letter unique company index
     * @param compGapValue - time gap where data is missing.
     */
    private void hmAddUpTypeCompany(String compKey, CompDataGap compGapValue){
        hmUpTypeCompany.put(compKey, compGapValue);
    }

    /**
     * Clears time gap info for all companies.
     */
    private void hmClearUpTypeCompany(){
        hmUpTypeCompany.clear();
    }

    /**
     * Updates All data with parameters specified in sTimePeriod and sTimeSeries. IMPORTANT: method clears the
     * arrCompStockData as it is supposed to be called as a single burst data request with unique request type
     * for all data.
     * Using threads for data pulling per company.
     * @return true on successful update of the arrCompStockData
     */
    public boolean CompDataCollectAll() {

        if(sTimePeriod.isEmpty() || sTimeSeries.isEmpty())
            return false;

        //IMPORTANT - clears the data list
        arrCompStockData.clear();

        int numThread = 0;

        //Get output node for json parser to parse raw data.
        String outputNode = getOutputNode(Constants.INTRA_DAY,Constants.ONEMIN);

        //even though data list is synchonized, mutex is used in addition to block attempt of parallel insertions into it.
        Semaphore mutex = new Semaphore(1, true);
        //permits limited number of simultaneous threads
        Semaphore block = new Semaphore(10, true);

        //int waitForTermination = compSymbData.compSymbolLength;
        ExecutorService es = Executors.newCachedThreadPool();

        //Permits further execution after latch == 0
        CountDownLatch latch = new CountDownLatch(compSymbData.compSymbolLength);

        long executionTime = System.currentTimeMillis();

        for (Iterator<String> i = compSymbData.compSymbols.iterator(); i.hasNext(); ) {
            String iCompSymb = i.next();
//Each Thread pulls separate group of acc data for each company name
            new Thread("" + numThread) {
                public void run() {
                    try {
                        block.acquire();
                        String newUrl = new String(compStockData.buildGetReqStockURL(sTimeSeries, iCompSymb, sTimePeriod));
                        System.out.println(newUrl);

                        List<CompDataPullAVj.stampedPrices> tempList = compStockData.convertDataFromRaw(newUrl, iCompSymb, outputNode);

                        block.release();

                        mutex.acquire();

                        listCompStockDataSync = tempList;

                        arrCompStockData.add(listCompStockDataSync);

                        mutex.release();

                        latch.countDown();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }.start();

            numThread++;
        }

        try {
            latch.await();

            executionTime = System.currentTimeMillis() - executionTime;
            System.out.println("Elapsed Time: " + executionTime + " ms");
        }catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        if (arrCompStockData.isEmpty())
            return false;
        else {

            //TODO: printout data

            return true;
        }
    }

    /**
     * Updates company data for a specific company list and adjusted timeframe.
     * Using threads for data pulling per company.
     * @param itSymbols - list of company symbols to collect data for.
     * @param series - type of data series to be requested.
     * @param period - time period to collect data for.
     * @param skipDataBegin - this ammount of last data units between Begin and End will not be added into list.
     * @param skipDataEnd
     * @return
     */
    public boolean CompDataCollect(ArrayList<String> itSymbols, String series, String period, int skipDataBegin, int skipDataEnd) {
        //arrCompStockData.clear();

        int numThread = 0;

        //Get output node for json parser to parse raw data.
        String outputNode = getOutputNode(series, period);

        //outputNode string shall not be empty
        if (outputNode.equals(""))
            return false;

        //even though data list is synchonized, mutex is used in addition to block attempt of parallel insertions into it.
        Semaphore mutex = new Semaphore(1, true);
        //permits limited number of simultaneous threads
        Semaphore block = new Semaphore(10, true);

        //int waitForTermination = compSymbData.compSymbolLength;
        ExecutorService es = Executors.newCachedThreadPool();

        //Permits further execution after latch == 0
        CountDownLatch latch = new CountDownLatch(compSymbData.compSymbolLength);

        long executionTime = System.currentTimeMillis();

        for (Iterator<String> i = itSymbols.iterator(); i.hasNext(); ) {
            String iCompSymb = i.next();
/*
            String newUrl = new String(compStockData.buildGetReqStockURL(sTimeSeries.toString(), iCompSymb, sTimePeriod.toString()));
            System.out.println(newUrl);

            try {
                List<CompDataPullAVj.stampedPrices> tempList = compStockData.convertDataFromRaw(newUrl);

                System.out.println(mutex.availablePermits());

                System.out.println("Acquire");

                mutex.acquire();
                listCompStockDataSync = tempList;

                arrCompStockData.add(listCompStockDataSync);

                System.out.println(mutex.availablePermits());

                System.out.println("Release");

                mutex.release();

                System.out.println(mutex.availablePermits());

                latch.countDown();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/



//Each Thread pulls separate group of acc data for each company name
            new Thread("" + numThread) {
                public void run() {
                    try {
                        block.acquire();
                        String newUrl = new String(compStockData.buildGetReqStockURL(series, iCompSymb, period));
                        System.out.println(newUrl);

                        List<CompDataPullAVj.stampedPrices> tempList = compStockData.convertDataFromRaw(newUrl, iCompSymb, outputNode);

                        block.release();

                        mutex.acquire();

                        //throw away data blocks blocks between begin and end.
                        tempList.subList(skipDataBegin, skipDataEnd).clear();

                        listCompStockDataSync = tempList;

                        arrCompStockData.add(listCompStockDataSync);

                        mutex.release();

                        latch.countDown();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }.start();

            numThread++;
        }


/*
                es.submit(()-> new Runnable() {
                    public void run()
                    {
                        String newUrl = new String(compStockData.buildGetReqStockURL(sTimeSeries.toString(), iCompSymb, sTimePeriod.toString()));
                        System.out.println(newUrl);

                        try {

                            List<CompDataPullAVj.stampedPrices> tempList = compStockData.convertDataFromRaw(newUrl);
                            mutex.acquire();

                            arrCompStockData.add(tempList);

                            mutex.release();

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                });
            }


            es.shutdown();
            try {
                boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);

            }catch (InterruptedException e)
            {
                e.printStackTrace();
            }
*/

        try {
            latch.await();

            executionTime = System.currentTimeMillis() - executionTime;
            System.out.println("Elapsed Time: " + executionTime + " ms");
        }catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        if (arrCompStockData.isEmpty())
            return false;
        else {

               //TODO: printout data

             return true;
        }
    }

    /**
     * Clears company stock data. Required for data request of few types.
     */
    public void clearCurrentStockData(){
        arrCompStockData.clear();
    }

    public boolean addCompDataAll () {

        //Comp List for a burst pull/write
        ArrayList<String> compListBurst = new ArrayList<>();
        //Comp List for an intra day pull/write
        ArrayList<String> compListIntraDay = new ArrayList<>();
        //Comp List for an intra week pull/write
        ArrayList<String> compListIntraWeek = new ArrayList<>();
        //Comp List for an intra month pull/write
        ArrayList<String> compListIntraMonth = new ArrayList<>();

        //Current date/time
        //Use this item for date comparison between current and readout
        Date dateNow = new Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("US/Eastern"));

        //Readout each company datafile, check for gaps in data and fill them
        for (Iterator<String> i = compSymbData.compSymbols.iterator(); i.hasNext(); ) {
            String iCompSymb = i.next();

            String fileName = "../volestimData/" + iCompSymb + "Data.csv";

            File iDataFile = new File(fileName);

            //data gap in seconds
            int secTimeGap = 0;

            if (iDataFile.exists() && !iDataFile.isDirectory()) {

                //Check latest item in each file
                try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
                    String line = br.readLine();

                    if (line == null || line.equals("")) {
                        //add all data to reduced data list for burst-write
                        secTimeGap = 0;

                        hmAddUpTypeCompany(iCompSymb, new CompDataGap(secTimeGap));
                        compListBurst.add(iCompSymb);

                    } else {
                        String[] splitLine = line.split("\\,");

                        //TODO: save as time in milliseconds - rewrite this part (NOT DATE format storage)
/*
                        //calculate the time gap
                        DateStringConverter readoutDate = new DateStringConverter();

                        Date timeEntryAsDate = readoutDate.fromString(splitLine[0]);
                        //timeEntryDate = readoutDate.fromString(splitLine[0]);

                        if (dateNow.compareTo(timeEntryAsDate) > 0) {
                            //get ms difference. Convert to int
                            secTimeGap = (int) ((dateNow.getTime() - timeEntryAsDate.getTime()) / 1000);//sec
                        }
*/
                        //readout time changed to pure int value in sec
                        int readoutTime = Integer.parseInt(splitLine[0]);

                        if (readoutTime > 0) {
                            //get ms difference. Convert to int
                            secTimeGap = (int) ((dateNow.getTime()/1000 - readoutTime));//sec
                        }

                        //Key-values with exact time gaps. To be extracted later
                        hmAddUpTypeCompany(iCompSymb, new CompDataGap(secTimeGap));

                        //Map the symbol on appropriate lists
                        if (secTimeGap <= 86400) {//shorter than day

                            compListIntraDay.add(iCompSymb);

                        } else if (secTimeGap <= 604800) {//shorter than week

                            compListIntraWeek.add(iCompSymb);

                        } else if (secTimeGap <= 2419200) {//shorter than month

                            compListIntraMonth.add(iCompSymb);

                        }

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }


            }else {
                //TODO: Create new Data file
                try{
                    //Create new data file @String fileName = "../volestimData/" + iCompSymb + "Data.csv";
                    iDataFile.createNewFile();

                    //Write Content - Empty
                    FileWriter writer = new FileWriter(iDataFile);
                    writer.write("\n");
                    writer.close();

                }catch (IOException e){
                    e.printStackTrace();
                }

                //add item in a list of items for a burst write
                //Key-values with exact time gaps. To be extracted later
                hmAddUpTypeCompany(iCompSymb, new CompDataGap(0));
                compListBurst.add(iCompSymb);
            }

        }

        //if item is up to date (not older than 1 day) put it in a list of items for a burst pull request (INTRADAY 1min)

        //if item is outdated, fill the gap - check missing time span:
        //If older than 1 month collect weekly data
        //For the last month collect all weekly data
        //For older time, get monthly data

        if (!compListBurst.isEmpty()) {
            clearCurrentStockData();
            CompDataCollect(compListBurst, Constants.INTRA_DAY, Constants.ONEMIN, 0, 0);
            //The data block for one last day is to be skipped
            CompDataCollect(compListBurst, Constants.DAILY, Constants.FULL, 0, 1);

            //TODO: Implement data extraction from arrCompStockData
        }
        if (!compListIntraDay.isEmpty()){


            //Constants.INTRA_DAY / ONE_MIN default
            CompDataCollectAll();

            //TODO: Add data filter in accordance with DataGap
            //TODO: Data filter is to be implemented inside the compListIntraDay.
        }
        if (!compListIntraWeek.isEmpty()){

            //The best fit:
            //TODO: 1. Collect INTRA_DAY 2.Collect DAILY for last week
        }
        if (!compListIntraWeek.isEmpty()){

            //The best fit:
            //TODO: 1. Collect INTRA_DAY 2.Collect DAILY for last month
        }


        //make a burst pull and write

        return true;
    }


    //TODO
    private boolean WriteDataReduceBy(String[] dataFields){
        //Convert date into time in ms (Same format as in hashmap)
        //Uses hashmap for time filtering
        //Uses DataFields as bitmask for required datafields

        Field[] dataFieldToReduce = new Field[dataFields.length];
        Object[] values = new Object[dataFields.length];

        try {

            //TODO: check if data fields of stampedPrices contains dataFields

            //Initialize data fields
            for (int i = 0; i < dataFields.length; i++){
                dataFieldToReduce[i] = arrCompStockData.get(0).get(0).whichField(dataFields[i]);
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        for (List<CompDataPullAVj.stampedPrices> listStampedPrices : arrCompStockData){
            for(CompDataPullAVj.stampedPrices stampedPrices : listStampedPrices){
                String currentCompany = stampedPrices.getcName();
                int currentTimeStampInSec = (int)stampedPrices.getDate().getTime()/1000;

                try {
                    for (int i = 0; i < dataFields.length; i++) {
                        values[i] = dataFieldToReduce[i].get(stampedPrices);
                    }
                }catch (IllegalAccessException e){
                    e.printStackTrace();
                }

                

            }
        }


        return true;
    }

}