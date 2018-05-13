public class volestim {
    public static void main(String[] args){
        CompSymbReader compSymb = new CompSymbReader();
        compSymb.getCompSymbols();

        //CompDataPullIEX compPullIEX = new CompDataPullIEX();

        //CompDataPullAV compDataPullAV = new CompDataPullAV();

        /*
        for (String object: compSymb.compSymbols){
            System.out.print(object+"\n");

            //Splits or Price
            compPullIEX.buildGetReqStock("splits", object, "5y");
            compPullIEX.getRawData();
            String Ratio = compPullIEX.extractDataLines("ratio");
        }
        */
/*
        compDataPullAV.buildGetReqStock("TIME_SERIES_INTRADAY",
                "aapl",
                "1min");

        compDataPullAV.getRawData();
        List<String> closeData1min = compDataPullAV.getDataList("Time Series (1min)", "4. close");


        for(int i = 0; i < closeData1min.size(); i++)
        {
            System.out.println("Close price index i" + Integer.toString(i)+ " "+closeData1min.get(i));

        }
*/
/*
        CompDataPullAVj compDataPullAVj = new CompDataPullAVj();

        compDataPullAVj.buildGetReqStockURL("TIME_SERIES_INTRADAY",
                "aapl",
                "1min");

        compDataPullAVj.convertDataFromRaw();
*/

        CompDataReduce compDataInst = new CompDataReduce("TIME_SERIES_INTRADAY", "1min");
        compDataInst.CompDataCollect();


        System.out.println("Volestim terminated.");
    }
}
