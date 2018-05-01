import java.net.ConnectException;

public class volestim {
    public static void main(String[] args){
        CompSymbReader compSymb = new CompSymbReader();
        compSymb.getCompSymbols();

        CompDataPullIEX compPullIEX = new CompDataPullIEX();

        CompDataPullAV compDataPullAV = new CompDataPullAV();

        /*
        for (String object: compSymb.compSymbols){
            System.out.print(object+"\n");

            //Splits or Price
            compPullIEX.buildGetReqStock("splits", object, "5y");
            compPullIEX.getRawData();
            String Ratio = compPullIEX.extractDataLines("ratio");
        }
        */

        compDataPullAV.buildGetReqStock("TIME_SERIES_INTRADAY",
                "aapl",
                "1min");

        compDataPullAV.getRawData();
        compDataPullAV.getDataList("Time Series (1min)");

    }
}
