/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.samples.bookpanel;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Denis Lebedev
 */
public class FGDataSimulator
{
    public static final String COL_SYMBOL = "symbol";
    public static final String COL_SIDE = "side";
    public static final String COL_ID = "id";
    public static final String COL_QTY = "qty";
    public static final String COL_PRICE = "price";
    public static final String COL_TIF = "time-in-force";
    public static final String COL_LAST_PX = "last-px";
    public static final String COL_ACCOUNT = "account";
    public static final String COL_CLIENT = "client";
    public static final String COL_BROKER = "broker";
    public static final String COL_EXCHANGE = "exchange";

    public static final String COL_ASK_SOURCE = "asksource";
    public static final String COL_ASK_SIZE = "asksize";
    public static final String COL_ASK_PX = "askpx";
    public static final String COL_BID_SOURCE = "bidsource";
    public static final String COL_BID_SIZE = "bidsize";
    public static final String COL_BID_PX = "bidpx";

    private static String CHARS = "ABCDEFGHIJKLMNOPQRSTUVXYZ";
    private static Format DOUBLE_FORMAT = new DecimalFormat("###.####");
    private static Format QUOTE_FORMAT = new DecimalFormat("###.##");

    private final int rowCount_;
    private final String[] symbols_;
    private final String[] sides_;
    private final String[] qtys_;
    private final String[] prices_;
    private final String[] lastpx_;
    private final String[] tifs_;
    private final String[] accounts_;
    private final String[] clients_;
    private final String[] brokers_;
    private final String[] exchanges_;

    private final String[] askSources_;
    private final String[] askSizes_;
    private final Double[] askPrices_;
    private final String[] bidSources_;
    private final String[] bidSizes_;
    private final Double[] bidPrices_;

    private Map<String, Function<Integer, Object>> columnIdToValSupplier_;

    public FGDataSimulator(int tableRowCount)
    {
        Random rnd = new Random();

        rowCount_ = tableRowCount;
        String[] allSymbols = genRandomStrArray(100, 4);
        symbols_ = genStrArray(rowCount_, () -> getRandomOf(allSymbols));
        sides_ = genStrArray(rowCount_, () -> getRandomOf("Buy", "Cover", "Sell", "Short"));
        qtys_ = genStrArray(rowCount_, () -> String.valueOf((rnd.nextInt(15)+1)*1000));

        prices_ = new String[rowCount_];
        lastpx_ = new String[rowCount_];
        Double[] prices = Stream.generate(() -> (rnd.nextDouble()+1.0d)*50.0d + 50.0d).limit(rowCount_).toArray(Double[]::new);
        for (int i=0; i<prices.length; i++)
        {
            prices_[i] = DOUBLE_FORMAT.format(prices[i]);
            lastpx_[i] = DOUBLE_FORMAT.format(prices[i] + rnd.nextDouble()*10.0d);
        }

        tifs_ = genStrArray(rowCount_, () -> getRandomOf("Day", "GTC", "OPG", "IOC", "FOK", "GTX", "GTD"));
        accounts_ = genRandomStrArray(rowCount_, 6);
        clients_ = genStrArray(rowCount_, () -> getRandomOf("Trader1", "Trader2", "Trader3"));
        brokers_ = genStrArray(rowCount_, () -> getRandomOf("Bank", "Broker", "Capital"));
        exchanges_ = genRandomStrArray(rowCount_, 3);

        askSources_ = genStrArray(100, () -> genRandomStr(3));
        askSizes_ = genStrArray(100, () -> String.valueOf((rnd.nextInt(15)+1)*100));
        askPrices_ = genDoubleArray(100, () -> 80 - rnd.nextDouble() * 30.0d);
        bidSources_ = genStrArray(100, () -> genRandomStr(3));
        bidSizes_ = genStrArray(100, () -> String.valueOf((rnd.nextInt(15)+1)*100));
        bidPrices_ = genDoubleArray(100, () -> 50 + rnd.nextDouble() * 30.0d);

        columnIdToValSupplier_ = new HashMap<>();

        columnIdToValSupplier_.put(COL_SYMBOL, r -> symbols_[r]);
        columnIdToValSupplier_.put(COL_SIDE, r -> sides_[r]);
        columnIdToValSupplier_.put(COL_ID, r -> String.valueOf(r+1));
        columnIdToValSupplier_.put(COL_QTY, r -> qtys_[r]);
        columnIdToValSupplier_.put(COL_PRICE, r -> prices_[r]);
        columnIdToValSupplier_.put(COL_TIF, r -> tifs_[r]);
        columnIdToValSupplier_.put(COL_LAST_PX, r -> lastpx_[r]);
        columnIdToValSupplier_.put(COL_ACCOUNT, r -> accounts_[r]);
        columnIdToValSupplier_.put(COL_CLIENT, r -> clients_[r]);
        columnIdToValSupplier_.put(COL_BROKER, r -> brokers_[r]);
        columnIdToValSupplier_.put(COL_EXCHANGE, r -> exchanges_[r]);

        columnIdToValSupplier_.put(COL_ASK_SOURCE, r -> askSources_[r]);
        columnIdToValSupplier_.put(COL_ASK_SIZE, r -> askSizes_[r]);
        columnIdToValSupplier_.put(COL_ASK_PX, r -> Double.valueOf(QUOTE_FORMAT.format(askPrices_[r]-r)));
        //columnIdToValSupplier_.put(COL_ASK_PX, r -> QUOTE_FORMAT.format(askPrices_[r]-r));

        columnIdToValSupplier_.put(COL_BID_SOURCE, r -> bidSources_[r]);
        columnIdToValSupplier_.put(COL_BID_SIZE, r -> bidSizes_[r]);
        columnIdToValSupplier_.put(COL_BID_PX, r -> Double.valueOf(QUOTE_FORMAT.format(bidPrices_[r]+r)));
        //columnIdToValSupplier_.put(COL_BID_PX, r -> QUOTE_FORMAT.format(bidPrices_[r]+r));
    }

    public Object getValue(Number modelRowIndex, String columnId)
    {
        return columnIdToValSupplier_.get(columnId).apply(Integer.valueOf(modelRowIndex.intValue()));
    }

    private static String[] genStrArray(int size, Supplier<String> supplier)
    {
        return Stream.generate(supplier).limit(size).toArray(String[]::new);
    }

    private static Double[] genDoubleArray(int size, Supplier<Double> supplier)
    {
        return Stream.generate(supplier).limit(size).toArray(Double[]::new);
    }

    private static String[] genRandomStrArray(int size, int strLen)
    {
        return genStrArray(size, () -> genRandomStr(strLen));
    }

    private static String genRandomStr(int len)
    {
        Random r = new Random();
        return r.ints(len, 0, CHARS.length()).mapToObj(i -> CHARS.substring(i, i+1)).reduce("", (a,b) -> a+b);
    }

    private static String getRandomOf(String... options)
    {
        Random r = new Random();
        return options[r.nextInt(options.length)];
    }


    public static void main(String[] args)
    {
        System.out.println("-DLTEMP- FGDataSimulator.main " + Arrays.toString(genRandomStrArray(10, 4)) );

//        Object[] a = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};
//
//        System.out.println("-DLTEMP- FGDataSimulator.main " + arrayToString( split(a, 3) ));
    }

}
