import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.ArrayList;

public class CompSymbReader {

    public ArrayList<String> compSymbols;
    public int compSymbolLength;

    public void loadCompSymbols()

    {
        compSymbols = new ArrayList<>();
        compSymbolLength = 0;

        String csvFileName = "companylist.csv";

        BufferedReader bufLineRd = null;
        String line = "";
        String delimiter = ",";


        try {
            bufLineRd = new BufferedReader(new FileReader(csvFileName));

            line = bufLineRd.readLine();//First line is not required

            while ((line = bufLineRd.readLine()) != null) {
                String[] strArray = line.split(delimiter);

                compSymbols.add(strArray[0]);
                compSymbolLength++;

                //System.out.print(strArray[0] + "\n");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufLineRd != null) {
                try {
                    bufLineRd.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}