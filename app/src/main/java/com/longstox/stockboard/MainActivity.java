package com.longstox.stockboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {
    private static final int SYMBOL_COLUMN = 0;
	private static final String SYMBOL_SEP = ";";
    private static final String DEFAULT_SYMBOLS = "AAPL;GOOG;AMZN";
    private static final String SYMBOLS = "symbols";
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private List<String> symbols = new ArrayList<String>();
    private static final String[] columns = { "Symbol", "Price", "SMA200d",
            "52w max", "P/S" };
    static final String[] yahooFields = { "s", "l1", "m6", "k5", "p5", "n" }; // http://www.jarloo.com/yahoo_finance/
    private TableLayout table;
    private static final int[] ROW_COLORS = { 0x770099CC, 0x550099CC };
	private static final int SMA_COLUMN = 2;

    public MainActivity() {
    }

    static class StockData {
        public String name;
    }

    private Map<String, StockData> stockData = new HashMap<String, StockData>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        restoreSettings();

        showQuotes();

    }

    @Override
    protected void onStop() {
        Log.d(LOG_TAG, "onStop");
        super.onStop();

        saveSettings();
    }

    private void saveSettings() {
        Log.d(LOG_TAG, "saveSettings");
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SYMBOLS, serializeList(symbols));
        editor.commit();
    }

    private void restoreSettings() {
        Log.d(LOG_TAG, "restoreSettings");
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        String stringList = settings.getString(SYMBOLS, DEFAULT_SYMBOLS);
        Log.d(LOG_TAG, SYMBOLS + " = " + stringList);
        String[] words = stringList.split(SYMBOL_SEP);
        symbols.clear();
        symbols.addAll(Arrays.asList(words));
    }

    private String serializeList(List<String> list) {
        StringBuilder builder = new StringBuilder();
        for (String s : list) {
            if (builder.length() > 0)
                builder.append(SYMBOL_SEP);
            builder.append(s);
        }
        return builder.toString();
    }

    private class FetchQuotesCallback implements FetchQuotesTask.ResultCallback {
        @Override
        public void onDone(FetchQuotesTask.Result result) {
            if (result.exception != null) {
                showError(result.exception.getMessage());
            } else {
                createTable(result.data);
            }
        }
    }

    private void showQuotes() {
        TextView view = new TextView(this);
        view.setText("Downloading data...");
        setContentView(view);

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            FetchQuotesTask.Parameters params = new FetchQuotesTask.Parameters();
            params.symbols = symbols;
            new FetchQuotesTask(new FetchQuotesCallback()).execute(params);
        } else {
            String message = "No internet connection";
            showError(message);
            view.setText(message);
        }
    }

    private void showError(String message) {
        Log.e(LOG_TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void createTable(String[][] data) {
        stockData.clear();

        table = new TableLayout(this);
        table.setStretchAllColumns(true);
        table.setShrinkAllColumns(true);

        // table header
        TableRow header = new TableRow(this);
        setPadding(header);
        header.setBackgroundColor(0xAA0099CC);
        for (String s : columns) {
            TextView cell = new TextView(this);
            cell.setText(s);
            cell.setTypeface(null, Typeface.BOLD);
            header.addView(cell);
        }
        table.addView(header);

        // table data
        for (int r = 0; r < data.length; ++r) {
            final String symbol = data[r][0];
            StockData sdata = new StockData();
            sdata.name = data[r][data[r].length - 1];
            stockData.put(symbol, sdata);
            TableRow row = new TableRow(this) {
                @Override
                protected ContextMenuInfo getContextMenuInfo() {
                    return new StockContextMenuInfo(symbol);
                }
            };
            row.setTag(symbol);
            setPadding(row);
            row.setBackgroundColor(ROW_COLORS[r % 2]);
            int nc = Math.min(columns.length, data[r].length);
            for (int column = 0; column < nc; ++column) {
                TextView cell = new TextView(this);
                String text = data[r][column];
				cell.setText(text);
                if (column == SYMBOL_COLUMN)
                    cell.setTypeface(null, Typeface.BOLD);
                else if (column == SMA_COLUMN) {
                	cell.setTextColor(text.startsWith("-") ? 0xFFFF3333 : 0xDD00FF00);
                }
                row.addView(cell);
            }
            row.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v,
                        ContextMenuInfo menuInfo) {
                    getMenuInflater().inflate(R.menu.stock_context, menu);
                    menu.setHeaderTitle(symbol + " ("
                            + stockData.get(symbol).name + ")");
                    Log.d(LOG_TAG, "onCreateContextMenu: " + v + " " + menuInfo);
                }
            });
            table.addView(row);
        }
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(table);
        setContentView(scrollView);
    }

    private void setPadding(TableRow header) {
        header.setPadding(5, 5, 5, 5);
    }

    private class StockContextMenuInfo implements ContextMenuInfo {
        private final String symbol;

        public StockContextMenuInfo(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        StockContextMenuInfo info = (StockContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case R.id.delete:
            deleteStock(info.getSymbol());
            return true;
        case R.id.yahoo:
            openYahoo(info.getSymbol());
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    private void deleteStock(String symbol) {
    	Log.d(LOG_TAG, "delete symbol " + symbol);
        deleteTableRow(symbol);
        symbols.remove(symbol);
        saveSettings();
    }

    private void openYahoo(String symbol) {
        Log.d(LOG_TAG, "Open " + symbol + " in Yahoo");
        Intent intent= new Intent(Intent.ACTION_VIEW, Uri.parse(
                "https://finance.yahoo.com/quote/" + symbol));
        startActivity(intent);
    }

    private void deleteTableRow(String symbol) {
        View row = table.findViewWithTag(symbol);
        int i = table.indexOfChild(row);
        table.removeView(row);
        int count = table.getChildCount();
        for (; i < count; ++i) {
            View child = table.getChildAt(i);
            child.setBackgroundColor(ROW_COLORS[(i - 1) % 2]);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_new:
            addStock();
            return true;
        case R.id.action_refresh:
            showQuotes();
            return true;
        case R.id.action_settings:
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void addStock() {
        AddStockDialog dialog = new AddStockDialog();
        dialog.listener = new AddStockDialog.Listener() {
            @Override
            public void onOK(String stockSymbol) {
                stockSymbol = stockSymbol.toUpperCase(Locale.ENGLISH);
                if (symbols.contains(stockSymbol)) {
                    showError(stockSymbol + " already added");
                } else {
                	Log.d(LOG_TAG, "add symbol " + stockSymbol);
                    symbols.add(stockSymbol);
                    saveSettings();
                    showQuotes();
                }
            }
        };
        dialog.show(getSupportFragmentManager(), dialog.getClass()
                .getSimpleName());
    }

}
