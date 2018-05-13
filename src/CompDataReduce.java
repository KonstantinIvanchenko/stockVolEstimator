import java.io.IOException;
import java.util.*;

import java.util.concurrent.*;

public class CompDataReduce {
    private CompSymbReader compSymbData;
    private CompDataPullAVj compStockData;
    public StringBuilder sTimeSeries;
    public StringBuilder sTimePeriod;

    public ArrayList<List<CompDataPullAVj.stampedPrices>> arrCompStockData;
    private List<CompDataPullAVj.stampedPrices> listCompStockDataSync;

    CompDataReduce(String timeSeries, String timePeriod) {
        compSymbData = new CompSymbReader();
        compStockData = new CompDataPullAVj();

        sTimeSeries = new StringBuilder(timeSeries);
        sTimePeriod = new StringBuilder(timePeriod);

        compSymbData.getCompSymbols();
        arrCompStockData = new ArrayList<List<CompDataPullAVj.stampedPrices>>(compSymbData.compSymbolLength);
        listCompStockDataSync = Collections.synchronizedList( new ArrayList<>() );
    }

    public boolean CompDataCollect() {
        arrCompStockData.clear();

        int numThread = 0;
        Semaphore mutex = new Semaphore(1, true);
        Semaphore block = new Semaphore(4, true);

        //int waitForTermination = compSymbData.compSymbolLength;
        ExecutorService es = Executors.newCachedThreadPool();

        CountDownLatch latch = new CountDownLatch(compSymbData.compSymbolLength);


        for (Iterator<String> i = compSymbData.compSymbols.iterator(); i.hasNext(); ) {
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


            new Thread("" + numThread) {
                public void run() {
                    try {
                        block.acquire();
                        String newUrl = new String(compStockData.buildGetReqStockURL(sTimeSeries.toString(), iCompSymb, sTimePeriod.toString()));
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
}