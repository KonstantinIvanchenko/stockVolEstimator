import java.io.IOException;
import java.util.*;

import java.util.concurrent.*;

import java.io.File;


public class CompDataReduce {

    private CompDataPullAVj compStockData;
    public CompSymbReader compSymbData;
    public StringBuilder sTimeSeries;
    public StringBuilder sTimePeriod;

    //Aggregated data list:
    //Cleared every new pull iteration
    //Stored
    public ArrayList<List<CompDataPullAVj.stampedPrices>> arrCompStockData;
    private List<CompDataPullAVj.stampedPrices> listCompStockDataSync;

    CompDataReduce(String timeSeries, String timePeriod) {
        compSymbData = new CompSymbReader();
        compStockData = new CompDataPullAVj();

        sTimeSeries = new StringBuilder(timeSeries);
        sTimePeriod = new StringBuilder(timePeriod);

        compSymbData.loadCompSymbols();
        arrCompStockData = new ArrayList<List<CompDataPullAVj.stampedPrices>>(compSymbData.compSymbolLength);
        listCompStockDataSync = Collections.synchronizedList( new ArrayList<>() );
    }

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

    boolean addCompDataAll () {

        //Comp List for a burst write
        ArrayList<String> compListBurst = new ArrayList<>();

        for (Iterator<String> i = compSymbData.compSymbols.iterator(); i.hasNext(); ) {
            String iCompSymb = i.next();

            File iDataFile = new File("../volestimData/" + iCompSymb + "Data.csv");

            if (iDataFile.exists() && !iDataFile.isDirectory()) {
                //Check latest item

                //if item is up to date (not older than 1 day) put it in a list of items for a burst pull request (INTRADAY 1min)

                //if item is outdated, fill the gap - check missing time span:
                //If older than 1 month collect weekly data
                //For the last month collect all weekly data
                //For older time, get monthly data


            } else {
                //Create new Data file
                //add item in a list of items for a burst write
            }


        }

        //make a burst pull and write

        return true;
    }

}