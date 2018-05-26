import javafx.util.converter.DateStringConverter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.concurrent.*;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;


public class CompDataReduce {

    private CompDataPullAVj compStockData;
    public CompSymbReader compSymbData;
    public String sTimeSeries;
    public String sTimePeriod;

    //Aggregated data list:
    //Cleared every new pull iteration
    //Stored
    public ArrayList<List<CompDataPullAVj.stampedPrices>> arrCompStockData;
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

    /**
     * Updates All data with parameters specified in sTimePeriod and sTimeSeries
     * @return true on successful update of the arrCompStockData
     */
    public boolean CompDataCollectAll() {

        if(sTimePeriod.isEmpty() || sTimeSeries.isEmpty())
            return false;

        arrCompStockData.clear();

        int numThread = 0;
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

                        List<CompDataPullAVj.stampedPrices> tempList = compStockData.convertDataFromRaw(newUrl, iCompSymb);

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
     * Updates company data for a specific company list and adjusted timeframe
     * @param itSymbols list of company symbols to collect data for
     * @param series type of data series to be requested
     * @param period time period to collect data for
     * @return
     */
    public boolean CompDataCollect(ArrayList<String> itSymbols, String series, String period) {
        arrCompStockData.clear();

        int numThread = 0;
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

                        List<CompDataPullAVj.stampedPrices> tempList = compStockData.convertDataFromRaw(newUrl, iCompSymb);

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

    public boolean addCompDataAll () {

        //Comp List for a burst write
        ArrayList<String> compListBurst = new ArrayList<>();

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

            long secTimeGap = 0;

            if (iDataFile.exists() && !iDataFile.isDirectory()) {

                //Check latest item in each file
                try (BufferedReader br = new BufferedReader(new FileReader(fileName))){
                    String line = br.readLine();

                    if (line.isEmpty()){
                        //add all data to reduced data list for burst-write
                        secTimeGap = 0;
                    }
                    else {
                        String[] splitLine = line.split("\\,");

                        //calculate the time gap
                        DateStringConverter readoutDate = new DateStringConverter();

                        Date timeEntryAsDate = readoutDate.fromString(splitLine[0]);
                        //timeEntryDate = readoutDate.fromString(splitLine[0]);

                        if (dateNow.compareTo(timeEntryAsDate) > 0){
                            //get ms difference. Convert to int
                            secTimeGap = (dateNow.getTime() - timeEntryAsDate.getTime())/1000;//sec
                        }

                    }

                }catch (IOException e)
                {
                    e.printStackTrace();
                }

                //if item is up to date (not older than 1 day) put it in a list of items for a burst pull request (INTRADAY 1min)

                //if item is outdated, fill the gap - check missing time span:
                //If older than 1 month collect weekly data
                //For the last month collect all weekly data
                //For older time, get monthly data

                if (secTimeGap == 0){
                    setsTimePeriod("TIME_SERIES_INTRADAY");

                }else if (secTimeGap <= 86400) {//shorter than day

                }else if (secTimeGap <= 604800){//shorter than week

                }else if (secTimeGap <= 2419200){//shorter than month

                }


            } else {
                //Create new Data file
                //add item in a list of items for a burst write
            }


        }

        //make a burst pull and write

        return true;
    }

}